package test.backend.model;

import org.junit.jupiter.api.Test;

import backend.model.Piece;
import backend.model.Player;
import backend.model.Position;

import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PieceTest {
    
    private Player player;
    private Piece piece;
    
    @BeforeEach
    void setUp() {
        player = new Player("Test Player", 4);
        piece = new Piece(player);
    }
    
    @Test
    void testInitialState() {
        assertEquals(player, piece.getOwner());
        assertEquals(Position.OFFBOARD, piece.getPosition());
        assertFalse(piece.isFinished());
        assertNull(piece.getPathContextWaypoint());
        assertNull(piece.getLastEnteredWaypoint());
    }
    
    @Test
    void testMoveTo() {
        piece.moveTo(Position.POS_5);
        assertEquals(Position.POS_5, piece.getPosition());
        assertFalse(piece.isFinished());
    }
    
    @Test
    void testMoveToEnd() {
        piece.moveTo(Position.END);
        assertEquals(Position.END, piece.getPosition());
        assertTrue(piece.isFinished());
        assertNull(piece.getPathContextWaypoint()); // 도착 시 문맥 초기화
    }
    
    @Test
    void testMoveToOffboard() {
        // 먼저 다른 위치로 이동
        piece.setPathContextWaypoint(Position.DIA_A2);
        piece.moveTo(Position.POS_10);
        
        // OFFBOARD로 이동 시 문맥 초기화
        piece.moveTo(Position.OFFBOARD);
        assertEquals(Position.OFFBOARD, piece.getPosition());
        assertNull(piece.getPathContextWaypoint());
    }
    
    @Test
    void testPathContextWaypoint() {
        piece.setPathContextWaypoint(Position.DIA_A2);
        assertEquals(Position.DIA_A2, piece.getPathContextWaypoint());
        
        piece.clearPathContext();
        assertNull(piece.getPathContextWaypoint());
    }
    
    @Test
    void testLastEnteredWaypoint() {
        piece.setLastEnteredWaypoint(Position.DIA_B2);
        assertEquals(Position.DIA_B2, piece.getLastEnteredWaypoint());
    }
}