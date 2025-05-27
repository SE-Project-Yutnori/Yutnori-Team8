package test.backend.game;

import org.junit.jupiter.api.Test;

import backend.game.YutThrowResult;
import backend.game.YutThrower;

import static org.junit.jupiter.api.Assertions.*;

public class YutThrowerTest {

    @Test
    void testThrowSpecified() {
        // 지정된 결과가 정확히 반환되는지 확인
        assertEquals(YutThrowResult.BACKDO, YutThrower.throwSpecified(YutThrowResult.BACKDO));
        assertEquals(YutThrowResult.DO, YutThrower.throwSpecified(YutThrowResult.DO));
        assertEquals(YutThrowResult.GAE, YutThrower.throwSpecified(YutThrowResult.GAE));
        assertEquals(YutThrowResult.GEOL, YutThrower.throwSpecified(YutThrowResult.GEOL));
        assertEquals(YutThrowResult.YUT, YutThrower.throwSpecified(YutThrowResult.YUT));
        assertEquals(YutThrowResult.MO, YutThrower.throwSpecified(YutThrowResult.MO));
    }

    @Test
    void testThrowRandom() {
        // 여러 번 던져서 모든 가능한 결과가 나올 수 있는지 확인
        boolean[] results = new boolean[6];
        int attempts = 1000;
        
        for (int i = 0; i < attempts; i++) {
            YutThrowResult result = YutThrower.throwRandom();
            results[result.ordinal()] = true;
        }
        
        // 모든 결과가 최소 한 번은 나왔는지 확인
        for (boolean result : results) {
            assertTrue(result, "모든 윷 결과가 최소 한 번은 나와야 합니다.");
        }
    }

    @Test
    void testThrowRandomDistribution() {
        // 확률 분포가 예상대로인지 확인
        int[] counts = new int[6];
        int attempts = 10000;
        
        for (int i = 0; i < attempts; i++) {
            YutThrowResult result = YutThrower.throwRandom();
            counts[result.ordinal()]++;
        }
        
        // 각 결과의 확률이 예상 범위 내에 있는지 확인
        // BACKDO: 5%
        assertTrue(counts[YutThrowResult.BACKDO.ordinal()] > attempts * 0.03);
        assertTrue(counts[YutThrowResult.BACKDO.ordinal()] < attempts * 0.07);
        
        // DO: 25%
        assertTrue(counts[YutThrowResult.DO.ordinal()] > attempts * 0.20);
        assertTrue(counts[YutThrowResult.DO.ordinal()] < attempts * 0.30);
        
        // GAE: 25%
        assertTrue(counts[YutThrowResult.GAE.ordinal()] > attempts * 0.20);
        assertTrue(counts[YutThrowResult.GAE.ordinal()] < attempts * 0.30);
        
        // GEOL: 20%
        assertTrue(counts[YutThrowResult.GEOL.ordinal()] > attempts * 0.15);
        assertTrue(counts[YutThrowResult.GEOL.ordinal()] < attempts * 0.25);
        
        // YUT: 15%
        assertTrue(counts[YutThrowResult.YUT.ordinal()] > attempts * 0.10);
        assertTrue(counts[YutThrowResult.YUT.ordinal()] < attempts * 0.20);
        
        // MO: 10%
        assertTrue(counts[YutThrowResult.MO.ordinal()] > attempts * 0.05);
        assertTrue(counts[YutThrowResult.MO.ordinal()] < attempts * 0.15);
    }
} 