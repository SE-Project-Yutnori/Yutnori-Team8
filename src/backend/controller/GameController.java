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
    
    private void updatePathContext(Piece piece, Position originalPos, List<Position> path) {
        // 경로가 비어있으면 아무것도 하지 않음
        if (path.isEmpty()) return;
        
        Position prevPos = originalPos;
        
        for (Position currentPos : path) {
            // CENTER로 진입하는 경우
            if (currentPos == Position.CENTER) {
                // 어느 지름길에서 왔는지 기록
                if (prevPos.name().startsWith("DIA_")) {
                    piece.setPathContextWaypoint(prevPos);
                }
            }
            // CENTER에서 나가는 경우
            else if (prevPos == Position.CENTER) {
                // 어느 지름길로 나가는지 확인하고 컨텍스트 유지
                if (currentPos.name().startsWith("DIA_")) {
                    char diagPath = currentPos.name().charAt(4); // DIA_X#에서 X 추출
                    // 해당 지름길의 중간 지점 위치를 컨텍스트로 설정
                    // (지름길 A의 경우 DIA_A2 등)
                    try {
                        Position contextPos = Position.valueOf("DIA_" + diagPath + "2");
                        piece.setPathContextWaypoint(contextPos);
                    } catch (IllegalArgumentException e) {
                        // 예상 포지션 이름이 없는 경우 CENTER를 컨텍스트로 설정
                        piece.setPathContextWaypoint(Position.CENTER);
                    }
                } else {
                    // CENTER에서 외곽 경로로 나가는 경우 CENTER를 컨텍스트로 저장
                    piece.setPathContextWaypoint(Position.CENTER);
                }
            }
            // 지름길 내부 이동
            else if (prevPos.name().startsWith("DIA_") && currentPos.name().startsWith("DIA_")) {
                char prevDiag = prevPos.name().charAt(4);
                char currDiag = currentPos.name().charAt(4);
                
                // 같은 지름길 내 이동 - 컨텍스트 유지
                if (prevDiag == currDiag) {
                    // 지름길 중간 지점을 컨텍스트로 설정
                    try {
                        Position contextPos = Position.valueOf("DIA_" + prevDiag + "2");
                        piece.setPathContextWaypoint(contextPos);
                    } catch (IllegalArgumentException e) {
                        // 안전장치
                        piece.setPathContextWaypoint(prevPos);
                    }
                }
            }
            // 지름길에서 외곽으로 나가는 경우
            else if (prevPos.name().startsWith("DIA_") && currentPos.name().startsWith("POS_")) {
                // 지름길 출구를 컨텍스트로 저장
                piece.setPathContextWaypoint(prevPos);
            }
            // 외곽 경로 이동
            else if (currentPos.name().startsWith("POS_")) {
                // 일반 외곽 경로 이동 시 컨텍스트 초기화 (필요한 경우)
                if (!prevPos.name().startsWith("DIA_")) {
                    piece.clearPathContext();
                }
            }
            
            prevPos = currentPos;
        }
        
        // 목적지가 최종 종료(END)인 경우 컨텍스트 초기화
        if (path.get(path.size() - 1) == Position.END) {
            piece.clearPathContext();
        }
    }

    private MoveOutcome movePiece(Player player, Piece pieceToMove, YutThrowResult yutResult) {
        Position originalPos = pieceToMove.getPosition();
        boolean captured = false;
        boolean pieceActuallyMoved = false;

        // 현재 경로 컨텍스트 저장 (이동 전)
        Position originalContext = pieceToMove.getPathContextWaypoint();

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

            // 경로 컨텍스트 업데이트 로직 개선
            updatePathContext(pieceToMove, originalPos, generatedPath);

            // 그룹 이동 및 상대 말 잡기
            for (Piece pInGroup : groupToMove) {
                // 그룹 내 다른 말들도 동일한 경로 컨텍스트 공유
                if (pInGroup != pieceToMove) {
                    pInGroup.setPathContextWaypoint(pieceToMove.getPathContextWaypoint());
                }
                
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
                if (originalContext != pieceToMove.getPathContextWaypoint()) {
                    logMsg += " [경로 컨텍스트: " + 
                        (pieceToMove.getPathContextWaypoint() != null ? 
                        pieceToMove.getPathContextWaypoint().name() : "없음") + "]";
                }
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