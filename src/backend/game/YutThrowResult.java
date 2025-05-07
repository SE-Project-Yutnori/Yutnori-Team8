package backend.game;

public enum YutThrowResult {
    BACKDO(-1), DO(1), GAE(2), GEOL(3), YUT(4), MO(5);

    private final int move;

    YutThrowResult(int move) {
        this.move = move;
    }

    public int getMove() {
        return move;
    }

    public static YutThrowResult fromString(String s) {
        return switch (s.toLowerCase()) {
            case "backdo" -> BACKDO;
            case "do" -> DO;
            case "gae" -> GAE;
            case "geol" -> GEOL;
            case "yut" -> YUT;
            case "mo" -> MO;
            default -> throw new IllegalArgumentException("Invalid yut result: " + s);
        };
    }
}
