package backend.game;

import java.util.Random;

public class YutThrower {
    private static final Random random = new Random();

    public static YutThrowResult throwRandom() {
        int rand = random.nextInt(100);
        if (rand < 5) return YutThrowResult.BACKDO;
        if (rand < 30) return YutThrowResult.DO;
        if (rand < 55) return YutThrowResult.GAE;
        if (rand < 75) return YutThrowResult.GEOL;
        if (rand < 90) return YutThrowResult.YUT;
        return YutThrowResult.MO;
    }

    public static YutThrowResult throwSpecified(YutThrowResult result) {
        return result;
    }
}
