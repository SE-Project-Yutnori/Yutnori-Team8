// File: src/backend/model/PathManager.java
package backend.model;

import java.util.ArrayList;
import java.util.List;

public class PathManager {

    // 상수 선언 (OUTER, DIAG_A_FULL, DIAG_B_FULL, DIAG_A_TO_B)은 이전과 동일
    private static final List<Position> OUTER = List.of(
            Position.POS_0,  Position.POS_1,  Position.POS_2,  Position.POS_3,  Position.POS_4,
            Position.POS_5,  Position.POS_6,  Position.POS_7,  Position.POS_8,  Position.POS_9,
            Position.POS_10, Position.POS_11, Position.POS_12, Position.POS_13, Position.POS_14,
            Position.POS_15, Position.POS_16, Position.POS_17, Position.POS_18, Position.POS_19
    );

    public static final List<Position> DIAG_A_FULL = List.of( // GameController에서 접근 가능하도록 public으로 변경 (선택적)
            Position.POS_5,
            Position.DIA_A1, Position.DIA_A2, // 이 지점을 lastMajorNodeBeforeCenter로 설정 가능
            Position.CENTER,
            Position.DIA_A3, Position.DIA_A4,
            Position.POS_15
    );

    public static final List<Position> DIAG_B_FULL = List.of( // GameController에서 접근 가능하도록 public으로 변경 (선택적)
            Position.POS_10,
            Position.DIA_B1, Position.DIA_B2, // 이 지점을 lastMajorNodeBeforeCenter로 설정 가능
            Position.CENTER,
            Position.DIA_B3, Position.DIA_B4,
            Position.POS_0
    );

    // DIAG_A_TO_B는 CENTER에서 나가는 특정 경로이므로, Piece의 lastMajorNodeBeforeCenter를 CENTER로 설정할 수 있음
    public static final List<Position> DIAG_A_TO_B = List.of(
            Position.CENTER,
            Position.DIA_B3,
            Position.DIA_B4,
            Position.POS_0
    );


