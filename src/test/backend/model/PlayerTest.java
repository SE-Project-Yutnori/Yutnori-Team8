package test.backend.model;

import org.junit.jupiter.api.Test;

import backend.model.Piece;
import backend.model.Player;
import backend.model.Position;

import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class PlayerTest {
    private Player player;
    private static final String PLAYER_NAME = "테스트 플레이어";
    private static final int PIECE_COUNT = 4;

    @BeforeEach
    void setUp() {
        player = new Player(PLAYER_NAME, PIECE_COUNT);
    }

    @Test
    void testPlayerCreation() {
        assertEquals(PLAYER_NAME, player.getName());
        assertEquals(PIECE_COUNT, player.getPieces().size());
    }

    @Test
    void testPiecesOwnership() {
        for (Piece piece : player.getPieces()) {
            assertEquals(player, piece.getOwner());
        }
    }

    @Test
    void testHasFinishedAllPieces() {
        // 초기 상태에서는 모든 말이 끝나지 않았어야 함
        assertFalse(player.hasFinishedAllPieces());

        // 모든 말을 END 위치로 이동
        for (Piece piece : player.getPieces()) {
            piece.moveTo(Position.END);
        }

        // 모든 말이 끝났는지 확인
        assertTrue(player.hasFinishedAllPieces());
    }

    @Test
    void testPiecesInitialPosition() {
        for (Piece piece : player.getPieces()) {
            assertEquals(Position.OFFBOARD, piece.getPosition());
            assertFalse(piece.isFinished());
        }
    }
} 