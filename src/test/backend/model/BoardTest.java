package test.backend.model;

import org.junit.jupiter.api.Test;

import backend.model.Board;
import backend.model.Piece;
import backend.model.Player;
import backend.model.Position;

import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class BoardTest {
    private Board board;
    private Player player1;
    private Player player2;
    private Piece piece1;
    private Piece piece2;

    @BeforeEach
    void setUp() {
        board = new Board();
        player1 = new Player("Player 1", 2); // 2개의 말을 가진 플레이어 생성
        player2 = new Player("Player 2", 1);
        piece1 = player1.getPieces().get(0);
        piece2 = player1.getPieces().get(1); // 같은 플레이어의 두 번째 말 사용
    }

    @Test
    void testInitialBoardState() {
        // 모든 위치에 말이 없어야 함
        for (Position pos : Position.values()) {
            if (pos != Position.OFFBOARD && pos != Position.END) {
                assertTrue(board.getPiecesAt(pos).isEmpty());
            }
        }
    }

    @Test
    void testPlacePiece() {
        // 말 배치
        board.placePiece(piece1, Position.POS_0);
        List<Piece> piecesAtPos0 = board.getPiecesAt(Position.POS_0);
        assertEquals(1, piecesAtPos0.size());
        assertEquals(piece1, piecesAtPos0.get(0));
        assertEquals(Position.POS_0, piece1.getPosition());
    }

    @Test
    void testMultiplePiecesAtSamePosition() {
        // 같은 플레이어의 말 두 개를 같은 위치에 배치
        board.placePiece(piece1, Position.POS_0);
        board.placePiece(piece2, Position.POS_0);

        List<Piece> piecesAtPos0 = board.getPiecesAt(Position.POS_0);
        assertEquals(2, piecesAtPos0.size());
        assertTrue(piecesAtPos0.contains(piece1));
        assertTrue(piecesAtPos0.contains(piece2));
    }

    @Test
    void testCapturePiece() {
        // 서로 다른 플레이어의 말로 잡기 테스트
        Piece opponentPiece = player2.getPieces().get(0);
        
        board.placePiece(opponentPiece, Position.POS_0);
        board.placePiece(piece1, Position.POS_1);

        // piece1이 opponentPiece를 잡는 상황
        boolean captured = board.placePiece(piece1, Position.POS_0);

        assertTrue(captured);
        assertEquals(Position.OFFBOARD, opponentPiece.getPosition());
        assertEquals(Position.POS_0, piece1.getPosition());
    }

    @Test
    void testFinishPiece() {
        // 말이 끝에 도달하는 경우
        board.placePiece(piece1, Position.END);
        assertTrue(piece1.isFinished());
        assertTrue(board.getFinishedPieces().contains(piece1));
    }
}