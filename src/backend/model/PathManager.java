// File: src/backend/model/PathManager.java
package backend.model;

import java.util.ArrayList;
import java.util.List;

public class PathManager {

    private static final List<Position> OUTER = List.of(
            Position.POS_0,  Position.POS_1,  Position.POS_2,  Position.POS_3,  Position.POS_4,
            Position.POS_5,  Position.POS_6,  Position.POS_7,  Position.POS_8,  Position.POS_9,
            Position.POS_10, Position.POS_11, Position.POS_12, Position.POS_13, Position.POS_14,
            Position.POS_15, Position.POS_16, Position.POS_17, Position.POS_18, Position.POS_19
    );

    private static final List<Position> DIAG_A_FULL = List.of(
            Position.POS_5,  // 바깥길과 공유
            Position.DIA_A1, Position.DIA_A2,
            Position.CENTER,
            Position.DIA_A3, Position.DIA_A4,
            Position.POS_15  // 바깥길과 공유
    );

    private static final List<Position> DIAG_B_FULL = List.of(
            Position.POS_10, // 바깥길과 공유
            Position.DIA_B1, Position.DIA_B2,
            Position.CENTER,
            Position.DIA_B3, Position.DIA_B4,
            Position.POS_0   // 바깥길과 공유 (도착점)
    );

    // 사용자가 직접 구현한 DIAG_A_TO_B 경로 (CENTER에서 가장 빠른 길)
    private static final List<Position> DIAG_A_TO_B = List.of(
            Position.CENTER,
            Position.DIA_B3,
            Position.DIA_B4,
            Position.POS_0
    );

