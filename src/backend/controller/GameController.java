package backend.controller;

import backend.game.Game;
import backend.game.YutThrowResult;
import backend.game.YutThrower;
import backend.model.BoardShape;
import backend.model.PathManager;
import backend.model.Piece;
import backend.model.Player;
import backend.model.Position;
import frontend.YutGameUI;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GameController {

    private static class MoveOutcome {
        final boolean pieceActuallyMoved;
        final boolean captured;
        MoveOutcome(boolean pieceActuallyMoved, boolean captured) {
            this.pieceActuallyMoved = pieceActuallyMoved;
            this.captured = captured;
        }
    }

    private Game game;
    private BoardShape selectedBoardShape;
    private final YutGameUI ui;
    private final List<YutThrowResult> currentTurnThrows = new ArrayList<>();
    private int pendingExtraTurns = 0;
    private boolean yutOrMoEffectFromLastThrow = false;

    public GameController(YutGameUI ui, BoardShape shape) {
        this.ui = ui;
        this.selectedBoardShape = shape; 
    }

    public void initializeGame(int playerCount, int pieceCount) {
        this.game = new Game(playerCount, pieceCount);
        prepareNewTurn();
        ui.setGameModel(game);
        ui.logMessage("게임을 시작합니다. " + game.getCurrentPlayer().getName() + " 차례입니다.");
        ui.updateStatusLabel(game.getCurrentPlayer().getName() + " 차례입니다.");
        ui.updateIndicators();
        ui.refreshBoard();
        ui.enableThrowButtons(true);
        ui.showActionPanel(false, null, null);
    }

    private void prepareNewTurn() {
        this.currentTurnThrows.clear();
        this.pendingExtraTurns = 0;
        this.yutOrMoEffectFromLastThrow = false;
        // 턴이 바뀌면 현재 플레이어의 모든 말의 경로 문맥을 초기화하는 것이 안전할 수 있음.
        // 단, 이월되는 정보가 있다면 신중해야 함
        // Player currentPlayer = game != null ? game.getCurrentPlayer() : null;
        // if (currentPlayer != null) {
        //     for (Piece p : currentPlayer.getPieces()) {
        //         p.clearPathContext();
        //     }
        // }
        if (game != null && game.getCurrentPlayer() != null) {
            game.getCurrentPlayer().getPieces().forEach(Piece::clearPathContext);
        }
    }

    private void startNewThrowSessionForCurrentPlayer() {
        this.currentTurnThrows.clear();
        this.yutOrMoEffectFromLastThrow = false;
        ui.enableThrowButtons(true);
        ui.showActionPanel(false, null, null);
        ui.logMessage(game.getCurrentPlayer().getName() + "님, 윷을 던져주세요.");
    }

    public Game getGame() {
        return game;
    }

    public void handleThrowRequest(boolean isRandom) {
        if (!canPlayerAct()) return;
        Player currentPlayer = game.getCurrentPlayer();

        if (this.pendingExtraTurns > 0) {
            this.pendingExtraTurns--;
            ui.logMessage("예약된 추가 던지기 기회를 사용합니다. (남은 예약: " + this.pendingExtraTurns + "번)");
        }
        this.yutOrMoEffectFromLastThrow = false;

        YutThrowResult result;
        if (isRandom) result = YutThrower.throwRandom();
        else {
            result = ui.promptForDesignatedThrow();
            if (result == null) { displayAvailableThrowsAndPromptAction(); return; }
        }
        ui.logMessage(currentPlayer.getName() + " → " + result.name());
        currentTurnThrows.add(result);

        if (result == YutThrowResult.YUT || result == YutThrowResult.MO) {
            this.yutOrMoEffectFromLastThrow = true;
        }
        displayAvailableThrowsAndPromptAction();
    }

    private void displayAvailableThrowsAndPromptAction() {
        if (!canPlayerAct()) return;
        List<YutThrowResult> availableThrows = getCurrentAvailableThrows();
        List<Piece> movablePieces = getMovablePiecesForCurrentPlayer();

        boolean hasYutToApply = !availableThrows.isEmpty();
        boolean canThrowFromYutMo = this.yutOrMoEffectFromLastThrow;
        boolean hasReservedTurns = this.pendingExtraTurns > 0;

        if (!hasYutToApply && !canThrowFromYutMo && !hasReservedTurns) {
            playerEndsTurnActions(); // 모든 행동 가능성 소진 시 턴 종료
            return;
        }

        ui.logMessage(game.getCurrentPlayer().getName() + "님, 행동을 선택하세요.");
        if (hasYutToApply) ui.logMessage("사용 가능한 윷: [" + availableThrows.stream().map(Enum::name).collect(Collectors.joining(", ")) + "]");
        else ui.logMessage("현재 사용할 수 있는 윷 결과가 없습니다.");

        ui.enableThrowButtons(canThrowFromYutMo || hasReservedTurns);
        if (canThrowFromYutMo) ui.logMessage("방금 윷/모! 한 번 더 던지거나, 현재 윷으로 이동 가능.");
        if (hasReservedTurns) ui.logMessage("예약된 추가 던지기 " + this.pendingExtraTurns + "번 가능.");

        ui.showActionPanel(hasYutToApply, availableThrows, movablePieces);
    }

    public void applySelectedYutAndPiece(YutThrowResult throwToApply, Piece pieceToMove) {
        if (!canPlayerAct() || !currentTurnThrows.contains(throwToApply)) {
            ui.logMessage("유효하지 않은 행동입니다.");
            return;
        }
        
        if (pieceToMove == null || pieceToMove.isFinished() || pieceToMove.getOwner() != game.getCurrentPlayer()) {
            ui.logMessage("유효하지 않은 말 선택입니다.");
            return;
        }
        
        if (pieceToMove.getPosition() == Position.OFFBOARD && throwToApply.getMove() < 0) {
            ui.logMessage("대기 말은 뒤로 이동할 수 없습니다.");
            return;
        }

        this.yutOrMoEffectFromLastThrow = false; // 말을 움직이는 액션을 시작하면 이 상태는 리셋

        MoveOutcome outcome = movePiece(game.getCurrentPlayer(), pieceToMove, throwToApply);
        currentTurnThrows.remove(throwToApply);

        if (outcome.captured) {
            this.pendingExtraTurns++;
            ui.logMessage("상대 말을 잡아 추가 던지기 +1! (총 예약 " + this.pendingExtraTurns + "번)");
        }

        ui.refreshBoard();
        ui.updateIndicators();
        
        // 승리 조건 확인
        if (checkPlayerWin(game.getCurrentPlayer())) {
            ui.logMessage(game.getCurrentPlayer().getName() + "님이 승리했습니다!");
            ui.updateStatusLabel(game.getCurrentPlayer().getName() + "님이 승리했습니다!");
            ui.enableThrowButtons(false);
            ui.showActionPanel(false, null, null);
            return;
        }

        displayAvailableThrowsAndPromptAction();
    }

    // 추가된 승리 확인 메소드 - 모든 말이 도착했는지 확인
    private boolean checkPlayerWin(Player player) {
        return player.getPieces().stream().allMatch(Piece::isFinished);
    }

    public void playerEndsTurnActions() { // UI의 "턴 마치기" 버튼과 연결
        if (!canPlayerAct()) return;
        if (!currentTurnThrows.isEmpty()) {
            ui.logMessage("남은 윷 결과 [" + currentTurnThrows.stream().map(Enum::name).collect(Collectors.joining(", ")) + "] 사용 포기.");
            currentTurnThrows.clear();
        }
        if (this.yutOrMoEffectFromLastThrow) { // 윷/모 던지고 말 안 움직이고 턴 넘기려 할 때
            this.pendingExtraTurns++;
            ui.logMessage("마지막 윷/모 효과로 추가 던지기 +1! (총 예약 " + this.pendingExtraTurns + "번)");
            this.yutOrMoEffectFromLastThrow = false;
        }

        if (this.pendingExtraTurns > 0) { // 예약된 추가 턴이 있다면
            // this.pendingExtraTurns--; // 여기서 소모하지 않고, handleThrowRequest에서 소모
            ui.logMessage(game.getCurrentPlayer().getName() + "님, 예약된 추가 던지기 기회가 " + this.pendingExtraTurns + "번 있습니다.");
            startNewThrowSessionForCurrentPlayer();
        } else { // 추가 턴 없으면 턴 종료
            game.nextTurn();
            prepareNewTurn();
            ui.logMessage(game.getCurrentPlayer().getName() + " 차례입니다.");
            ui.updateStatusLabel(game.getCurrentPlayer().getName() + " 차례입니다.");
            ui.enableThrowButtons(true);
            ui.showActionPanel(false, null, null);
        }
    }

    private MoveOutcome movePiece(Player player, Piece pieceToMove, YutThrowResult yutResult) {
        Position originalPos = pieceToMove.getPosition();
        boolean captured = false;
        boolean pieceActuallyMoved = false;

        List<Piece> groupToMove = new ArrayList<>();
        if (originalPos == Position.OFFBOARD) { 
            groupToMove.add(pieceToMove); 
        } else { 
            // 같은 위치에 있는 같은 플레이어의 말 그룹핑
            for (Piece p : game.getBoard().getPiecesAt(originalPos)) {
                if (p.getOwner().equals(player)) { 
                    groupToMove.add(p); 
                }
            }
        }
        
        if (groupToMove.isEmpty() && originalPos != Position.OFFBOARD) {
            groupToMove.add(pieceToMove);
        }

        List<Position> generatedPath = PathManager.getNextPositions(pieceToMove, yutResult.getMove(), selectedBoardShape);

        if (!generatedPath.isEmpty()) {
            Position destination = generatedPath.get(generatedPath.size() - 1);
            Position currentPosBeforeBoardPlace = pieceToMove.getPosition(); // Board.placePiece 호출 전 위치

            // 경로 문맥 업데이트 로직 강화
            // 대표 말(pieceToMove)이 이동한 경로(generatedPath)를 기반으로 문맥 설정
            Position prevPosInPathForContext = originalPos;
            for (Position stepInPath : generatedPath) {
                if (stepInPath == Position.CENTER) { // CENTER에 도달/통과
                    if (prevPosInPathForContext == Position.DIA_A2 || prevPosInPathForContext == Position.DIA_B2 || 
                        (prevPosInPathForContext.name().startsWith("DIA_") && prevPosInPathForContext.name().endsWith("2"))) {
                        pieceToMove.setPathContextWaypoint(prevPosInPathForContext);
                    }
                } else if (stepInPath.name().startsWith("DIA_") && stepInPath.name().endsWith("3") && 
                           prevPosInPathForContext == Position.CENTER) {
                    // CENTER -> DIA_X3 : 해당 문맥 유지 (모든 보드 형태 지원)
                    char diagPath = stepInPath.name().charAt(4); // "DIA_X3"에서 X 추출
                    Position contextPos = Position.valueOf("DIA_" + diagPath + "2");
                    pieceToMove.setPathContextWaypoint(contextPos);
                } else if (prevPosInPathForContext == Position.CENTER) {
                    // CENTER -> 다른 경로 진입: 문맥을 CENTER로 설정
                    pieceToMove.setPathContextWaypoint(Position.CENTER);
                }
                // 특정 지름길 출구에 도달했을 때도 문맥 유지 필요
                else if (prevPosInPathForContext.name().startsWith("DIA_") && 
                         prevPosInPathForContext.name().endsWith("4")) {
                    // 지름길 출구를 통해 나가는 경우
                    char diagPath = prevPosInPathForContext.name().charAt(4); // "DIA_X4"에서 X 추출
                    pieceToMove.setPathContextWaypoint(Position.valueOf("DIA_" + diagPath + "4"));
                }
                // 일반 외곽 경로로 나가는 경우 문맥 초기화
                else if (stepInPath.name().startsWith("POS_") && 
                        !prevPosInPathForContext.name().startsWith("POS_")) {
                    // 지름길에서 외곽으로 나가는 경우
                    pieceToMove.clearPathContext();
                }
                
                prevPosInPathForContext = stepInPath;
            }

            // 그룹 이동 및 상대 말 잡기
            for (Piece pInGroup : groupToMove) {
                if (game.getBoard().placePiece(pInGroup, destination)) {
                    captured = true;
                }
            }

            if (pieceToMove.getPosition() != currentPosBeforeBoardPlace || pieceToMove.getPosition() == Position.END) {
                pieceActuallyMoved = true;
            }

            if (pieceActuallyMoved) {
                String logMsg = player.getName() + "님의 말 " + groupToMove.size() + "개 (" +
                        pieceToMove.getOwner().getName() + ") " + originalPos.name() +
                        " → " + destination.name() + " (" + yutResult.name() + ")";
                ui.logMessage(logMsg);
            }
        } else {
            pieceActuallyMoved = false;
        }
        return new MoveOutcome(pieceActuallyMoved, captured);
    }
    
    public BoardShape getShape() { 
        return selectedBoardShape; 
    }

    private boolean canPlayerAct() {
        return game != null && game.getCurrentPlayer() != null && (!checkPlayerWin(game.getCurrentPlayer()));
    }

    public List<YutThrowResult> getCurrentAvailableThrows() {
        return new ArrayList<>(currentTurnThrows);
    }

    public List<Piece> getMovablePiecesForCurrentPlayer() {
        if (game == null || game.getCurrentPlayer() == null) return new ArrayList<>();
        return game.getCurrentPlayer().getPieces().stream()
                .filter(p -> !p.isFinished())
                .collect(Collectors.toList());
    }
}