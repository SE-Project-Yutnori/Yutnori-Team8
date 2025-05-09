// File: src/backend/model/Piece.java
package backend.model;

// 경로 문맥 저장을 위해 PathManager의 경로 리스트를 참조할 수 있도록 import (선택적)
// import backend.model.PathManager;

public class Piece {
    private final Player owner;
    private Position position;
    private boolean finished;
    private Position pathContextWaypoint;
    private Position lastEnteredWaypoint; 

    // 경로 문맥: 이 말이 CENTER로 진입하기 직전의 주요 분기점 (예: DIA_A2 또는 DIA_B2)
    // 또는 CENTER에서 특정 경로(예: DIAG_A_TO_B)로 나갔음을 표시하기 위해 CENTER를 저장할 수도 있음.
    // 또는 말이 특정 지름길의 출구(POS_15, POS_0)에 도달했을 때, 어떤 지름길에서 왔는지 표시.

    public Piece(Player owner) {
        this.owner = owner;
        this.finished = false;
        this.position = Position.OFFBOARD;
        this.pathContextWaypoint = null;
    }

    public void moveTo(Position newPos) {
        this.position = newPos;
        if (newPos == Position.END) {
            this.finished = true;
            clearPathContext(); // 도착 시 문맥 초기화
        }
        // 말이 OFFBOARD로 이동하면 (예: 잡혔을 때) 경로 문맥 초기화
        if (this.position == Position.OFFBOARD) {
            clearPathContext();
        }
        // 그 외의 경우, 경로 문맥은 GameController에서 명시적으로 관리
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

    public Position getPathContextWaypoint() {
        return pathContextWaypoint;
    }
    
    public Position getLastEnteredWaypoint() {
        return lastEnteredWaypoint;
    }

    // GameController에서 말이 특정 지점에 도달/통과했을 때 호출
    public void setPathContextWaypoint(Position waypoint) {
        // 유효한 문맥 정보로 간주되는 Position들에서만 설정
        // 예: DIA_A2, DIA_B2 (CENTER 진입 전)
        //     CENTER (DIAG_A_TO_B 경로로 CENTER에서 나갈 때)
        //     DIA_A4 (POS_15 도착 전), DIA_B4 (POS_0 도착 전)
        // 이 메소드의 호출 시점과 waypoint 값은 GameController가 결정
        this.pathContextWaypoint = waypoint;
    }
    
    public void setLastEnteredWaypoint(Position waypoint) {
        this.lastEnteredWaypoint = waypoint;
        System.out.println("DEBUG - Set last entered waypoint: " + waypoint);
    }

    public void clearPathContext() {
        this.pathContextWaypoint = null;
    }
}