package test.backend.game;

import org.junit.jupiter.api.Test;
import backend.game.YutThrowResult;

import static org.junit.jupiter.api.Assertions.*;

public class YutThrowResultTest {

    @Test
    void testMoveValues() {
        assertEquals(-1, YutThrowResult.BACKDO.getMove());
        assertEquals(1, YutThrowResult.DO.getMove());
        assertEquals(2, YutThrowResult.GAE.getMove());
        assertEquals(3, YutThrowResult.GEOL.getMove());
        assertEquals(4, YutThrowResult.YUT.getMove());
        assertEquals(5, YutThrowResult.MO.getMove());
    }

    @Test
    void testFromString() {
        assertEquals(YutThrowResult.BACKDO, YutThrowResult.fromString("backdo"));
        assertEquals(YutThrowResult.DO, YutThrowResult.fromString("do"));
        assertEquals(YutThrowResult.GAE, YutThrowResult.fromString("gae"));
        assertEquals(YutThrowResult.GEOL, YutThrowResult.fromString("geol"));
        assertEquals(YutThrowResult.YUT, YutThrowResult.fromString("yut"));
        assertEquals(YutThrowResult.MO, YutThrowResult.fromString("mo"));
    }

    @Test
    void testFromStringCaseInsensitive() {
        assertEquals(YutThrowResult.BACKDO, YutThrowResult.fromString("BACKDO"));
        assertEquals(YutThrowResult.DO, YutThrowResult.fromString("DO"));
        assertEquals(YutThrowResult.GAE, YutThrowResult.fromString("GAE"));
    }

    @Test
    void testFromStringInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            YutThrowResult.fromString("invalid");
        });
    }
} 