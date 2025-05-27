package test.backend.model;

import org.junit.jupiter.api.Test;

import backend.model.Position;

import static org.junit.jupiter.api.Assertions.*;

public class PositionTest {
    
    @Test
    void testPositionValues() {
        // 기본 위치 값들이 존재하는지 확인
        assertNotNull(Position.OFFBOARD);
        assertNotNull(Position.END);
        assertNotNull(Position.CENTER);
        
        // 외곽 위치들이 존재하는지 확인
        assertNotNull(Position.POS_0);
        assertNotNull(Position.POS_30);
        
        // 지름길 위치들이 존재하는지 확인
        assertNotNull(Position.DIA_A1);
        assertNotNull(Position.DIA_A4);
        assertNotNull(Position.DIA_B1);
        assertNotNull(Position.DIA_B4);
    }
    
    @Test
    void testPositionOrder() {
        // 위치 값들의 순서가 올바른지 확인
        Position[] positions = Position.values();
        assertTrue(positions.length > 0);
        
        // OFFBOARD가 첫 번째 위치인지 확인
        assertEquals(Position.OFFBOARD, positions[0]);
        
        // END가 마지막 위치인지 확인
        assertEquals(Position.END, positions[positions.length - 1]);
    }
} 