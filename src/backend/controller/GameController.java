// File: src/backend/controller/GameController.java
package backend.controller;

import backend.game.Game;
import backend.game.YutThrowResult;
import backend.game.YutThrower;
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
    private YutGameUI ui;
    private List<YutThrowResult> currentTurnThrows = new ArrayList<>();
    private int pendingExtraTurns = 0; // 잡기로 인해 "예약된" 추가 던지기 횟수
    private boolean yutOrMoJustThrown = false; // 가장 최근 던지기가 윷/모였는지 (1회성 추가 던지기 유도)

    public GameController(YutGameUI ui) {
        this.ui = ui;
    }

    public void initializeGame(int playerCount, int pieceCount) {
        this.game = new Game(playerCount, pieceCount);
        prepareNewTurn(); // 턴 상태 초기화
        ui.setGameModel(game);
        ui.logMessage("게임을 시작합니다. " + game.getCurrentPlayer().getName() + " 차례입니다.");
        ui.updateStatusLabel(game.getCurrentPlayer().getName() + " 차례입니다.");
        ui.updateIndicators();
        ui.refreshBoard();
        ui.enableThrowButtons(true);
        ui.showActionPanel(false, null, null);
    }

    // 새 턴이 시작되거나, 게임이 초기화될 때 호출
    private void prepareNewTurn() {
        this.currentTurnThrows.clear();
        this.pendingExtraTurns = 0; // 새 턴이므로 이전 턴의 잡기로 인한 추가 턴은 없음
        this.yutOrMoJustThrown = false; // 새 턴이므로 초기화
    }

    // 현재 플레이어의 턴이 시작될 때 필요한 상태 초기화
    private void resetForCurrentPlayerTurnSegment() {
        this.currentTurnThrows.clear(); // 이번 "던지기 세트"에 대한 윷 결과 초기화
        this.yutOrMoJustThrown = false; // 이번 "던지기 세트" 시작 시 초기화
        // pendingExtraTurns는 턴 전체에 걸쳐 누적되므로 여기서는 리셋하지 않음
    }


    public Game getGame() {
        return game;
    }

    public void handleThrowRequest(boolean isRandom) {
        if (!canPlayerAct()) return;

        Player currentPlayer = game.getCurrentPlayer();
        YutThrowResult result;
        // this.yutOrMoJustThrown = false; // 매 던지기 시도 전에 초기화 (중요)
        // -> resetForCurrentPlayerTurnSegment 또는 턴 시작 시 처리하므로, 여기선 불필요할 수 있음
        //    하지만, 플레이어가 윷/모 던지고 -> 말 안 움직이고 -> 또 던질 때를 대비해 초기화.
        //    아니다, yutOrMoJustThrown은 "가장 최근 던지기"만 반영해야 함.

        this.yutOrMoJustThrown = false; // 새로운 던지기이므로 초기화

        if (isRandom) {
            result = YutThrower.throwRandom();
        } else {
            YutThrowResult selectedResult = ui.promptForDesignatedThrow();
            if (selectedResult == null) return; // 사용자 취소
            result = selectedResult;
        }
        ui.logMessage(currentPlayer.getName() + " → " + result.name());
        currentTurnThrows.add(result);

        if (result == YutThrowResult.YUT || result == YutThrowResult.MO) {
            this.yutOrMoJustThrown = true; // 이번 던지기가 윷/모였음을 표시
        }

        // 단일 윷 결과 자동 이동 유도 로직 수정:
        // 윷/모가 아니고, 이번에 던져서 currentTurnThrows에 딱 하나만 있고, 예약된 추가 턴도 없을 때
        if (!this.yutOrMoJustThrown && this.currentTurnThrows.size() == 1 && this.pendingExtraTurns == 0) {
            ui.logMessage("하나의 결과만 나왔습니다. 바로 이동할 말을 선택하세요.");
            ui.enableThrowButtons(false); // 더 이상 던질 수 없음 (윷/모도 아니고, 추가 턴도 없으므로)
            ui.showActionPanel(true, getCurrentAvailableThrows(), getMovablePiecesForCurrentPlayer());
        } else {
            displayAvailableThrowsAndPromptAction();
        }
    }

    private void displayAvailableThrowsAndPromptAction() {
        if (!canPlayerAct()) return;

        List<YutThrowResult> availableThrows = getCurrentAvailableThrows();
        List<Piece> movablePieces = getMovablePiecesForCurrentPlayer();

        boolean hasAvailableActions = !availableThrows.isEmpty() || this.yutOrMoJustThrown || this.pendingExtraTurns > 0;

        if (!hasAvailableActions) { // 사용할 윷도 없고, 추가로 던질 기회도 없다면
            decideNextStepAfterTurnActions(); // 턴 종료 또는 다음 플레이어로
            return;
        }

        ui.logMessage(game.getCurrentPlayer().getName() + "님, 행동을 선택하세요.");
        if (!availableThrows.isEmpty()){
            ui.logMessage("사용 가능한 윷: [" + availableThrows.stream().map(Enum::name).collect(Collectors.joining(", ")) + "]");
        } else {
            ui.logMessage("현재 사용할 수 있는 윷 결과가 없습니다.");
        }

        boolean canThrowAgain = this.yutOrMoJustThrown || this.pendingExtraTurns > 0;
        ui.enableThrowButtons(canThrowAgain);

        if (this.yutOrMoJustThrown) {
            ui.logMessage("방금 윷/모가 나왔습니다! 한 번 더 던지거나, 현재 윷으로 이동할 수 있습니다.");
        }
        if (this.pendingExtraTurns > 0) {
            ui.logMessage("잡기 등으로 인해 " + this.pendingExtraTurns + "번의 추가 던지기 기회가 예약되어 있습니다.");
        }

        // 말을 움직일 윷이 있거나, 움직일 말이 있다면 액션 패널 표시
        ui.showActionPanel(true, availableThrows, movablePieces);
    }

    public void applySelectedYutAndPiece(YutThrowResult throwToApply, Piece pieceToMove) {
        if (!canPlayerAct() || !currentTurnThrows.contains(throwToApply)) {
            ui.showError("선택한 윷 결과는 현재 사용할 수 없거나 잘못된 시도입니다.");
            displayAvailableThrowsAndPromptAction();
            return;
        }
        if (pieceToMove == null || pieceToMove.isFinished() || pieceToMove.getOwner() != game.getCurrentPlayer()) {
            ui.showError("선택한 말은 움직일 수 없거나 잘못된 말입니다.");
            displayAvailableThrowsAndPromptAction();
            return;
        }
        if (pieceToMove.getPosition() == Position.OFFBOARD && throwToApply.getMove() < 0) {
            ui.logMessage(game.getCurrentPlayer().getName() + "님, 출발하지 않은 말은 빽도로 움직일 수 없습니다.");
            displayAvailableThrowsAndPromptAction();
            return;
        }

        // 말을 움직이면, "직전에 윷/모를 던졌다"는 상태는 해제됨.
        // 이로 인한 추가 던지기 기회는 decideNextStepAfterTurnActions에서 pendingExtraTurns로 전환됨.
        boolean wasYutOrMoJustThrownBeforeMove = this.yutOrMoJustThrown;
        this.yutOrMoJustThrown = false;

        MoveOutcome outcome = movePiece(game.getCurrentPlayer(), pieceToMove, throwToApply);
        currentTurnThrows.remove(throwToApply); // 사용한 윷 결과는 항상 제거

        if (outcome.captured) {
            this.pendingExtraTurns++; // 잡았으면 무조건 예약된 추가 턴 +1
            ui.logMessage("상대 말을 잡아 추가 던지기 기회가 +1 되었습니다! (예약된 추가 던지기: " + this.pendingExtraTurns + "번)");
        }

        // 윷/모를 던지고 그 결과로 바로 말을 움직였다면, 그 윷/모로 인한 '즉시 다시 던지기' 기회는 소진된 것.
        // 그 대신 '예약된 추가 턴'으로 전환될 수 있음 (decideNextStepAfterTurnActions에서 처리).
        // 여기서는 yutOrMoJustThrown을 false로 만들었으므로, displayAvailableThrowsAndPromptAction에서
        // "방금 윷/모가 나왔으니 또 던져라"는 메시지는 안 나감.

        ui.refreshBoard();
        ui.updateIndicators();

        if (game.checkWin(game.getCurrentPlayer())) {
            ui.showWinMessage(game.getCurrentPlayer().getName() + " 승리!");
            // 게임 종료 시 모든 상태 초기화
            prepareNewTurn(); // 다음 게임을 위해 초기화 (또는 게임 종료 상태 유지)
            currentTurnThrows.clear(); // 혹시 모르니 한번 더
            ui.enableThrowButtons(false);
            ui.showActionPanel(false, null, null);
            return;
        }

        displayAvailableThrowsAndPromptAction(); // 다음 행동 유도
    }

    // 플레이어가 '턴 마치기'를 누르거나, 모든 윷을 사용했을 때 호출
    public void decideNextStepAfterTurnActions() {
        if (!canPlayerAct()) return;

        // 사용하지 않고 남은 윷 결과가 있다면, 포기하는 것으로 간주하고 비움
        if (!currentTurnThrows.isEmpty()) {
            ui.logMessage("남은 윷 결과 [" + currentTurnThrows.stream().map(Enum::name).collect(Collectors.joining(", ")) + "] 사용을 포기합니다.");
            currentTurnThrows.clear();
        }

        // 만약 '직전에 윷/모를 던졌는데' (yutOrMoJustThrown == true) 말을 안 움직이고 '턴 마치기' 등을 했다면,
        // 그 윷/모로 인한 추가 던지기 기회를 pendingExtraTurns에 반영한다.
        if (this.yutOrMoJustThrown) {
            this.pendingExtraTurns++;
            ui.logMessage("마지막 던지기(윷/모)로 인해 추가 던지기 기회가 +1 되었습니다! (총 예약 " + this.pendingExtraTurns + "번)");
            this.yutOrMoJustThrown = false; // 이 효과는 pendingExtraTurns로 이전됨
        }

        if (this.pendingExtraTurns > 0) { // 예약된 추가 던지기 기회가 있다면
            this.pendingExtraTurns--; // 하나 사용
            ui.logMessage(game.getCurrentPlayer().getName() + "님, 추가 던지기 기회입니다! (남은 예약: " + this.pendingExtraTurns + "번)");
            resetForCurrentPlayerTurnSegment(); // 새 던지기 세트를 위해 윷 결과 목록 등 초기화
            ui.enableThrowButtons(true);
            ui.showActionPanel(false, null, null); // 액션 패널은 다음 던지기 후에 표시
            // 사용자가 다시 '윷 던지기' 버튼을 누르도록 유도
        } else { // 추가 던지기 기회가 없다면 턴 종료
            game.nextTurn();
            prepareNewTurn(); // 다음 플레이어의 새 턴을 위해 모든 관련 상태 초기화
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
            for (Piece p : game.getBoard().getPiecesAt(originalPos)) {
                if (p.getOwner().equals(player)) {
                    groupToMove.add(p);
                }
            }
        }
        if (groupToMove.isEmpty() && originalPos != Position.OFFBOARD) {
            groupToMove.add(pieceToMove);
        }

        List<Position> path = PathManager.getNextPositions(originalPos, yutResult.getMove());

        if (!path.isEmpty()) {
            Position destination = path.get(path.size() - 1);
            for (Piece pInGroup : groupToMove) {
                if (game.getBoard().placePiece(pInGroup, destination)) {
                    captured = true;
                }
            }
            if (pieceToMove.getPosition() != originalPos || pieceToMove.getPosition() == Position.END) {
                pieceActuallyMoved = true;
            }
            if (pieceActuallyMoved) {
                ui.logMessage(String.format("%s님의 말 %s개 (%s) %s → %s (%s)",
                        player.getName(), groupToMove.size(), pieceToMove.getOwner().getName(),
                        originalPos.name(), destination.name(), yutResult.name()));
            }
        } else {
            pieceActuallyMoved = false; // 경로가 없으면 이동 안 함
        }
        return new MoveOutcome(pieceActuallyMoved, captured);
    }

    private boolean canPlayerAct() {
        return game != null && game.getCurrentPlayer() != null && (game.checkWin(game.getCurrentPlayer()) == false);
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