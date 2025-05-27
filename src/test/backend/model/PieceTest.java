package test.backend.model;

import org.junit.jupiter.api.Test;

import backend.model.Piece;
import backend.model.Player;
import backend.model.Position;

import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class PieceTest {
    private Player player;
    private Piece piece;

    @BeforeEach
    void setUp() {
        player = new Player("테스트 플레이어", 1);
        piece = player.getPieces().get(0);
    }

    @Test
    void testPieceCreation() {
        assertEquals(player, piece.getOwner());
        assertEquals(Position.OFFBOARD, piece.getPosition());
        assertFalse(piece.isFinished());
        assertNull(piece.getPathContextWaypoint());
        assertNull(piece.getLastEnteredWaypoint());
    }

    @Test
    void testMoveTo() {
        // 일반 위치로 이동
        piece.moveTo(Position.POS_0);
        assertEquals(Position.POS_0, piece.getPosition());
        assertFalse(piece.isFinished());

        // END 위치로 이동
        piece.moveTo(Position.END);
        assertEquals(Position.END, piece.getPosition());
        assertTrue(piece.isFinished());
    }

    @Test
    void testPathContext() {
        // 경로 문맥 설정
        piece.setPathContextWaypoint(Position.DIA_A2);
        assertEquals(Position.DIA_A2, piece.getPathContextWaypoint());

        // 마지막 진입 지점 설정
        piece.setLastEnteredWaypoint(Position.CENTER);
        assertEquals(Position.CENTER, piece.getLastEnteredWaypoint());

        // 경로 문맥 초기화
        piece.clearPathContext();
        assertNull(piece.getPathContextWaypoint());
    }

    @Test
    void testOffboardMovement() {
        // OFFBOARD로 이동 시 경로 문맥이 초기화되는지 확인
        piece.setPathContextWaypoint(Position.DIA_A2);
        piece.setLastEnteredWaypoint(Position.CENTER);
        
        piece.moveTo(Position.OFFBOARD);
        
        assertNull(piece.getPathContextWaypoint());
        assertEquals(Position.OFFBOARD, piece.getPosition());
    }

    @Test
    void testEndMovement() {
        // END로 이동 시 경로 문맥이 초기화되는지 확인
        piece.setPathContextWaypoint(Position.DIA_A2);
        piece.setLastEnteredWaypoint(Position.CENTER);
        
        piece.moveTo(Position.END);
        
        assertNull(piece.getPathContextWaypoint());
        assertTrue(piece.isFinished());
    }
} 