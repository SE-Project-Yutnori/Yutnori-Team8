// File: backend/model/Board.java
package backend.model;

import java.util.*;

/**
 * 말 배치·이동·잡기·업기·골인 관리
 */
public class Board {
    private final Map<Position, List<Piece>> boardMap = new HashMap<>();
    private final List<Piece> finishedPieces     = new ArrayList<>();

    public Board() {
        for (Position p : Position.values()) {
            boardMap.put(p, new ArrayList<>());
        }
    }

    /** 해당 칸의 말 목록 */
    public List<Piece> getPiecesAt(Position pos) {
        return boardMap.get(pos);
    }

    /** 완전 골인한 말 목록 (읽기 전용) */
    public List<Piece> getFinishedPieces() {
        return Collections.unmodifiableList(finishedPieces);
    }

    /**
     * 말 이동
     *  - OFFBOARD → 단순 이동
     *  - 그 외 모든 칸 → 잡기 처리
     *  - END → finishedPieces 로 이동
     *
     * @return 잡기(capture)가 발생했으면 true
     */
    public boolean placePiece(Piece pc, Position dest) {
        // 1) 이전 위치에서 제거
        removePiece(pc);

        // 2) END에 도달하면 완전 골인 처리 (잡기 없음)
        if (dest == Position.END) {
            finishedPieces.add(pc);
            pc.moveTo(dest);
            return false;
        }

        boolean captured = false;

        // 3) OFFBOARD 가 아니고 END 도 아니면, 무조건 잡기
        if (dest != Position.OFFBOARD) {
            // dest 칸에 있는 모든 상대 말 잡아서 OFFBOARD 로 돌려보냄
            List<Piece> toCapture = new ArrayList<>();
            for (Piece p : boardMap.get(dest)) {
                if (!p.getOwner().equals(pc.getOwner())) {
                    toCapture.add(p);
                }
            }
            for (Piece p : toCapture) {
                boardMap.get(dest).remove(p);
                boardMap.get(Position.OFFBOARD).add(p);  // OFFBOARD 로
                p.moveTo(Position.OFFBOARD);
                captured = true;
            }
        }

        // 4) 목적지에 말 배치
        boardMap.get(dest).add(pc);
        pc.moveTo(dest);
        return captured;
    }

    /** 모든 칸에서 해당 말을 제거 */
    public void removePiece(Piece pc) {
        for (List<Piece> list : boardMap.values()) {
            list.remove(pc);
        }
    }
}
