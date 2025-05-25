package frontend;

import backend.game.Game;
import backend.game.YutThrowResult;
import backend.model.BoardShape;
import backend.model.Piece;
import java.util.List;

public interface YutGameUIInterface {
    // 게임 초기화 및 설정
    void setController(backend.controller.GameController controller);
    void setGameModel(Game gameModel);
    void promptForGameSetup();
    
    // 게임 상태 업데이트
    void updateStatusLabel(String text);
    void logMessage(String message);
    void refreshBoard();
    void updateIndicators();
    
    // 게임 액션 관련
    void enableThrowButtons(boolean enable);
    void showActionPanel(boolean show, List<YutThrowResult> availableThrows, List<Piece> movablePieces);
    YutThrowResult promptForDesignatedThrow();
    
    // 메시지 표시
    void showInfo(String message);
    void showError(String message);
    void showWinMessage(String winnerName);
} 