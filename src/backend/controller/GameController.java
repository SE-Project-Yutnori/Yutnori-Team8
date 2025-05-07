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
    private boolean pieceMovedThisTurnSegment = false; // 현재 "던지기-이동" 세그먼트에서 이동 여부
    private int pendingExtraTurns = 0; // 남은 추가 턴 횟수

    public GameController(YutGameUI ui) {
        this.ui = ui;
    }

    public void initializeGame(int playerCount, int pieceCount) {
        this.game = new Game(playerCount, pieceCount);
        this.currentTurnThrows.clear();
        this.pieceMovedThisTurnSegment = false;
        this.pendingExtraTurns = 0; // 게임 시작 시 추가 턴 없음
        ui.setGameModel(game);
        ui.logMessage("게임을 시작합니다. " + game.getCurrentPlayer().getName() + " 차례입니다.");
        ui.updateStatusLabel(game.getCurrentPlayer().getName() + " 차례입니다.");
        ui.updateIndicators();
        ui.refreshBoard();
    }

    public Game getGame() {
        return game;
    }

    public void handleThrowRequest(boolean isRandom) {
        if (game == null || (game.getCurrentPlayer() != null && game.checkWin(game.getCurrentPlayer())) ) {
            if (game != null && game.getCurrentPlayer() != null && game.checkWin(game.getCurrentPlayer())) {
                ui.showInfo("게임이 이미 종료되었습니다.");
            } else if (game == null) {
                ui.showError("게임이 아직 시작되지 않았습니다.");
            }
            return;
        }

        Player currentPlayer = game.getCurrentPlayer();
        YutThrowResult result;

        if (isRandom) {
            result = YutThrower.throwRandom();
        } else {
            YutThrowResult selectedResult = ui.promptForDesignatedThrow();
            if (selectedResult == null) {
                return;
            }
            result = selectedResult;
        }
        ui.logMessage(currentPlayer.getName() + " → " + result.name());

        // 윷/모로 인한 추가 턴 예약
        if (result == YutThrowResult.YUT || result == YutThrowResult.MO) {
            this.pendingExtraTurns++;
            ui.logMessage("윷 또는 모가 나와 추가 턴이 예약되었습니다! (현재 총 " + this.pendingExtraTurns + "번 추가 턴)");
        }
        processThrowResult(result);
    }

    private void processThrowResult(YutThrowResult result) {
        currentTurnThrows.add(result);
        promptAndMovePiece();
    }

    public void promptAndMovePiece() {
        if (currentTurnThrows.isEmpty()) {
            // 모든 윷 결과가 소모됨. 추가 턴 또는 턴 종료 결정.
            // 이 시점에서는 pieceMovedThisTurnSegment가 마지막 이동 시도에 대한 결과를 가짐.
            // 마지막 잡기 여부는 MoveOutcome에서 관리되어야 함.
            // endTurnOrContinue는 마지막 잡기 여부와 마지막 윷 결과를 받아야 함.
            // 하지만 여기서는 currentTurnThrows가 비었으므로, 마지막 정보를 전달할 수 없음.
            // 따라서, endTurnOrContinue()는 pendingExtraTurns를 확인하게 됨.
            endTurnOrContinue();
            return;
        }

        Player currentPlayer = game.getCurrentPlayer();
        YutThrowResult throwToApply;

        if (currentTurnThrows.size() > 1) {
            throwToApply = ui.promptForThrowSelection(new ArrayList<>(currentTurnThrows));
            if (throwToApply == null) {
                ui.showInfo("윷 결과를 선택해야 합니다.");
                return;
            }
        } else {
            throwToApply = currentTurnThrows.get(0);
        }

        // 특별 규칙: 모든 말이 출발하지 않았고 빽도인 경우
        if (throwToApply.getMove() < 0) {
            boolean allPiecesOffBoard = currentPlayer.getPieces().stream()
                    .filter(p -> !p.isFinished())
                    .allMatch(p -> p.getPosition() == Position.OFFBOARD);

            if (allPiecesOffBoard) {
                ui.logMessage(currentPlayer.getName() + "님, 모든 말이 출발하지 않은 상태에서 빽도가 나와, " +
                        "'" + throwToApply.name() + "' 결과는 사용되지 않고 소모됩니다.");
                currentTurnThrows.remove(throwToApply);
                this.pieceMovedThisTurnSegment = false; // 이동 없었음

                if (!currentTurnThrows.isEmpty()) {
                    promptAndMovePiece();
                } else {
                    // 이 경우, 잡기는 발생하지 않았고, throwToApply가 마지막으로 고려된 윷.
                    // pendingExtraTurns는 이미 handleThrowRequest에서 윷/모에 따라 증가했을 수 있음.
                    endTurnOrContinue();
                }
                return;
            }
        }

        final YutThrowResult finalThrowToApplyForFilter = throwToApply;
        List<Piece> movablePieces = currentPlayer.getPieces().stream()
                .filter(p -> !p.isFinished())
                .filter(p -> !(p.getPosition() == Position.OFFBOARD && finalThrowToApplyForFilter.getMove() < 0))
                .collect(Collectors.toList());

        if (movablePieces.isEmpty()) {
            ui.logMessage("'" + throwToApply.name() + "' 결과로 움직일 수 있는 말이 없습니다. 결과가 소모됩니다.");
            currentTurnThrows.remove(throwToApply);
            this.pieceMovedThisTurnSegment = false;

            if (!currentTurnThrows.isEmpty()) {
                promptAndMovePiece();
            } else {
                endTurnOrContinue();
            }
            return;
        }

        Piece selectedPiece;
        if (movablePieces.size() > 1) {
            selectedPiece = ui.promptForPieceSelection(movablePieces, "어떤 말에 '" + throwToApply.name() + "' 결과를 적용하시겠습니까?");
        } else {
            selectedPiece = movablePieces.get(0);
        }

        if (selectedPiece == null) {
            ui.showInfo("말을 선택해야 합니다.");
            return;
        }

        if (selectedPiece.getPosition() == Position.CENTER) { // GameController에 정의된 CENTER 특별 규칙
            boolean allOtherPiecesStarted = currentPlayer.getPieces().stream()
                    .filter(p -> p != selectedPiece && !p.isFinished())
                    .allMatch(p -> p.getPosition() != Position.OFFBOARD);

            if (allOtherPiecesStarted) {
                ui.logMessage(currentPlayer.getName() + "님, CENTER에 있고 모든 다른 말이 출발하여, " +
                        "'" + throwToApply.name() + "' 결과는 사용되지 않고 소모됩니다. (특별 규칙)");
                currentTurnThrows.remove(throwToApply);
                this.pieceMovedThisTurnSegment = false;

                if (!currentTurnThrows.isEmpty()) {
                    promptAndMovePiece();
                } else {
                    endTurnOrContinue();
                }
                return;
            }
        }

        MoveOutcome outcome = movePiece(currentPlayer, selectedPiece, throwToApply);

        if (outcome.pieceActuallyMoved || outcome.captured) {
            currentTurnThrows.remove(throwToApply);
            if (outcome.pieceActuallyMoved) {
                this.pieceMovedThisTurnSegment = true;
            }
            if (outcome.captured) {
                this.pendingExtraTurns++; // 잡기로 인한 추가 턴 예약
                ui.logMessage("상대 말을 잡아 추가 턴이 예약되었습니다! (현재 총 " + this.pendingExtraTurns + "번 추가 턴)");
            }
        } else {
            ui.logMessage("'" + throwToApply.name() + "' 결과로 말을 움직이지 못했습니다. 결과가 소모됩니다.");
            currentTurnThrows.remove(throwToApply);
            // pieceMovedThisTurnSegment는 변경 없음 (이동 없었으므로 이전 값 유지)
        }

        ui.refreshBoard();
        ui.updateIndicators();

        if (game.checkWin(currentPlayer)) {
            ui.showWinMessage(currentPlayer.getName() + " 승리!");
            currentTurnThrows.clear();
            this.pendingExtraTurns = 0; // 게임 종료 시 추가 턴 무효
            return;
        }

        if (!currentTurnThrows.isEmpty()) {
            ui.logMessage(currentPlayer.getName() + "님, 남은 윷 결과가 있습니다: " + currentTurnThrows.stream().map(Enum::name).collect(Collectors.joining(", ")));
            promptAndMovePiece();
        } else {
            // 모든 윷 결과 소모됨. 이제 추가 턴 여부 판단.
            endTurnOrContinue();
        }
    }

    private MoveOutcome movePiece(Player player, Piece pieceToMove, YutThrowResult yutResult) {
        Position originalPos = pieceToMove.getPosition();
        boolean captured = false;
        boolean pieceActuallyMoved = false;

        if (originalPos == Position.OFFBOARD && yutResult.getMove() < 0) {
            return new MoveOutcome(false, false);
        }

        List<Piece> groupToMove = new ArrayList<>();
        // ... (groupToMove 로직 동일) ...
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
                // 잡기 메시지는 promptAndMovePiece에서 pendingExtraTurns 증가와 함께 처리
            }
        } else {
            pieceActuallyMoved = false;
        }
        return new MoveOutcome(pieceActuallyMoved, captured);
    }

    // endTurnOrContinue는 이제 인자 없이, pendingExtraTurns를 확인하여 동작
    private void endTurnOrContinue() {
        currentTurnThrows.clear(); // 현재 처리 중이던 윷 결과는 모두 소진된 상태여야 함

        if (this.pendingExtraTurns > 0) {
            this.pendingExtraTurns--;
            ui.logMessage(game.getCurrentPlayer().getName() + "님, 추가 던지기 기회입니다! (남은 추가 턴: " + this.pendingExtraTurns + "번)");
            // 현재 플레이어가 다시 던지도록 유도 (handleThrowRequest를 직접 호출하면 무한 루프 위험, UI 버튼을 통해 다시 시작)
            // 여기서는 단순히 상태를 유지하고, UI가 다음 던지기를 유도하도록 함.
            // 또는, 바로 던지게 하려면:
            // SwingUtilities.invokeLater(() -> handleThrowRequest(true)); // 예시: 랜덤 던지기로 바로 연결 (UI 스레드 고려)
            // 지금은 메시지만 남기고, 플레이어가 버튼을 다시 누르도록.
        } else {
            game.nextTurn();
            this.pieceMovedThisTurnSegment = false; // 다음 턴을 위해 초기화
            // this.pendingExtraTurns = 0; // 이미 0이거나 위에서 감소됨
            ui.logMessage(game.getCurrentPlayer().getName() + " 차례입니다.");
            ui.updateStatusLabel(game.getCurrentPlayer().getName() + " 차례입니다.");
        }
        // pieceMovedThisTurnSegment는 각 "던지기-이동" 시도에 대한 결과이므로,
        // 턴이 실제로 넘어갈 때 false로 리셋되는 것이 더 적절.
        // 추가 턴이 있다면, 다음 "던지기-이동" 시도를 위해 pieceMovedThisTurnSegment는 유지될 필요 없음 (새로 판단)
        this.pieceMovedThisTurnSegment = false; // 추가 턴이 있든 없든, 다음 던지기 세션을 위해 리셋
    }

    public List<YutThrowResult> getCurrentTurnThrows() {
        return new ArrayList<>(currentTurnThrows);
    }
}