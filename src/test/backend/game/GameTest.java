package test.backend.game;

import backend.game.Game;
import backend.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class GameTest {
    private Game game;
    private static final int PLAYER_COUNT = 2;
    private static final int PIECE_COUNT = 4;

    @BeforeEach
    void setUp() {
        game = new Game(PLAYER_COUNT, PIECE_COUNT);
    }

    @Test
    void testGameInitialization() {
        assertEquals(PLAYER_COUNT, game.getPlayers().size());
        assertNotNull(game.getBoard());
        assertEquals("Player 1", game.getCurrentPlayer().getName());
    }

    @Test
    void testNextTurn() {
        Player firstPlayer = game.getCurrentPlayer();
        game.nextTurn();
        Player secondPlayer = game.getCurrentPlayer();
        
        assertNotEquals(firstPlayer, secondPlayer);
        assertEquals("Player 2", secondPlayer.getName());
        
        // 다시 첫 번째 플레이어로 돌아오는지 확인
        game.nextTurn();
        assertEquals(firstPlayer, game.getCurrentPlayer());
    }

    @Test
    void testCheckWin() {
        Player player = game.getCurrentPlayer();
        
        // 초기 상태에서는 승리하지 않음
        assertFalse(game.checkWin(player));
        
        // 모든 말을 END 위치로 이동
        for (Piece piece : player.getPieces()) {
            piece.moveTo(Position.END);
        }
        
        // 모든 말이 END에 도달했으므로 승리
        assertTrue(game.checkWin(player));
    }

    @Test
    void testPlayerPieces() {
        for (Player player : game.getPlayers()) {
            assertEquals(PIECE_COUNT, player.getPieces().size());
            for (Piece piece : player.getPieces()) {
                assertEquals(Position.OFFBOARD, piece.getPosition());
                assertFalse(piece.isFinished());
            }
        }
    }
}