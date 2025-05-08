// File: backend/model/Board.java
package backend.model;

import java.util.*;

public class Board {
    private final Map<Position, List<Piece>> boardMap = new HashMap<>();
    private final List<Piece> finishedPieces = new ArrayList<>();

    public Board() {
        for (Position p : Position.values()) {
            boardMap.put(p, new ArrayList<>());
        }
    }

    public List<Piece> getPiecesAt(Position pos) {
        // Position.OFFBOARD나 Position.END에 대한 요청이 올 경우 빈 리스트 반환 또는 예외 처리
        if (pos == Position.OFFBOARD || pos == Position.END) {
            return Collections.emptyList(); // 또는 다른 적절한 처리
        }
        return boardMap.getOrDefault(pos, Collections.emptyList());
    }

    public List<Piece> getFinishedPieces() {
        return Collections.unmodifiableList(finishedPieces);
    }

    public boolean placePiece(Piece pc, Position dest) {
        removePiece(pc); // 현재 위치에서 제거

        if (dest == Position.END) {
            if (!finishedPieces.contains(pc)) { // 중복 추가 방지
                finishedPieces.add(pc);
            }
            pc.moveTo(dest); // Piece 내부에서 END 도착 시 경로 문맥 초기화됨
            return false; // 잡기 없음
        }

        boolean captured = false;
        if (dest != Position.OFFBOARD) { // 목적지가 판 위인 경우
            List<Piece> piecesAtDestination = boardMap.getOrDefault(dest, new ArrayList<>());
            List<Piece> toCapture = new ArrayList<>();
            for (Piece existingPiece : piecesAtDestination) {
                if (!existingPiece.getOwner().equals(pc.getOwner())) {
                    toCapture.add(existingPiece);
                }
            }
            for (Piece capturedPiece : toCapture) {
                piecesAtDestination.remove(capturedPiece); // 목적지에서 제거
                // boardMap.get(Position.OFFBOARD)는 없으므로, 잡힌 말은 단순히 위치만 변경
                capturedPiece.moveTo(Position.OFFBOARD); // Piece 내부에서 OFFBOARD 이동 시 경로 문맥 초기화됨
                captured = true;
            }
        }

        // 새로운 목적지에 말 배치 (OFFBOARD는 boardMap에 없음)
        if (dest != Position.OFFBOARD) {
            boardMap.computeIfAbsent(dest, k -> new ArrayList<>()).add(pc);
        }
        pc.moveTo(dest); // Piece의 현재 위치 업데이트
        // pc의 경로 문맥 업데이트는 GameController.movePiece에서 이동 후 처리

        return captured;
    }

    public void removePiece(Piece pc) {
        Position currentPos = pc.getPosition();
        if (currentPos != null && currentPos != Position.OFFBOARD && currentPos != Position.END && boardMap.containsKey(currentPos)) {
            boardMap.get(currentPos).remove(pc);
        }
        // finishedPieces에서도 제거해야 할 수 있음 (규칙에 따라)
        // finishedPieces.remove(pc);
    }
}