package test.backend.model;

import org.junit.jupiter.api.Test;
import backend.model.Board;
import backend.model.Piece;
import backend.model.Player;
import backend.model.Position;

import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

class BoardTest {
    
    private Board board;
    private Player player1, player2;
    private Piece piece1, piece2;
    
    @BeforeEach
    void setUp() {
        board = new Board();
        player1 = new Player("Player 1", 4);
        player2 = new Player("Player 2", 4);
        piece1 = new Piece(player1);
        piece2 = new Piece(player2);
    }
    
    @Test
    void testInitialBoardState() {
        // 모든 위치에 빈 리스트가 있어야 함
        for (Position pos : Position.values()) {
            if (pos != Position.OFFBOARD && pos != Position.END) {
                assertTrue(board.getPiecesAt(pos).isEmpty());
            }
        }
        assertTrue(board.getFinishedPieces().isEmpty());
    }
    
    @Test
    void testGetPiecesAtSpecialPositions() {
        // OFFBOARD와 END 위치에 대한 요청 시 빈 리스트 반환
        assertTrue(board.getPiecesAt(Position.OFFBOARD).isEmpty());
        assertTrue(board.getPiecesAt(Position.END).isEmpty());
    }
    
    @Test
    void testPlacePieceNormalPosition() {
        boolean captured = board.placePiece(piece1, Position.POS_0);
        assertFalse(captured);
        assertEquals(Position.POS_0, piece1.getPosition());
        assertTrue(board.getPiecesAt(Position.POS_0).contains(piece1));
    }
    
    @Test
    void testPlacePieceToEnd() {
        boolean captured = board.placePiece(piece1, Position.END);
        assertFalse(captured);
        assertEquals(Position.END, piece1.getPosition());
        assertTrue(piece1.isFinished());
        assertTrue(board.getFinishedPieces().contains(piece1));
    }
    
    @Test
    void testCapturePiece() {
        // piece2를 POS_5에 먼저 배치
        board.placePiece(piece2, Position.POS_5);
        assertEquals(Position.POS_5, piece2.getPosition());
        
        // piece1을 같은 위치에 배치하여 piece2를 잡기
        boolean captured = board.placePiece(piece1, Position.POS_5);
        assertTrue(captured);
        
        // piece1은 POS_5에 있고, piece2는 OFFBOARD로 이동
        assertEquals(Position.POS_5, piece1.getPosition());
        assertEquals(Position.OFFBOARD, piece2.getPosition());
        assertTrue(board.getPiecesAt(Position.POS_5).contains(piece1));
        assertFalse(board.getPiecesAt(Position.POS_5).contains(piece2));
    }
    
    @Test
    void testSamePlayerPiecesNotCaptured() {
        Piece anotherPiece1 = new Piece(player1);
        
        // 같은 플레이어의 두 말을 같은 위치에 배치
        board.placePiece(piece1, Position.POS_10);
        boolean captured = board.placePiece(anotherPiece1, Position.POS_10);
        
        assertFalse(captured);
        assertEquals(2, board.getPiecesAt(Position.POS_10).size());
        assertTrue(board.getPiecesAt(Position.POS_10).contains(piece1));
        assertTrue(board.getPiecesAt(Position.POS_10).contains(anotherPiece1));
    }
    
    @Test
    void testRemovePiece() {
        board.placePiece(piece1, Position.POS_15);
        assertTrue(board.getPiecesAt(Position.POS_15).contains(piece1));
        
        board.removePiece(piece1);
        assertFalse(board.getPiecesAt(Position.POS_15).contains(piece1));
    }
}