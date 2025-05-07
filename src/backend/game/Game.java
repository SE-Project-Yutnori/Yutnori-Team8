package backend.game;

import backend.model.*;
import java.util.*;

/** 게임 전체 로직 */
public class Game {
    private final List<Player> players = new ArrayList<>();
    private final Board board;
    private int currentPlayer = 0;

    public Game(int playerCount, int pieceCount) {
        for (int i = 1; i <= playerCount; i++) {
            players.add(new Player("Player " + i, pieceCount));
        }
        board = new Board();
        // 모든 말 OFFBOARD(초기값), POS_0엔 아무도 없음
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayer);
    }

    public void nextTurn() {
        currentPlayer = (currentPlayer + 1) % players.size();
    }

    public boolean checkWin(Player p) {
        return p.getPieces().stream().allMatch(Piece::isFinished);
    }

    public Board getBoard() {
        return board;
    }

    public List<Player> getPlayers() {
        return players;
    }
}
