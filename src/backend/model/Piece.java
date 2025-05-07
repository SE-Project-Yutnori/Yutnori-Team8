// File: src/backend/model/Piece.java
package backend.model;

import java.util.List; // 경로 문맥 저장을 위해 추가 (선택적)

public class Piece {
    private final Player owner;
    private Position position;
    private boolean finished;

    // 경로 문맥: 이 말이 현재 어떤 주요 경로 위에 있는지, 또는 CENTER로 오기 직전 위치
    // 방법 1: 직전 주요 위치 저장
    private Position lastMajorNodeBeforeCenter = null;
    // 방법 2: 현재 경로 타입 저장 (더 일반적일 수 있음)
    // public enum PathType { OUTER, DIAG_A, DIAG_B, CUSTOM_CENTER_EXIT }
    // private PathType currentPathType = PathType.OUTER; // 기본값

    public Piece(Player owner) {
        this.owner = owner;
        this.finished = false;
        this.position = Position.OFFBOARD;
        // this.currentPathType = PathType.OUTER; // 방법 2 사용 시 초기화
    }

    public void moveTo(Position newPos) {
        this.position = newPos;
        if (newPos == Position.END) {
            this.finished = true;
        }
        // CENTER를 벗어나면 경로 문맥 초기화 (방법 1 사용 시)
        if (this.position != Position.CENTER && this.lastMajorNodeBeforeCenter != null) {
            if(!(this.position == Position.DIA_A3 && this.lastMajorNodeBeforeCenter == Position.DIA_A2) && // A경로 진행
                    !(this.position == Position.DIA_B3 && this.lastMajorNodeBeforeCenter == Position.DIA_B2) && // B경로 진행
                    !(PathManager.DIAG_A_TO_B.contains(this.position) && this.lastMajorNodeBeforeCenter == Position.CENTER) // 커스텀 경로 진행 (만약 CENTER가 lastMajorNode가 될 수 있다면)
            ) {
                // this.lastMajorNodeBeforeCenter = null; // 주석 처리: CENTER에서 바로 다음 칸으로 갈 때도 유지 필요
            }
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

    // --- 경로 문맥 관련 메소드 (방법 1 사용) ---
    public Position getLastMajorNodeBeforeCenter() {
        return lastMajorNodeBeforeCenter;
    }

    public void setLastMajorNodeBeforeCenter(Position lastMajorNode) {
        // CENTER로 진입하는 특정 지점들에서만 설정 (예: DIA_A2, DIA_B2)
        if (lastMajorNode == Position.DIA_A2 || lastMajorNode == Position.DIA_B2 || lastMajorNode == Position.CENTER) { // CENTER는 DIAG_A_TO_B 진입 시
            this.lastMajorNodeBeforeCenter = lastMajorNode;
        } else {
            // CENTER를 완전히 벗어난 경우 null로 설정할 수 있지만, moveTo에서 이미 일부 처리.
            // 말을 잡아서 OFFBOARD로 가는 경우 등에도 초기화 필요.
            if (this.position == Position.OFFBOARD) { // 잡혔을 때
                this.lastMajorNodeBeforeCenter = null;
            }
        }
    }

    public void clearPathContext() { // 예: 말이 잡혔을 때
        this.lastMajorNodeBeforeCenter = null;
    }

    // --- 방법 2 사용 시 ---
    // public PathType getCurrentPathType() { return currentPathType; }
    // public void setCurrentPathType(PathType type) { this.currentPathType = type; }
}