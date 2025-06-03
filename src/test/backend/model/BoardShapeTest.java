package test.backend.model;

import org.junit.jupiter.api.Test;
import backend.model.BoardShape;
import backend.model.Position;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

class BoardShapeTest {
    
    @Test
    void testTraditionalBoardShape() {
        BoardShape traditional = BoardShape.TRADITIONAL;
        
        // 외곽 경로 테스트
        List<Position> outerPath = traditional.getOuterPath();
        assertEquals(21, outerPath.size()); // 0~19 + POS_0
        assertEquals(Position.POS_0, outerPath.get(0));
        assertEquals(Position.POS_19, outerPath.get(19));
        assertEquals(Position.POS_0, outerPath.get(20)); // 마지막에 POS_0 추가
        
        // 대각선 이름 테스트
        assertEquals(List.of('A', 'B'), traditional.getDiagNames());
        
        // 기본 중앙 출구 경로
        assertEquals('B', traditional.getDefaultCenterExitPath());
    }
    
    @Test
    void testPentagonBoardShape() {
        BoardShape pentagon = BoardShape.PENTAGON;
        
        List<Position> outerPath = pentagon.getOuterPath();
        assertEquals(26, outerPath.size()); // 0~24 + POS_0
        
        assertEquals(List.of('A', 'B', 'C'), pentagon.getDiagNames());
        assertEquals('B', pentagon.getDefaultCenterExitPath());
    }
    
    @Test
    void testHexagonBoardShape() {
        BoardShape hexagon = BoardShape.HEXAGON;
        
        List<Position> outerPath = hexagon.getOuterPath();
        assertEquals(31, outerPath.size()); // 0~29 + POS_0
        
        assertEquals(List.of('A', 'B', 'C'), hexagon.getDiagNames());
        assertEquals('C', hexagon.getDefaultCenterExitPath());
    }
    
    @Test
    void testTraditionalDiagPaths() {
        BoardShape traditional = BoardShape.TRADITIONAL;
        
        // A 대각선 경로 테스트
        List<Position> diagA = traditional.getDiagPath('A');
        assertEquals(7, diagA.size());
        assertEquals(Position.POS_5, diagA.get(0));
        assertEquals(Position.DIA_A1, diagA.get(1));
        assertEquals(Position.DIA_A2, diagA.get(2));
        assertEquals(Position.CENTER, diagA.get(3));
        assertEquals(Position.DIA_A3, diagA.get(4));
        assertEquals(Position.DIA_A4, diagA.get(5));
        assertEquals(Position.POS_15, diagA.get(6));
        
        // B 대각선 경로 테스트
        List<Position> diagB = traditional.getDiagPath('B');
        assertEquals(7, diagB.size());
        assertEquals(Position.POS_10, diagB.get(0));
        assertEquals(Position.CENTER, diagB.get(3));
        assertEquals(Position.POS_0, diagB.get(6));
    }
    
    @Test
    void testDistanceToEnd() {
        BoardShape traditional = BoardShape.TRADITIONAL;
        
        // B 대각선은 POS_0에서 끝나므로 거리 20
        assertEquals(20, traditional.distanceToEnd('B'));
        
        // A 대각선은 POS_15에서 끝나므로 거리 5
        assertEquals(5, traditional.distanceToEnd('A'));
    }
    
    @Test
    void testCacheDistanceToEnd() {
        BoardShape traditional = BoardShape.TRADITIONAL;
        
        // 첫 번째 호출
        int distance1 = traditional.distanceToEnd('A');
        // 두 번째 호출 (캐시에서 가져옴)
        int distance2 = traditional.distanceToEnd('A');
        
        assertEquals(distance1, distance2);
        assertEquals(5, distance1);
    }
}
