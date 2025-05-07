// File: backend/model/PathManager.java
package backend.model;

import java.util.ArrayList;
import java.util.List;

public class PathManager {

    // =======================================================================
    // ========= 이 상수들이 여기에 클래스 멤버로 선언되어야 합니다 =========
    // =======================================================================
    private static final List<Position> OUTER = List.of(
            Position.POS_0,  Position.POS_1,  Position.POS_2,  Position.POS_3,  Position.POS_4,
            Position.POS_5,  Position.POS_6,  Position.POS_7,  Position.POS_8,  Position.POS_9,
            Position.POS_10, Position.POS_11, Position.POS_12, Position.POS_13, Position.POS_14,
            Position.POS_15, Position.POS_16, Position.POS_17, Position.POS_18, Position.POS_19
    );

    private static final List<Position> DIAG_A_FULL = List.of(
            Position.POS_5,
            Position.DIA_A1, Position.DIA_A2,
            Position.CENTER,
            Position.DIA_A3, Position.DIA_A4,
            Position.POS_15
    );

    private static final List<Position> DIAG_B_FULL = List.of(
            Position.POS_10,
            Position.DIA_B1, Position.DIA_B2,
            Position.CENTER,
            Position.DIA_B3, Position.DIA_B4,
            Position.POS_0
    );

    // 중앙에서 B 지름길로 이동하는 경로 : POS_5 →
    private static final List<Position> DIAG_A_TO_B = List.of(
            Position.CENTER,
            Position.DIA_B3, Position.DIA_B4,
            Position.POS_0
    );

    // =======================================================================
    // =======================================================================


    public static List<Position> getNextPositions(Position cur, int steps) {
        List<Position> path = new ArrayList<>();
        if (steps == 0) return path;

        // 1) 출발하지 않은 말(OFFBOARD)에서 시작하는 경우
        if (cur == Position.OFFBOARD) {
            if (steps > 0) { // 앞으로 이동 (도, 개, 걸, 윷, 모)
                // 요구사항: OFFBOARD에서 POS_0으로 가는 것은 0칸 이동으로 취급.
                // '도'(steps=1)는 실질적으로 1칸 이동하여 POS_1에 도착.
                // 경로에는 POS_0부터 목표 지점까지 모두 포함.

                if (0 < OUTER.size()) { // OUTER가 비어있지 않은지 확인
                    path.add(OUTER.get(0)); // POS_0을 경로에 추가
                } else {
                    return path; // OUTER 리스트가 비정상적이면 빈 경로 반환
                }

                for (int i = 1; i <= steps; i++) {
                    if (i < OUTER.size()) {
                        path.add(OUTER.get(i));
                    } else {
                        if (!path.isEmpty() && path.get(path.size()-1) != Position.END) {
                            path.add(Position.END);
                        }
                        break;
                    }
                }
            } else {
                // 출발하지 않은 말은 빽도로 움직일 수 없음.
            }
            return path;
        }

        // # center에 도달했을 때 처리법
        if (cur == Position.CENTER && steps > 0) {
            int idx = DIAG_A_TO_B.indexOf(cur);
            for (int i = 1; i <= steps; i++) {
                idx++;
                if (idx < DIAG_A_TO_B.size()) path.add(DIAG_A_TO_B.get(idx));
                else { path.add(Position.END); break; }
            }
            return path;
        }

        // 2) A 지름길 (DIAG_A_FULL) 위에 있는 경우
        if (DIAG_A_FULL.contains(cur)) { // 여기서 DIAG_A_FULL 참조
            int currentIndex = DIAG_A_FULL.indexOf(cur);
            if (steps > 0) {
                for (int i = 0; i < steps; i++) {
                    currentIndex++;
                    if (currentIndex < DIAG_A_FULL.size()) {
                        path.add(DIAG_A_FULL.get(currentIndex));
                    } else {
                        if (path.isEmpty() || path.get(path.size()-1) != Position.END) path.add(Position.END);
                        break;
                    }
                }
            } else {
                for (int i = 0; i < Math.abs(steps); i++) {
                    currentIndex--;
                    if (currentIndex >= 0) {
                        path.add(DIAG_A_FULL.get(currentIndex));
                    } else {
                        if (path.isEmpty() || path.get(path.size()-1) != Position.POS_5) {
                            path.add(Position.POS_5);
                        }
                        break;
                    }
                }
            }
            return path;
        }

        // 3) B 지름길 (DIAG_B_FULL) 위에 있는 경우
        if (DIAG_B_FULL.contains(cur)) { // 여기서 DIAG_B_FULL 참조
            int currentIndex = DIAG_B_FULL.indexOf(cur);
            // ... (이하 DIAG_B_FULL 사용하는 로직은 동일하게 유지) ...
            if (steps > 0) { // 앞으로 이동
                for (int i = 0; i < steps; i++) {
                    currentIndex++;
                    if (currentIndex < DIAG_B_FULL.size()) {
                        Position targetPos = DIAG_B_FULL.get(currentIndex);
                        path.add(targetPos);
                        if (targetPos == Position.POS_0) {
                            path.remove(path.size()-1);
                            if (path.isEmpty() || path.get(path.size()-1) != Position.END) path.add(Position.END);
                            break;
                        }
                    } else {
                        if (path.isEmpty() || path.get(path.size()-1) != Position.END) path.add(Position.END);
                        break;
                    }
                }
            } else { // 뒤로 이동 (빽도)
                for (int i = 0; i < Math.abs(steps); i++) {
                    currentIndex--;
                    if (currentIndex >= 0) {
                        path.add(DIAG_B_FULL.get(currentIndex));
                    } else {
                        if (path.isEmpty() || path.get(path.size()-1) != Position.POS_10) {
                            path.add(Position.POS_10);
                        }
                        break;
                    }
                }
            }
            return path;
        }


        // 4) 바깥쪽 경로 (OUTER) 위에 있는 경우
        if (OUTER.contains(cur)) { // 여기서 OUTER 참조
            int currentIndex = OUTER.indexOf(cur);
            if (steps > 0) { // 앞으로 이동
                // for (int i = 0; i < steps; i_outer) { // 이전 오류가 있었던 부분
                for (int i_loop_counter = 0; i_loop_counter < steps; i_loop_counter++) { // 수정된 부분: 루프 변수 선언 및 증가
                    currentIndex++;
                    if (currentIndex >= OUTER.size()) {
                        if (path.isEmpty() || path.get(path.size()-1) != Position.END) path.add(Position.END);
                        break;
                    }
                    path.add(OUTER.get(currentIndex));
                    if (OUTER.get(currentIndex) == Position.POS_0 && cur != Position.POS_0) {
                        path.remove(path.size()-1);
                        if (path.isEmpty() || path.get(path.size()-1) != Position.END) path.add(Position.END);
                        break;
                    }
                }
            } else { // 뒤로 이동 (빽도)
                // for (int i_outer = 0; i_outer < Math.abs(steps); i_outer++) { // 이전 오류 가능성이 있는 부분
                for (int i_loop_counter = 0; i_loop_counter < Math.abs(steps); i_loop_counter++) { // 수정된 부분: 루프 변수 선언 및 증가
                    currentIndex--;
                    if (currentIndex < 0) {
                        path.add(OUTER.get(OUTER.size() - 1));
                    } else {
                        path.add(OUTER.get(currentIndex));
                    }
                }
            }
            return path;
        }

        return path;
    }
}