    public static List<Position> getNextPositions(Piece piece, int steps) { // Piece 객체를 인자로 받음
        Position cur = piece.getPosition();
        Position lastNode = piece.getLastMajorNodeBeforeCenter(); // Piece로부터 경로 문맥 가져오기
        List<Position> path = new ArrayList<>();

        if (steps == 0) return path;

        if (cur == Position.OFFBOARD) {
            // ... (OFFBOARD 처리 로직 - 이전과 동일, Piece 인자 불필요) ...
            if (steps > 0) {
                if (!OUTER.isEmpty()) {
                    path.add(OUTER.get(0));
                } else { return path; }
                for (int i = 1; i <= steps; i++) {
                    if (i < OUTER.size()) { path.add(OUTER.get(i)); }
                    else {
                        if (!path.isEmpty() && path.get(path.size()-1) != Position.END) path.add(Position.END);
                        break;
                    }
                }
            }
            return path;
        }

        // 1. CENTER에서의 처리 (빽도 및 전진)
        if (cur == Position.CENTER) {
            if (steps < 0) { // CENTER에서 빽도
                if (lastNode == Position.DIA_A2) { // A지름길에서 CENTER로 왔다가 빽도
                    int centerIdxInA = DIAG_A_FULL.indexOf(Position.CENTER);
                    for (int i = 0; i < Math.abs(steps); i++) {
                        centerIdxInA--;
                        if (centerIdxInA >= 0 && centerIdxInA < DIAG_A_FULL.indexOf(Position.CENTER) ) { // DIAG_A_FULL 내에서 후진
                            path.add(DIAG_A_FULL.get(centerIdxInA));
                        } else break; // 지름길 시작 이전으로는 안 감 (또는 시작점으로 이동)
                    }
                } else if (lastNode == Position.DIA_B2) { // B지름길에서 CENTER로 왔다가 빽도
                    int centerIdxInB = DIAG_B_FULL.indexOf(Position.CENTER);
                    for (int i = 0; i < Math.abs(steps); i++) {
                        centerIdxInB--;
                        if (centerIdxInB >= 0 && centerIdxInB < DIAG_B_FULL.indexOf(Position.CENTER)) {
                            path.add(DIAG_B_FULL.get(centerIdxInB));
                        } else break;
                    }
                } else if (lastNode == Position.CENTER && DIAG_A_TO_B.get(0) == Position.CENTER) {
                    // DIAG_A_TO_B 경로를 타고 CENTER에 "도착"한 상태에서 빽도하는 경우는 거의 없음.
                    // (DIAG_A_TO_B는 CENTER에서 나가는 경로이므로)
                    // 만약 이런 경우가 생긴다면, 기본 후진 경로(예: DIAG_A_FULL의 이전)를 따르거나 정의 필요.
                    // 임시: DIAG_A_FULL의 이전으로.
                    int centerIdxInA = DIAG_A_FULL.indexOf(Position.CENTER);
                    for (int i = 0; i < Math.abs(steps); i++) {
                        centerIdxInA--;
                        if (centerIdxInA >= 0 && centerIdxInA < DIAG_A_FULL.indexOf(Position.CENTER) ) {
                            path.add(DIAG_A_FULL.get(centerIdxInA));
                        } else break;
                    }
                }
                // lastNode가 null이거나 다른 값일 때 (예: 게임 시작 시 CENTER에 놓인 경우) 기본 빽도 경로 지정
                else if (path.isEmpty()){ // 위 조건들 모두 만족 안하면
                    int centerIdxInA = DIAG_A_FULL.indexOf(Position.CENTER);
                    for (int i = 0; i < Math.abs(steps); i++) {
                        centerIdxInA--;
                        if (centerIdxInA >= 0 && centerIdxInA < DIAG_A_FULL.indexOf(Position.CENTER)) {
                            path.add(DIAG_A_FULL.get(centerIdxInA));
                        } else break;
                    }
                }
                return path;
            } else { // CENTER에서 전진 (사용자 정의 DIAG_A_TO_B 우선)
                int idx = DIAG_A_TO_B.indexOf(cur); // cur == CENTER
                if (idx != -1) { // DIAG_A_TO_B가 CENTER로 시작한다면 idx는 0
                    for (int i = 0; i < steps; i++) {
                        idx++;
                        if (idx < DIAG_A_TO_B.size()) {
                            Position targetPos = DIAG_A_TO_B.get(idx);
                            path.add(targetPos);
                            if (targetPos == Position.POS_0 && DIAG_A_TO_B.get(DIAG_A_TO_B.size()-1) == Position.POS_0) {
                                path.remove(path.size()-1);
                                if (path.isEmpty() || path.get(path.size()-1) != Position.END) path.add(Position.END);
                                break;
                            }
                        } else {
                            if (!path.isEmpty() && path.get(path.size() - 1) != Position.END) path.add(Position.END);
                            break;
                        }
                    }
                    return path;
                }
            }
        }

        // 2. 지름길 시작/끝점(바깥길과 공유)에서의 빽도 -> 바깥길 규칙 따름
        if (steps < 0 && (cur == Position.POS_5 || cur == Position.POS_15 || cur == Position.POS_10 || cur == Position.POS_0)) {
            // 이 경우는 아래 OUTER.contains(cur) 블록에서 처리될 것임
        }
        // 3. 그 외 지름길 처리 (전진 또는 순수 지름길 내부 빽도)
        else if (DIAG_A_FULL.contains(cur)) { // cur가 CENTER가 아닌 DIAG_A_FULL 위의 점
            int currentIndex = DIAG_A_FULL.indexOf(cur);
            if (steps > 0) { // 전진
                for (int i = 0; i < steps; i++) {
                    currentIndex++;
                    if (currentIndex < DIAG_A_FULL.size()) path.add(DIAG_A_FULL.get(currentIndex));
                    else { if (path.isEmpty() || path.get(path.size()-1) != Position.END) path.add(Position.END); break; }
                }
            } else { // 빽도 (cur는 POS_5, POS_15, CENTER가 아닌 순수 지름길 내부)
                for (int i = 0; i < Math.abs(steps); i++) {
                    currentIndex--;
                    if (currentIndex >= DIAG_A_FULL.indexOf(Position.POS_5)) { // 지름길 시작점까지만 후진
                        path.add(DIAG_A_FULL.get(currentIndex));
                    } else break;
                }
            }
            return path;
        } else if (DIAG_B_FULL.contains(cur)) { // cur가 CENTER가 아닌 DIAG_B_FULL 위의 점
            int currentIndex = DIAG_B_FULL.indexOf(cur);
            if (steps > 0) { // 전진
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
                    } else { if (path.isEmpty() || path.get(path.size()-1) != Position.END) path.add(Position.END); break; }
                }
            } else { // 빽도 (cur는 POS_10, POS_0, CENTER가 아닌 순수 지름길 내부)
                for (int i = 0; i < Math.abs(steps); i++) {
                    currentIndex--;
                    if (currentIndex >= DIAG_B_FULL.indexOf(Position.POS_10)) {
                        path.add(DIAG_B_FULL.get(currentIndex));
                    } else break;
                }
            }
            return path;
        }

        // 4. 바깥쪽 경로 (OUTER) 처리
        if (OUTER.contains(cur)) {
            // ... (이전과 동일한 OUTER 경로 처리 로직) ...
            int currentIndex = OUTER.indexOf(cur);
            if (steps > 0) {
                for (int i_loop_counter = 0; i_loop_counter < steps; i_loop_counter++) {
                    currentIndex++;
                    if (currentIndex >= OUTER.size()) {
                        if (path.isEmpty() || path.get(path.size() - 1) != Position.END) path.add(Position.END);
                        break;
                    }
                    path.add(OUTER.get(currentIndex));
                    if (OUTER.get(currentIndex) == Position.POS_0 && cur != Position.POS_0) {
                        path.remove(path.size()-1);
                        if (path.isEmpty() || path.get(path.size()-1) != Position.END) path.add(Position.END);
                        break;
                    }
                }
            } else { // 빽도
                for (int i_loop_counter = 0; i_loop_counter < Math.abs(steps); i_loop_counter++) {
                    currentIndex--;
                    if (currentIndex < 0) { path.add(OUTER.get(OUTER.size() - 1)); }
                    else { path.add(OUTER.get(currentIndex)); }
                }
            }
            return path;
        }

        return path;
    }
}