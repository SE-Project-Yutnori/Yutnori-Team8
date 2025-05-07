package backend.model;

import java.util.*;

public class Player {
    private final String name;
    private final List<Piece> pieces = new ArrayList<>();

    public Player(String name, int pieceCount) {
        this.name = name;
        for (int i = 0; i < pieceCount; i++) {
            pieces.add(new Piece(this));
        }
    }

    public String getName() {
        return name;
    }

    public List<Piece> getPieces() {
        return pieces;
    }

    public boolean hasFinishedAllPieces() {
        return pieces.stream().allMatch(Piece::isFinished);
    }
}
