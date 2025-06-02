package test.backend.game;

import backend.game.Game;
import backend.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class GameTest {
    
    private Game game;
    
    @BeforeEach
    void setUp() {
        game = new Game(2, 4); // 2명, 각자 4개 말
    }
    
    @Test
    void testGameInitialization() {
        assertNotNull(game.getPlayers());
        assertEquals(2, game.getPlayers().size());
        assertEquals("Player 1", game.getPlayers().get(0).getName());
        assertEquals("Player 2", game.getPlayers().get(1).getName());
        
        // 각 플레이어가 4개씩 말을 가지고 있는지 확인
        for (Player player : game.getPlayers()) {
            assertEquals(4, player.getPieces().size());
            // 모든 말이 OFFBOARD에서 시작하는지 확인
            for (Piece piece : player.getPieces()) {
                assertEquals(Position.OFFBOARD, piece.getPosition());
                assertFalse(piece.isFinished());
            }
        }
    }
    
    @Test
    void testCurrentPlayer() {
        assertEquals("Player 1", game.getCurrentPlayer().getName());
    }
    
    @Test
    void testNextTurn() {
        assertEquals("Player 1", game.getCurrentPlayer().getName());
        game.nextTurn();
        assertEquals("Player 2", game.getCurrentPlayer().getName());
        game.nextTurn();
        assertEquals("Player 1", game.getCurrentPlayer().getName());
    }
    
    @Test
    void testCheckWin() {
        Player player1 = game.getCurrentPlayer();
        assertFalse(game.checkWin(player1));
        
        // 모든 말을 END로 보내서 승리 조건 테스트
        for (Piece piece : player1.getPieces()) {
            piece.moveTo(Position.END);
        }
        assertTrue(game.checkWin(player1));
    }
    
    @Test
    void testBoardInitialization() {
        assertNotNull(game.getBoard());
    }
}