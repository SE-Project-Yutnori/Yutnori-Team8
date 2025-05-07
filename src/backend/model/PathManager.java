// File: backend/model/PathManager.java
package backend.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 경로 계산기: POS_5/POS_10 지름길 진입,
 * CENTER 통과 후 A→B 자동 전환 처리
 */
public class PathManager {
    // 1) 외곽 20칸 (시계 반대 방향)
    private static final List<Position> OUTER = List.of(
            Position.POS_0,  Position.POS_1,  Position.POS_2,  Position.POS_3,  Position.POS_4,
            Position.POS_5,  Position.POS_6,  Position.POS_7,  Position.POS_8,  Position.POS_9,
            Position.POS_10, Position.POS_11, Position.POS_12, Position.POS_13, Position.POS_14,
            Position.POS_15, Position.POS_16, Position.POS_17, Position.POS_18, Position.POS_19
    );

    // 2) A 지름길 전체 경로
    //   POS_5 → DIA_A1 → DIA_A2 → CENTER → DIA_A3 → DIA_A4 → POS_15
    private static final List<Position> DIAG_A_FULL = List.of(
            Position.POS_5,
            Position.DIA_A1, Position.DIA_A2,
            Position.CENTER,
            Position.DIA_A3, Position.DIA_A4,
            Position.POS_15
    );

    // 3) B 지름길 전체 경로
    //   POS_10 → DIA_B1 → DIA_B2 → CENTER → DIA_B3 → DIA_B4 → POS_0
    private static final List<Position> DIAG_B_FULL = List.of(
            Position.POS_10,
            Position.DIA_B1, Position.DIA_B2,
            Position.CENTER,
            Position.DIA_B3, Position.DIA_B4,
            Position.POS_0
    );

    // 4) CENTER에서 A가 끝나고 B 후반으로 넘어갈 때만 사용
    private static final List<Position> DIAG_A_TO_B = List.of(
            Position.CENTER,
            Position.DIA_B3, Position.DIA_B4,
            Position.POS_0
    );

    /**
     * cur 위치에서 steps만큼 이동할 전체 경로를 순서대로 반환.
     */
    public static List<Position> getNextPositions(Position cur, int steps) {
        List<Position> path = new ArrayList<>();
        if (steps == 0) return path;

        // 1) OFFBOARD → 외곽 진입 (do/gae/…)
        if (cur == Position.OFFBOARD && steps > 0) {
            for (int i = 1; i <= steps; i++) {
                path.add(i < OUTER.size() ? OUTER.get(i) : Position.END);
            }
            return path;
        }

        // 2) CENTER에서 A 지름길을 완주하고 B로 이어갈 때
        if (cur == Position.CENTER && steps > 0) {
            int idx = DIAG_A_TO_B.indexOf(cur);
            for (int i = 1; i <= steps; i++) {
                idx++;
                path.add(idx < DIAG_A_TO_B.size() ? DIAG_A_TO_B.get(idx) : Position.END);
            }
            return path;
        }

        // 3) A 지름길 전체 경로
        if (DIAG_A_FULL.contains(cur)) {
            int idx = DIAG_A_FULL.indexOf(cur);
            for (int i = 0; i < steps; i++) {
                idx++;
                path.add(idx < DIAG_A_FULL.size() ? DIAG_A_FULL.get(idx) : Position.END);
            }
            return path;
        }

        // 4) B 지름길 전체 경로
        if (DIAG_B_FULL.contains(cur)) {
            int idx = DIAG_B_FULL.indexOf(cur);
            for (int i = 0; i < steps; i++) {
                idx++;
                path.add(idx < DIAG_B_FULL.size() ? DIAG_B_FULL.get(idx) : Position.END);
            }
            return path;
        }

        // 5) 외곽 경로 (시계 반대 방향), backdo 포함
        if (OUTER.contains(cur)) {
            int idx = OUTER.indexOf(cur);
            if (steps < 0) {
                for (int i = 0; i < -steps; i++) {
                    idx--;
                    path.add(idx >= 0 ? OUTER.get(idx) : Position.END);
                }
            } else {
                for (int i = 0; i < steps; i++) {
                    idx++;
                    path.add(idx < OUTER.size() ? OUTER.get(idx) : Position.END);
                }
            }
            return path;
        }

        // 6) 기타(END 등): 빈 리스트 반환
        return path;
    }
}
