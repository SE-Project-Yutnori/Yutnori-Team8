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

    private Game game; // initializeGame에서 할당되므로 final 아님
    private final YutGameUI ui; // 생성자에서 할당 후 변경 없으므로 final
    private final List<YutThrowResult> currentTurnThrows = new ArrayList<>(); // 객체 자체가 변경되지 않으므로 final
    private int pendingExtraTurns = 0; // 잡기 또는 윷/모로 인해 누적된 추가 던지기 횟수
    private boolean yutOrMoJustThrown = false; // 바로 직전에 윷/모를 던졌는지 (연속 던지기 유도용)

    public GameController(YutGameUI ui) {
        this.ui = ui;
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
        this.yutOrMoJustThrown = false;
    }

    private void resetForCurrentPlayerTurnSegment() {
        this.currentTurnThrows.clear();
        this.yutOrMoJustThrown = false;
    }

    public Game getGame() {
        return game;
    }

    public void handleThrowRequest(boolean isRandom) {
        if (!canPlayerAct()) return;

        Player currentPlayer = game.getCurrentPlayer();
        YutThrowResult result;
        this.yutOrMoJustThrown = false;

        if (isRandom) {
            result = YutThrower.throwRandom();
        } else {
            YutThrowResult selectedResult = ui.promptForDesignatedThrow();
            if (selectedResult == null) return;
            result = selectedResult;
        }
        ui.logMessage(currentPlayer.getName() + " → " + result.name());
        currentTurnThrows.add(result);

        if (result == YutThrowResult.YUT || result == YutThrowResult.MO) {
            this.yutOrMoJustThrown = true;
        }

        if (!this.yutOrMoJustThrown && this.currentTurnThrows.size() == 1 && this.pendingExtraTurns == 0) {
            ui.logMessage("하나의 결과만 나왔습니다. 바로 이동할 말을 선택하세요.");
            ui.enableThrowButtons(false);
            ui.showActionPanel(true, getCurrentAvailableThrows(), getMovablePiecesForCurrentPlayer());
        } else {
            displayAvailableThrowsAndPromptAction();
        }
    }

    private void displayAvailableThrowsAndPromptAction() {
        if (!canPlayerAct()) return;

        List<YutThrowResult> availableThrows = getCurrentAvailableThrows();
        List<Piece> movablePieces = getMovablePiecesForCurrentPlayer();

        boolean hasAvailableActionsToTake = !availableThrows.isEmpty(); // 움직일 윷이 있는지
        boolean canPlayerThrowAgain = this.yutOrMoJustThrown || this.pendingExtraTurns > 0; // 더 던질 수 있는지

        if (!hasAvailableActionsToTake && !canPlayerThrowAgain) {
            decideNextStepAfterTurnActions();
            return;
        }

        ui.logMessage(game.getCurrentPlayer().getName() + "님, 행동을 선택하세요.");
        if (hasAvailableActionsToTake){
            ui.logMessage("사용 가능한 윷: [" + availableThrows.stream().map(Enum::name).collect(Collectors.joining(", ")) + "]");
        } else {
            ui.logMessage("현재 사용할 수 있는 윷 결과가 없습니다. 윷을 던지거나 턴을 마칠 수 있습니다.");
        }

        ui.enableThrowButtons(canPlayerThrowAgain);

        if (this.yutOrMoJustThrown) {
            ui.logMessage("방금 윷/모가 나왔습니다! 한 번 더 던지거나, 현재 윷으로 이동할 수 있습니다.");
        }
        if (this.pendingExtraTurns > 0) {
            ui.logMessage("잡기 등으로 인해 " + this.pendingExtraTurns + "번의 추가 던지기 기회가 예약되어 있습니다.");
        }

        ui.showActionPanel(hasAvailableActionsToTake, availableThrows, movablePieces);
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

        boolean wasYutOrMoJustThrownBeforeMove = this.yutOrMoJustThrown;
        this.yutOrMoJustThrown = false;

        MoveOutcome outcome = movePiece(game.getCurrentPlayer(), pieceToMove, throwToApply);
        currentTurnThrows.remove(throwToApply);

        if (outcome.captured) {
            this.pendingExtraTurns++;
            ui.logMessage("상대 말을 잡아 추가 던지기 기회가 +1 되었습니다! (예약된 추가 던지기: " + this.pendingExtraTurns + "번)");
        }

        // 윷/모를 던지고 그 결과로 바로 말을 움직였다면, yutOrMoJustThrown은 false가 되었고,
        // 이로 인한 추가 턴 기회는 decideNextStepAfterTurnActions에서 pendingExtraTurns로 변환될 것임.
        // 만약, 윷/모를 던지고 말을 움직였는데, 그 움직임으로 인해 또 다른 윷/모가 발생하지는 않으므로
        // yutOrMoJustThrown은 false 유지.

        ui.refreshBoard();
        ui.updateIndicators();

        if (game.checkWin(game.getCurrentPlayer())) {
            ui.showWinMessage(game.getCurrentPlayer().getName() + " 승리!");
            prepareNewTurn(); // 상태 초기화
            currentTurnThrows.clear();
            ui.enableThrowButtons(false);
            ui.showActionPanel(false, null, null);
            return;
        }

        displayAvailableThrowsAndPromptAction();
    }

    public void decideNextStepAfterTurnActions() {
        if (!canPlayerAct()) return;

        if (!currentTurnThrows.isEmpty()) {
            ui.logMessage("남은 윷 결과 [" + currentTurnThrows.stream().map(Enum::name).collect(Collectors.joining(", ")) + "] 사용을 포기하고 턴을 마칩니다.");
            currentTurnThrows.clear();
        }

        if (this.yutOrMoJustThrown) {
            this.pendingExtraTurns++;
            ui.logMessage("마지막 던지기(윷/모)로 인해 추가 던지기 기회가 +1 되었습니다! (총 예약 " + this.pendingExtraTurns + "번)");
            this.yutOrMoJustThrown = false;
        }

        if (this.pendingExtraTurns > 0) {
            this.pendingExtraTurns--;
            ui.logMessage(game.getCurrentPlayer().getName() + "님, 추가 던지기 기회입니다! (남은 예약: " + this.pendingExtraTurns + "번)");
            resetForCurrentPlayerTurnSegment(); // 중요: 새 던지기 세트를 위해 초기화
            ui.enableThrowButtons(true);
            ui.showActionPanel(false, null, null);
        } else {
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

        List<Position> path = PathManager.getNextPositions(pieceToMove, yutResult.getMove());

        if (!path.isEmpty()) {
            Position destination = path.get(path.size() - 1);
            // 경로 문맥 업데이트 로직 (Piece의 상태를 변경하므로 신중한 설계 필요)
            // 예: 말이 CENTER에 진입/이탈 시 Piece의 상태 업데이트
            for (Position stepPos : path) {
                if (pieceToMove.getPosition() == Position.CENTER && stepPos != Position.CENTER) { // CENTER를 벗어남
                    // pieceToMove.setLastMajorNodeBeforeCenter(null); // 또는 다른 로직
                }
                if (stepPos == Position.DIA_A2 || stepPos == Position.DIA_B2) {
                    pieceToMove.setLastMajorNodeBeforeCenter(stepPos);
                } else if (stepPos == Position.CENTER && PathManager.DIAG_A_TO_B.get(0) == Position.CENTER) {
                    pieceToMove.setLastMajorNodeBeforeCenter(Position.CENTER);
                }
            }


            for (Piece pInGroup : groupToMove) {
                if (game.getBoard().placePiece(pInGroup, destination)) {
                    captured = true;
                }
            }

            if (pieceToMove.getPosition() != originalPos || pieceToMove.getPosition() == Position.END) {
                pieceActuallyMoved = true;
            }

            if (pieceActuallyMoved) {
                // String.format() 대신 문자열 연결 사용
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