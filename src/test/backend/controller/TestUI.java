package test.backend.controller;

import backend.controller.GameController;
import backend.game.Game;
import backend.model.Piece;
import frontend.YutGameUIInterface;
import backend.game.YutThrowResult;

import java.util.List;

public class TestUI implements YutGameUIInterface {
    @Override
    public void setGameModel(Game game) {
        // 테스트에서는 아무것도 하지 않음
    }

    @Override
    public void logMessage(String message) {
        // 테스트에서는 아무것도 하지 않음
    }

    @Override
    public void updateStatusLabel(String status) {
        // 테스트에서는 아무것도 하지 않음
    }

    @Override
    public void updateIndicators() {
        // 테스트에서는 아무것도 하지 않음
    }

    @Override
    public void refreshBoard() {
        // 테스트에서는 아무것도 하지 않음
    }

    @Override
    public void enableThrowButtons(boolean enable) {
        // 테스트에서는 아무것도 하지 않음
    }

    @Override
    public void showActionPanel(boolean show, List<YutThrowResult> availableThrows, List<Piece> movablePieces) {
        // 테스트에서는 아무것도 하지 않음
    }

    @Override
    public YutThrowResult promptForDesignatedThrow() {
        // 테스트에서는 기본값 반환
        return YutThrowResult.DO;
    }

    @Override
    public void showWinMessage(String playerName) {
        // 테스트에서는 아무것도 하지 않음
    }

	@Override
	public void setController(GameController controller) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void promptForGameSetup() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showInfo(String message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showError(String message) {
		// TODO Auto-generated method stub
		
	}
} 