    public static List<Position> getNextPositions(Position cur, int steps) {
        List<Position> path = new ArrayList<>();
        if (steps == 0) return path;

        // 1) 출발하지 않은 말(OFFBOARD)에서 시작하는 경우 (이전과 동일)
        if (cur == Position.OFFBOARD) {
            if (steps > 0) {
                if (!OUTER.isEmpty()) {
                    path.add(OUTER.get(0)); // POS_0
                } else {
                    return path;
                }
                for (int i = 1; i <= steps; i++) {
                    if (i < OUTER.size()) {
                        path.add(OUTER.get(i));
                    } else {
                        if (!path.isEmpty() && path.get(path.size() - 1) != Position.END) {
                            path.add(Position.END);
                        }
                        break;
                    }
                }
            }
            return path;
        }

        // 2) CENTER에 도달했을 때 처리법 (사용자 정의 규칙 우선)
        if (cur == Position.CENTER && steps > 0) {
            // DIAG_A_TO_B는 CENTER로 시작해야 함
            int idx = DIAG_A_TO_B.indexOf(cur); // idx should be 0 if DIAG_A_TO_B starts with CENTER
            if (idx != -1) { // CENTER가 DIAG_A_TO_B에 포함되어 있다면
                for (int i = 0; i < steps; i++) {
                    idx++;
                    if (idx < DIAG_A_TO_B.size()) {
                        Position targetPos = DIAG_A_TO_B.get(idx);
                        path.add(targetPos);
                        if (targetPos == Position.POS_0 && DIAG_A_TO_B.get(DIAG_A_TO_B.size()-1) == Position.POS_0) { // DIAG_A_TO_B 경로의 끝이 POS_0(도착)이면
                            path.remove(path.size()-1); // 마지막 POS_0 제거
                            if (path.isEmpty() || path.get(path.size()-1) != Position.END) path.add(Position.END);
                            break;
                        }
                    } else {
                        if (!path.isEmpty() && path.get(path.size() - 1) != Position.END) {
                            path.add(Position.END);
                        }
                        break;
                    }
                }
                return path;
            }
        }

        // 3) 빽도 처리 시, 지름길 시작/끝점(바깥길과 공유)에 있다면 바깥길 우선
        if (steps < 0) { // 빽도일 경우
            if (cur == Position.POS_5 || cur == Position.POS_15 || cur == Position.POS_10 || cur == Position.POS_0) {
                // 이 지점들은 바깥길에도 속하므로, 빽도는 바깥길 규칙을 따름
                // (아래 OUTER.contains(cur) 블록에서 처리될 것임)
            } else if (DIAG_A_FULL.contains(cur)) { // 순수 지름길 내부에서의 빽도 (공유점 제외)
                int currentIndex = DIAG_A_FULL.indexOf(cur);
                for (int i = 0; i < Math.abs(steps); i++) {
                    currentIndex--;
                    if (currentIndex >= 0) {
                        path.add(DIAG_A_FULL.get(currentIndex));
                    } else { // 지름길 시작 이전으로 가려 하면, 해당 지름길 시작점으로 (더 이상 뒤로 못 감)
                        if (path.isEmpty() || path.get(path.size()-1) != DIAG_A_FULL.get(0)) { // DIAG_A_FULL.get(0)은 POS_5
                            path.clear(); // 경로 비우고 시작점만 추가
                            path.add(DIAG_A_FULL.get(0));
                        }
                        break;
                    }
                }
                return path;
            } else if (DIAG_B_FULL.contains(cur)) { // 순수 지름길 내부에서의 빽도 (공유점 제외)
                int currentIndex = DIAG_B_FULL.indexOf(cur);
                for (int i = 0; i < Math.abs(steps); i++) {
                    currentIndex--;
                    if (currentIndex >= 0) {
                        path.add(DIAG_B_FULL.get(currentIndex));
                    } else {
                        if (path.isEmpty() || path.get(path.size()-1) != DIAG_B_FULL.get(0)) { // DIAG_B_FULL.get(0)은 POS_10
                            path.clear();
                            path.add(DIAG_B_FULL.get(0));
                        }
                        break;
                    }
                }
                return path;
            }
            // 빽도인데 위 조건들에 해당 안되면, 아래 바깥길 로직으로 넘어감
        }


        // 4) 일반 전진 시, 지름길 처리 (CENTER는 이미 위에서 처리됨)
        if (steps > 0) {
            if (DIAG_A_FULL.contains(cur) && cur != Position.CENTER) {
                int currentIndex = DIAG_A_FULL.indexOf(cur);
                for (int i = 0; i < steps; i++) {
                    currentIndex++;
                    if (currentIndex < DIAG_A_FULL.size()) {
                        path.add(DIAG_A_FULL.get(currentIndex));
                    } else {
                        if (path.isEmpty() || path.get(path.size() - 1) != Position.END) path.add(Position.END);
                        break;
                    }
                }
                return path;
            }
            if (DIAG_B_FULL.contains(cur) && cur != Position.CENTER) {
                int currentIndex = DIAG_B_FULL.indexOf(cur);
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
                return path;
            }
        }

        // 5) 바깥쪽 경로 (OUTER) 처리 (빽도 포함)
        if (OUTER.contains(cur)) {
            int currentIndex = OUTER.indexOf(cur);
            if (steps > 0) { // 앞으로 이동
                for (int i_loop_counter = 0; i_loop_counter < steps; i_loop_counter++) {
                    currentIndex++;
                    if (currentIndex >= OUTER.size()) {
                        if (path.isEmpty() || path.get(path.size() - 1) != Position.END) path.add(Position.END);
                        break;
                    }
                    path.add(OUTER.get(currentIndex));
                    // POS_0에 도착하면(출발점이자 도착점), 그리고 시작 위치가 POS_0이 아니었다면 END 처리
                    if (OUTER.get(currentIndex) == Position.POS_0 && cur != Position.POS_0) {
                        path.remove(path.size()-1); // 마지막 POS_0 제거
                        if (path.isEmpty() || path.get(path.size()-1) != Position.END) path.add(Position.END);
                        break;
                    }
                }
            } else { // 뒤로 이동 (빽도)
                for (int i_loop_counter = 0; i_loop_counter < Math.abs(steps); i_loop_counter++) {
                    currentIndex--;
                    if (currentIndex < 0) { // POS_0에서 빽도하면 POS_19로
                        path.add(OUTER.get(OUTER.size() - 1));
                    } else {
                        path.add(OUTER.get(currentIndex));
                    }
                }
            }
            return path;
        }

        return path; // 모든 조건에 해당하지 않으면 빈 경로 (예: END에서 이동 시도)
    }
}