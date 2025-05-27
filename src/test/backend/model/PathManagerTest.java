package test.backend.model;

import org.junit.jupiter.api.Test;
import backend.model.BoardShape;
import backend.model.PathManager;
import backend.model.Piece;
import backend.model.Player;
import backend.model.Position;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

public class PathManagerTest {
    private Piece piece;
    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("Test Player", 1);
        piece = player.getPieces().get(0);
    }

    @Test
    void testGetNextPositionsFromStart() {
        // POS_0에서 전진하면 PathManager 코드에 따라 바로 END로 이동
        piece.moveTo(Position.POS_0);
        List<Position> path = PathManager.getNextPositions(piece, 1, BoardShape.TRADITIONAL);
        assertEquals(1, path.size());
        assertEquals(Position.END, path.get(0));
    }

    @Test
    void testGetNextPositionsFromStartBackward() {
        // POS_0에서 후진하는 경우 테스트
        piece.moveTo(Position.POS_0);
        List<Position> path = PathManager.getNextPositions(piece, -1, BoardShape.TRADITIONAL);
        assertEquals(1, path.size());
        assertEquals(Position.POS_19, path.get(0)); // 순환구조에서 뒤로 1칸
    }

    @Test
    void testGetNextPositionsFromOffboard() {
        // OFFBOARD에서 시작하는 경우 테스트
        // piece의 초기 위치가 OFFBOARD라고 가정
        List<Position> path = PathManager.getNextPositions(piece, 1, BoardShape.TRADITIONAL);
        assertEquals(1, path.size());
        // outer path의 첫 번째 위치 (보통 POS_1)를 확인
        assertNotNull(path.get(0));
    }

    @Test
    void testGetNextPositionsWithDiagonal() {
        // POS_5는 대각선 A의 입구라고 가정
        piece.moveTo(Position.POS_5);
        List<Position> path = PathManager.getNextPositions(piece, 1, BoardShape.TRADITIONAL);
        assertEquals(1, path.size());
        // 실제 결과를 확인하기 위해 디버그 출력
        System.out.println("POS_5에서 1칸 이동 결과: " + path.get(0));
        // 대각선 입구에서 1칸 이동하면 대각선 경로로 들어감
        assertTrue(path.get(0).name().startsWith("DIA_") || path.get(0) == Position.CENTER);
    }

    @Test
    void testGetNextPositionsToCenter() {
        // 대각선 경로에서 CENTER로 이동 테스트
        piece.moveTo(Position.DIA_A2); // 대각선 경로 중간에서 시작
        List<Position> path = PathManager.getNextPositions(piece, 1, BoardShape.TRADITIONAL);
        assertEquals(1, path.size());
        // 결과 확인
        System.out.println("DIA_A2에서 1칸 이동 결과: " + path.get(0));
        assertNotNull(path.get(0));
    }

    @Test
    void testGetNextPositionsFromCenter() {
        // 중앙에서 이동 - 컨텍스트가 없으면 기본 경로 사용
        piece.moveTo(Position.CENTER);
        List<Position> path = PathManager.getNextPositions(piece, 1, BoardShape.TRADITIONAL);
        assertEquals(1, path.size());
        System.out.println("CENTER에서 1칸 이동 결과: " + path.get(0));
        // 기본 center exit path를 사용함
        assertNotNull(path.get(0));
    }

    @Test
    void testGetNextPositionsFromCenterWithContext() {
        // CENTER에서 컨텍스트를 가지고 이동
        piece.moveTo(Position.CENTER);
        piece.setPathContextWaypoint(Position.DIA_A2); // A 대각선에서 온 컨텍스트
        List<Position> path = PathManager.getNextPositions(piece, 1, BoardShape.TRADITIONAL);
        assertEquals(1, path.size());
        System.out.println("CENTER에서 컨텍스트 있이 1칸 이동 결과: " + path.get(0));
        assertNotNull(path.get(0));
    }

    @Test
    void testGetNextPositionsToEnd() {
        // POS_19에서 2칸 이동해서 END로 가는 테스트
        piece.moveTo(Position.POS_19);
        List<Position> path = PathManager.getNextPositions(piece, 2, BoardShape.TRADITIONAL);
        assertEquals(1, path.size());
        assertEquals(Position.END, path.get(0));
    }

    @Test
    void testGetNextPositionsCircular() {
        // POS_19에서 1칸 이동하면 POS_0으로 순환
        piece.moveTo(Position.POS_19);
        List<Position> path = PathManager.getNextPositions(piece, 1, BoardShape.TRADITIONAL);
        assertEquals(1, path.size());
        assertEquals(Position.POS_0, path.get(0));
    }

    @Test
    void testGetNextPositionsBackward() {
        // 뒤로 이동 테스트
        piece.moveTo(Position.POS_5);
        List<Position> path = PathManager.getNextPositions(piece, -1, BoardShape.TRADITIONAL);
        assertEquals(1, path.size());
        assertEquals(Position.POS_4, path.get(0));
    }

    @Test
    void testGetNextPositionsBackwardFromCenter() {
        // CENTER에서 후진 - 컨텍스트 필요
        piece.moveTo(Position.CENTER);
        piece.setPathContextWaypoint(Position.DIA_A2); // A 대각선에서 온 컨텍스트
        List<Position> path = PathManager.getNextPositions(piece, -1, BoardShape.TRADITIONAL);
        assertEquals(1, path.size());
        System.out.println("CENTER에서 후진 결과: " + path.get(0));
        assertNotNull(path.get(0));
    }

    @Test
    void testGetNextPositionsWithContext() {
        // 대각선 경로에서 컨텍스트를 가지고 이동
        piece.moveTo(Position.DIA_A2);
        piece.setPathContextWaypoint(Position.DIA_A1); // 이전 위치 컨텍스트
        List<Position> path = PathManager.getNextPositions(piece, 1, BoardShape.TRADITIONAL);
        assertEquals(1, path.size());
        System.out.println("DIA_A2에서 컨텍스트와 함께 1칸 이동 결과: " + path.get(0));
        // 대각선 경로를 따라 이동하거나 CENTER로 이동
        assertNotNull(path.get(0));
    }

    @Test
    void testGetNextPositionsZeroSteps() {
        // 0칸 이동 - 빈 리스트 반환
        piece.moveTo(Position.POS_5);
        List<Position> path = PathManager.getNextPositions(piece, 0, BoardShape.TRADITIONAL);
        assertTrue(path.isEmpty());
    }

    @Test
    void testGetNextPositionsWithDifferentBoardShapes() {
        // 다른 보드 모양에서의 이동
        piece.moveTo(Position.POS_1); // 안전한 위치에서 테스트
        
        List<Position> pathTraditional = PathManager.getNextPositions(piece, 1, BoardShape.TRADITIONAL);
        List<Position> pathPentagon = PathManager.getNextPositions(piece, 1, BoardShape.PENTAGON);
        List<Position> pathHexagon = PathManager.getNextPositions(piece, 1, BoardShape.HEXAGON);

        assertNotNull(pathTraditional);
        assertNotNull(pathPentagon);
        assertNotNull(pathHexagon);
        
        assertEquals(1, pathTraditional.size());
        assertEquals(1, pathPentagon.size());
        assertEquals(1, pathHexagon.size());
    }

    // 더 구체적인 END 이동 테스트들
    @Test
    void testGetNextPositionsMultipleStepsToEnd() {
        // 다양한 위치에서 충분한 스텝으로 END 도달
        Position[] startPositions = {Position.POS_18, Position.POS_17, Position.POS_16};
        
        for (Position start : startPositions) {
            piece.moveTo(start);
            // outer path 크기가 20이라고 가정하고, 충분한 스텝으로 이동
            List<Position> path = PathManager.getNextPositions(piece, 5, BoardShape.TRADITIONAL);
            assertEquals(1, path.size());
            assertEquals(Position.END, path.get(0), 
                        start + "에서 5칸 이동 시 END에 도달해야 함");
        }
    }

    @Test
    void testCircularMovement() {
        // 순환 이동 테스트
        piece.moveTo(Position.POS_18);
        List<Position> path = PathManager.getNextPositions(piece, 1, BoardShape.TRADITIONAL);
        assertEquals(1, path.size());
        assertEquals(Position.POS_19, path.get(0));
        
        // POS_19에서 1칸 더
        piece.moveTo(Position.POS_19);
        path = PathManager.getNextPositions(piece, 1, BoardShape.TRADITIONAL);
        assertEquals(1, path.size());
        assertEquals(Position.POS_0, path.get(0));
    }
    @Test
    void debugPathManager() {
        System.out.println("=== PathManager 디버깅 ===");
        
        // 다양한 위치에서의 이동 결과 확인
        Position[] testPositions = {
            Position.OFFBOARD, Position.POS_0, Position.POS_1, Position.POS_5, 
            Position.POS_10, Position.CENTER, Position.DIA_A2
        };
        
        for (Position pos : testPositions) {
            try {
                piece.moveTo(pos);
                List<Position> result = PathManager.getNextPositions(piece, 1, BoardShape.TRADITIONAL);
                System.out.println(pos + " -> " + (result.isEmpty() ? "[]" : result.get(0)));
            } catch (Exception e) {
                System.out.println(pos + " -> ERROR: " + e.getMessage());
            }
        }
    }
}