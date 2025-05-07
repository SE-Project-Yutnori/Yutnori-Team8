package backend.model;

/**
 * 한 개의 윷말 상태를 관리하는 클래스
 */
public class Piece {
    private final Player owner;
    private Position position;
    private boolean finished;

    public Piece(Player owner) {
        this.owner = owner;
        this.finished = false;
        // 처음에는 OFFBOARD
        this.position = Position.OFFBOARD;
    }

    public void moveTo(Position newPos) {
        this.position = newPos;
        if (newPos == Position.END) {
            this.finished = true;
        }
    }

    public Player getOwner() {
        return owner;
    }

    public Position getPosition() {
        return position;
    }

    public boolean isFinished() {
        return finished;
    }
}
