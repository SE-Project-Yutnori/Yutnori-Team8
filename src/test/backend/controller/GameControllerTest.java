package test.backend.controller;

import backend.game.*;
import backend.model.*;
import backend.controller.GameController;
import test.backend.controller.TestUI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class GameControllerTest {
    private GameController controller;
    private TestUI testUI;
    private static final int PLAYER_COUNT = 2;
    private static final int PIECE_COUNT = 4;

    @BeforeEach
    void setUp() {
        testUI = new TestUI();
        controller = new GameController(testUI, BoardShape.TRADITIONAL);
        controller.initializeGame(PLAYER_COUNT, PIECE_COUNT);
    }

    @Test
    void testGameInitialization() {
        Game game = controller.getGame();
        assertNotNull(game);
        assertEquals(PLAYER_COUNT, game.getPlayers().size());
        assertEquals(PIECE_COUNT, game.getPlayers().get(0).getPieces().size());
    }

    @Test
    void testHandleThrowRequest() {
        // 랜덤 던지기 테스트
        controller.handleThrowRequest(true);
        List<YutThrowResult> availableThrows = controller.getCurrentAvailableThrows();
        assertNotNull(availableThrows);
    }

    @Test
    void testApplySelectedYutAndPiece() {
        // 초기 말 선택
        Player currentPlayer = controller.getGame().getCurrentPlayer();
        Piece piece = currentPlayer.getPieces().get(0);
        
        // 윷 결과 설정
        controller.handleThrowRequest(true);
        YutThrowResult result = controller.getCurrentAvailableThrows().get(0);
        
        // 말 이동
        controller.applySelectedYutAndPiece(result, piece);
        
        // 말이 이동했는지 확인
        assertNotEquals(Position.OFFBOARD, piece.getPosition());
    }
}