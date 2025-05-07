package backend.controller;

import backend.game.Game;
import backend.game.YutThrowResult;
import backend.game.YutThrower;
import backend.model.PathManager;
import backend.model.Piece;
import backend.model.Player;
import backend.model.Position;
import frontend.YutGameUI; // UI 업데이트 메소드 호출용

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GameController {
    private Game game;
    private YutGameUI ui; // UI 상호작용을 위한 인터페이스 (추후 실제 인터페이스로 변경 가능)
    private List<YutThrowResult> currentTurnThrows = new ArrayList<>(); // 현재 턴에 발생한 윷 결과 목록
    private boolean pieceMovedThisTurnSegment = false; // 윷/모 이후 또는 잡기 이후 말이 이동했는지 추적 (추가 턴 결정용)


    public GameController(YutGameUI ui) {
        this.ui = ui;
    }

    public void initializeGame(int playerCount, int pieceCount) {
        this.game = new Game(playerCount, pieceCount);
        this.currentTurnThrows.clear();
        this.pieceMovedThisTurnSegment = false;
        ui.setGameModel(game); // UI가 렌더링을 위해 게임 모델을 알도록 함
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
            // 게임이 이미 끝났거나 시작되지 않은 경우 더 이상 진행하지 않음
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
                return; // 사용자가 취소함
            }
            result = selectedResult;
        }
        ui.logMessage(currentPlayer.getName() + " → " + result.name());
        processThrowResult(result);
    }

    private void processThrowResult(YutThrowResult result) {
        currentTurnThrows.add(result);
        // 윷이나 모가 나오면, 이동하기 *전에* 한 번 더 던질 기회를 가질 수도 있고,
        // 현재 윷/모 결과로 먼저 이동할 수도 있습니다.
        // 여기서는 일단 현재 던진 결과들로 이동을 시도하고, 그 후에 추가 턴 여부를 결정합니다.
        // PDF 요구사항: "사용자는 윷 던지기 결과를 적용할 게임 말을 선택할 수 있으며, 그에 따라 진행이 자동으로 되어야 한다.
        // 단, 사용자의 선택이 필요한 순간에는 사용자에게 선택권을 주어야 함
        // (예를 들어 개 위치에 말이 있는 상황에서, 윷을 던졌는데 모가 나오고 잇달아 걸이 나오면, 어떤 말에 모/걸을 적용할지 판단 의뢰)"
        // 이는 윷 결과가 쌓일 수 있음을 의미하며, 플레이어가 어떤 말을 어떤 결과로 움직일지 선택해야 함을 시사합니다.

        pieceMovedThisTurnSegment = false; // 이번 던지기 결과에 대한 이동 여부 초기화
        promptAndMovePiece(); // 쌓인 윷 결과들을 사용하여 말 이동 시도
    }

    public void promptAndMovePiece() {
        if (currentTurnThrows.isEmpty()) {
            // 이 경우는 모든 윷 결과가 소진되었거나, 플레이어가 윷/모 후 이동하지 않기로 결정했을 때 발생할 수 있습니다.
            // 여기서는 남은 윷 결과가 없으면 턴 종료 또는 추가 턴 로직으로 넘어갑니다.
            endTurnOrContinue();
            return;
        }

        Player currentPlayer = game.getCurrentPlayer();
        List<Piece> movablePieces = currentPlayer.getPieces().stream()
                .filter(p -> !p.isFinished())
                .collect(Collectors.toList());

        if (movablePieces.isEmpty()) {
            ui.showInfo(currentPlayer.getName() + "님은 움직일 말이 없습니다.");
            // currentTurnThrows.clear(); // 이 윷 결과들로는 이동할 수 있는 말이 없음
            // 그래도 윷/모를 던졌다면 추가 턴 기회는 있을 수 있으므로 clear하지 않고 endTurnOrContinue로 넘김
            endTurnOrContinue();
            return;
        }

        // 여러 윷 결과가 대기 중인 경우 (예: 윷 + 걸), 사용자가 적용할 하나를 선택하도록 함
        YutThrowResult throwToApply;
        if (currentTurnThrows.size() > 1) {
            throwToApply = ui.promptForThrowSelection(new ArrayList<>(currentTurnThrows)); // 복사본 전달
            if (throwToApply == null) { // 사용자가 윷 결과 선택을 취소함
                ui.showInfo("윷 결과를 선택해야 합니다. 턴을 넘기려면 모든 말을 이동시키거나 이동할 수 없어야 합니다.");
                return; // 선택할 때까지 대기 (혹은 다른 정책 적용 가능)
            }
        } else {
            throwToApply = currentTurnThrows.get(0);
        }

        Piece selectedPiece;
        // 이동 가능한 말이 여러 개이거나, 한 개라도 OFFBOARD 상태면 선택 필요 (어떤 말을 내보낼지)
        if (movablePieces.size() > 1 || (movablePieces.size() == 1 && movablePieces.get(0).getPosition() == Position.OFFBOARD && throwToApply.getMove() < 0 )) {
            // 빽도인데 OFFBOARD 말만 있으면 선택 불필요 (이동 불가 메시지는 movePiece에서 처리)
            if (movablePieces.size() == 1 && movablePieces.get(0).getPosition() == Position.OFFBOARD && throwToApply.getMove() < 0) {
                selectedPiece = movablePieces.get(0); // 자동으로 선택되지만, 어차피 못 움직임
            } else {
                selectedPiece = ui.promptForPieceSelection(movablePieces, "어떤 말에 '" + throwToApply.name() + "' 결과를 적용하시겠습니까?");
            }
        } else {
            selectedPiece = movablePieces.get(0); // 이동 가능한 말이 하나고 이미 보드 위에 있다면 자동 선택
        }


        if (selectedPiece == null) { // 사용자가 말 선택을 취소함
            ui.showInfo("말을 선택해야 합니다. 턴을 넘기려면 모든 말을 이동시키거나 이동할 수 없어야 합니다.");
            return; // 선택할 때까지 대기
        }

        // 이동 실행
        boolean captured = movePiece(currentPlayer, selectedPiece, throwToApply);
        if (captured || selectedPiece.getPosition() != Position.OFFBOARD || throwToApply.getMove() > 0) { // 실제 이동이 발생했거나, 잡기가 발생했다면
            currentTurnThrows.remove(throwToApply); // 이 윷 결과 사용 완료
            pieceMovedThisTurnSegment = true; // 이번 '던지기-이동' 세그먼트에서 이동 발생
        }


        ui.refreshBoard();
        ui.updateIndicators();

        if (game.checkWin(currentPlayer)) {
            ui.showWinMessage(currentPlayer.getName() + " 승리!");
            currentTurnThrows.clear();
            // UI 컨트롤 비활성화 또는 새 게임 제안 고려
            return;
        }

        // 남은 윷 결과가 더 있으면, 다시 프롬프트 (재귀 호출)
        if (!currentTurnThrows.isEmpty()) {
            ui.logMessage(currentPlayer.getName() + "님, 남은 윷 결과가 있습니다: " + currentTurnThrows.stream().map(Enum::name).collect(Collectors.joining(", ")));
            promptAndMovePiece(); // 남은 윷 결과를 재귀적으로 처리
        } else {
            // 이 구간의 모든 윷 결과가 사용되었습니다. 이제 추가 턴 여부를 결정합니다.
            endTurnOrContinue(captured, throwToApply); // 마지막 이동 결과 기반으로 결정
        }
    }

    private boolean movePiece(Player player, Piece pieceToMove, YutThrowResult yutResult) {
        Position currentPos = pieceToMove.getPosition();

        // 빽도인데 출발하지 않은 말(OFFBOARD)을 움직이려 할 경우
        if (currentPos == Position.OFFBOARD && yutResult.getMove() < 0) {
            ui.logMessage(player.getName() + "님, 시작하지 않은 말은 빽도로 움직일 수 없습니다.");
            // 이 경우 윷 결과는 소모되지 않고, 다른 말을 선택하거나 다른 윷 결과를 사용해야 함.
            // 하지만 현재 로직에서는 이 함수를 호출하기 전에 선택하므로, 여기서는 false 반환하여 이동 없음을 알림.
            // pieceMovedThisTurnSegment가 true로 설정되지 않도록 주의.

            // 아무것도 출발하지 않았을 때 백도가 나오면 처리하지 않는 프로세스. (promtandMovePiece에서 처리 시도)
//            Player currentPlayer = game.getCurrentPlayer();
//            int pieceCnt = 0;
//            int offPieceCnt = 0;
//            for(Piece p : currentPlayer.getPieces()) {
//                pieceCnt++;
//                if(p.getPosition() == Position.OFFBOARD) {
//                    offPieceCnt++;
//                }
//            }
//            // 출발한 말이 없을 경우
//            if(pieceCnt == offPieceCnt){
//                if(currentTurnThrows.size() == 1){
//                    currentTurnThrows.clear();
//                }
//            }

            return false; // 이동 없음, 잡기 없음
        }


        List<Piece> groupToMove = new ArrayList<>();
        if (currentPos == Position.OFFBOARD) {
            groupToMove.add(pieceToMove); // 출발하지 않은 말은 단독 이동
        } else {
            // 현재 위치에 있는 현재 플레이어의 모든 말을 추가 (업기 처리)
            for (Piece p : game.getBoard().getPiecesAt(currentPos)) {
                if (p.getOwner().equals(player)) {
                    groupToMove.add(p);
                }
            }
        }
        // pieceToMove가 groupToMove에 없는 경우 (이론상 발생 안함) 추가
        if (groupToMove.isEmpty() && pieceToMove.getPosition() != Position.OFFBOARD) {
            groupToMove.add(pieceToMove);
        }


        // 표준 보드의 경우: POS_5 또는 POS_10에 있다면, 플레이어는 지름길 또는 바깥 경로를 선택할 수 있습니다.
        // 현재 PathManager는 이 선택을 명시적으로 묻지 않습니다. 고정된 우선순위를 가집니다. (요구사항에 따라 수정 필요)
        // 현재는 PathManager가 알아서 경로를 결정합니다.
        List<Position> path = PathManager.getNextPositions(currentPos, yutResult.getMove());

        boolean captured = false;
        if (!path.isEmpty()) {
            Position destination = path.get(path.size() - 1);
            // 모든 그룹 말을 목적지로 이동
            for (Piece pInGroup : groupToMove) {
                // board.placePiece는 이전 위치에서 말을 제거하고 새 위치에 놓으며, 잡기 발생 여부 반환
                if (game.getBoard().placePiece(pInGroup, destination)) {
                    captured = true; // 한 번이라도 잡으면 true
                }
            }
            ui.logMessage(String.format("%s님의 말 %s개 (%s) %s → %s (%s)",
                    player.getName(), groupToMove.size(), pieceToMove.getOwner().getName(),
                    currentPos.name(), destination.name(), yutResult.name()));
            if (captured) {
                ui.logMessage("상대 말을 잡았습니다!");
            }
        } else if (yutResult.getMove() != 0 && currentPos != Position.OFFBOARD ) { // 0칸 이동이 아니고, 출발하지 않은 말이 아닌데 경로가 없는 경우 (예: END에서 이동 시도)
            ui.logMessage(player.getName() + "님의 말 " + pieceToMove.getOwner().getName() + "은(는) 더 이상 움직일 수 없습니다 (" + yutResult.name() + ").");
            return false; // 이동 없음
        } else if (yutResult.getMove() == 0) {
            // 0칸 이동 (규칙상 없음, 혹시 모를 상황 대비)
            ui.logMessage(player.getName() + "님의 말은 움직이지 않습니다.");
            return false; // 이동 없음
        }
        return captured;
    }

    // currentTurnThrows가 비었을 때 호출되어 턴 종료 또는 추가 턴을 결정
    private void endTurnOrContinue(boolean lastMoveCaptured, YutThrowResult lastThrowApplied) {
        Player currentPlayer = game.getCurrentPlayer();
        boolean earnedExtraTurn = false;

        // 추가 턴 조건:
        // 1. 마지막으로 적용된 윷 결과가 윷(YUT) 또는 모(MO)인 경우
        // 2. 마지막 이동에서 상대 말을 잡은 경우(captured)
        if (lastThrowApplied != null && (lastThrowApplied == YutThrowResult.YUT || lastThrowApplied == YutThrowResult.MO)) {
            earnedExtraTurn = true;
        }
        if (lastMoveCaptured) {
            earnedExtraTurn = true;
        }

        // pieceMovedThisTurnSegment는 해당 윷 결과로 *실제* 말이 움직였는지를 나타냄
        // 만약 윷/모가 나왔지만 말이 움직일 수 없었다면? 그래도 추가 턴은 주어져야 함 (문제 명세: "윷·모·잡기 시")
        // 따라서 lastThrowApplied를 직접 확인하는 것이 더 정확.

        if (earnedExtraTurn) {
            ui.logMessage(currentPlayer.getName() + "님, 추가 던지기 기회!");
            // 플레이어는 그대로 유지되며, 다시 던집니다. currentTurnThrows는 이미 비어있어야 함.
        } else {
            game.nextTurn();
            ui.logMessage(game.getCurrentPlayer().getName() + " 차례입니다.");
            ui.updateStatusLabel(game.getCurrentPlayer().getName() + " 차례입니다.");
        }
        currentTurnThrows.clear(); // 다음 턴 또는 추가 던지기를 위해 초기화
        pieceMovedThisTurnSegment = false; // 다음 턴의 첫번째 세그먼트를 위해 초기화
    }

    // currentTurnThrows가 비었지만, 어떤 이유로 (예: 움직일 말이 없음) 이동을 못했을 때 호출
    private void endTurnOrContinue() {
        // 이 경우는 마지막 이동이나 잡기가 없었으므로, 순수하게 던진 윷 결과만으로 추가 턴을 판단해야 함.
        // 하지만 이미 currentTurnThrows는 비어있을 것이므로, 이 함수는 사실상 턴을 넘기는 역할만 하게 됨.
        // 더 정확한 추가 턴 로직은 endTurnOrContinue(boolean, YutThrowResult)에 통합되어야 함.
        // 이 간소화된 버전은, 윷/모를 던졌으나 말이 없어 못 움직인 경우, 추가 턴을 못 받는 문제가 생길 수 있음.
        // 요구사항 "윷·모·잡기 시"는 '던진 결과'가 윷/모인 경우를 포함해야 함.

        // 임시 로직: 현재 턴에 윷이나 모가 한 번이라도 나왔었다면 추가 턴을 준다 (더 정교하게 수정 필요)
        // -> 아니, 이 함수는 currentTurnThrows가 비었을 때 불리므로, 마지막 action을 기준으로 해야한다.
        //    endTurnOrContinue(boolean, YutThrowResult)가 주 로직이므로, 이 함수는 비상용.
        //    사실상 모든 이동 후에는 currentTurnThrows가 비워지므로, 이 함수 대신 위의 함수가 호출될 것.
        //    만약 말이 아예 없어 처음부터 이동 못했다면, lastThrowApplied가 null일 것이고, 잡기도 false일 것.
        //    그때는 바로 턴이 넘어가야 함 (윷/모로 인한 추가턴은 던진 직후 반영되어야 함).
        //    GameController의 processThrowResult에서 윷/모면 currentTurnThrows에 쌓고 바로 promptAndMovePiece를 부름.
        //    결국 모든 평가는 promptAndMovePiece 후 endTurnOrContinue(boolean, YutThrowResult)에서 이루어짐.
        //    이 오버로딩된 메소드는 currentTurnThrows가 비었지만, 어떠한 명시적 이동/잡기/윷/모 정보 없이
        //    턴을 마쳐야 할 때 (예: 초기부터 말이 없어 이동 불가) 사용될 수 있다.
        //    이 경우, 이전 턴의 마지막 정보를 기반으로 추가 턴을 결정하는 것은 부적절.
        //    간단히 턴을 넘긴다.

        // Player currentPlayer = game.getCurrentPlayer();
        // boolean earnedExtraBasedOnPreviousThrows = !processedThrowsInThisTurnSegmentForExtraTurnCheck.isEmpty() &&
        // processedThrowsInThisTurnSegmentForExtraTurnCheck.stream().anyMatch(r -> r == YutThrowResult.YUT || r == YutThrowResult.MO);

        // if (earnedExtraBasedOnPreviousThrows) {
        //     ui.logMessage(currentPlayer.getName() + "님, 추가 던지기 기회! (이전 윷/모)");
        // } else {
        game.nextTurn();
        ui.logMessage(game.getCurrentPlayer().getName() + " 차례입니다.");
        ui.updateStatusLabel(game.getCurrentPlayer().getName() + " 차례입니다.");
        // }
        currentTurnThrows.clear();
        pieceMovedThisTurnSegment = false;
    }


    // UI가 여러 대기 중인 윷 결과 중 사용자가 선택한 것을 컨트롤러에 알릴 때 사용 (현재는 통합됨)
    public void userSelectedThrowAndPiece(YutThrowResult selectedThrow, Piece selectedPiece) {
        // 이 메소드는 현재 직접 호출되지 않음. promptAndMovePiece 내에서 선택과 이동이 함께 처리됨.
        // 만약 UI에서 선택만 받고 컨트롤러에 전달하는 방식으로 변경한다면 이 메소드가 사용될 수 있음.
        if (selectedThrow == null || selectedPiece == null || !currentTurnThrows.contains(selectedThrow)) {
            ui.showError("잘못된 선택입니다.");
            return;
        }

        Player currentPlayer = game.getCurrentPlayer();
        boolean captured = movePiece(currentPlayer, selectedPiece, selectedThrow);
        currentTurnThrows.remove(selectedThrow); // 사용한 윷 결과 제거
        pieceMovedThisTurnSegment = true;

        ui.refreshBoard();
        ui.updateIndicators();

        if (game.checkWin(currentPlayer)) {
            ui.showWinMessage(currentPlayer.getName() + " 승리!");
            currentTurnThrows.clear();
            return;
        }

        if (!currentTurnThrows.isEmpty()) {
            ui.logMessage(currentPlayer.getName() + "님, 남은 윷 결과가 있습니다: " + currentTurnThrows.stream().map(Enum::name).collect(Collectors.joining(", ")));
            promptAndMovePiece(); // 남은 결과 처리
        } else {
            endTurnOrContinue(captured, selectedThrow);
        }
    }

    public List<YutThrowResult> getCurrentTurnThrows() {
        return new ArrayList<>(currentTurnThrows); // 방어적 복사
    }
}