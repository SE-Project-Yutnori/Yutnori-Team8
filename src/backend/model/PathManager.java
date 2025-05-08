// File: src/backend/model/PathManager.java
package backend.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathManager {

    public static final List<Position> OUTER = List.of(
            Position.POS_0,  Position.POS_1,  Position.POS_2,  Position.POS_3,  Position.POS_4,
            Position.POS_5,  Position.POS_6,  Position.POS_7,  Position.POS_8,  Position.POS_9,
            Position.POS_10, Position.POS_11, Position.POS_12, Position.POS_13, Position.POS_14,
            Position.POS_15, Position.POS_16, Position.POS_17, Position.POS_18, Position.POS_19
    );

    public static final List<Position> DIAG_A_FULL = List.of(
            Position.POS_5,  Position.DIA_A1, Position.DIA_A2, Position.CENTER,
            Position.DIA_A3, Position.DIA_A4, Position.POS_15
    );

    public static final List<Position> DIAG_B_FULL = List.of(
            Position.POS_10, Position.DIA_B1, Position.DIA_B2, Position.CENTER,
            Position.DIA_B3, Position.DIA_B4, Position.POS_0
    );

    public static final List<Position> DIAG_A_TO_B = List.of(
            Position.CENTER, Position.DIA_B3, Position.DIA_B4, Position.POS_0
    );
    
    private static final Map<BoardShape, Map<Character, Position>> DIAG_ENDPOINTS = new HashMap<>();

    public static List<Position> getNextPositions(Piece piece, int steps, BoardShape shape) {
        Position cur = piece.getPosition();
        Position ctx = piece.getPathContextWaypoint();
        List<Position> outer = shape.getOuterPath();
        List<Position> path = new ArrayList<>();

        // Early exit conditions
        if (steps == 0) return path;

        if (cur == Position.OFFBOARD) {
            if (steps < outer.size()) path.add(outer.get(steps));
            else path.add(Position.END);
            return path;
        }

        if (steps < 0) return backward(cur, -steps, ctx, outer, shape);
        else return forward(cur, steps, ctx, outer, shape);
    }

    private static List<Position> backward(Position cur, int steps,
                                          Position ctx, List<Position> outer,
                                          BoardShape shape) {
        List<Position> out = new ArrayList<>();
        // 1) CENTER에서 후진
        if (cur == Position.CENTER) {
            char d = chooseDiag(ctx, shape.getDiagNames(), true, shape);
            List<Position> diag = shape.getDiagPath(d);
            int idx = diag.indexOf(cur) - steps;
            if (idx >= 0) out.add(diag.get(idx));
            else {
                int entry = outer.indexOf(diag.get(0));
                int off   = -idx;
                int oidx  = (entry - off % outer.size() + outer.size()) % outer.size();
                out.add(outer.get(oidx));
            }
            return out;
        }
        // 2) diag 내부 후진
        for (char c : shape.getDiagNames()) {
            List<Position> diag = shape.getDiagPath(c);
            int idx = diag.indexOf(cur);
            if (idx != -1) {
                int tgt = idx - steps;
                if (tgt >= 0) out.add(diag.get(tgt));
                else {
                    int entry = outer.indexOf(diag.get(0));
                    int off   = -tgt;
                    int oidx  = (entry - off % outer.size() + outer.size()) % outer.size();
                    out.add(outer.get(oidx));
                }
                return out;
            }
        }
        // 3) outer 후진
        int oidx = outer.indexOf(cur);
        if (oidx != -1) {
            int tgt = (oidx - steps % outer.size() + outer.size()) % outer.size();
            out.add(outer.get(tgt));
        } else {
            out.add(cur);
        }
        return out;
    }

    private static List<Position> forward(Position cur, int steps,
                                         Position ctx, List<Position> outer,
                                         BoardShape shape) {
        List<Position> out = new ArrayList<>();
        
        // 1) CENTER 앞으로
        if (cur == Position.CENTER) {
            // 모든 보드 형태에서는 도착점(END)으로 가는 최단 경로를 선택
            char d = chooseDiag(ctx, shape.getDiagNames(), false, shape);
            List<Position> diag = shape.getDiagPath(d);
            int cIdx = diag.indexOf(Position.CENTER) + steps;
            return advanceWithPathCheck(diag, cIdx, outer, shape);
        }
        
        // 2) outer→diag 입구 (지름길 시작점에 있는 경우)
        for (char c : shape.getDiagNames()) {
            List<Position> diag = shape.getDiagPath(c);
            if (diag.get(0) == cur) return advanceWithPathCheck(diag, steps, outer, shape);
        }
        
        // 3) diag 내부 앞으로 (지름길 안에 있는 경우)
        for (char c : shape.getDiagNames()) {
            List<Position> diag = shape.getDiagPath(c);
            int idx = diag.indexOf(cur);
            if (idx != -1) {
                // 지름길에서 움직여 END를 넘어가는 경우 확인
                if (diag.get(diag.size() - 1) == Position.END && idx + steps >= diag.size()) {
                    out.add(Position.END);
                    return out;
                }
                return advanceWithPathCheck(diag, idx + steps, outer, shape);
            }
        }
        
        // 4) outer 앞으로 (일반 경로)
        int oidx = outer.indexOf(cur);
        if (oidx != -1) {
            int dest = oidx + steps;
            
            // 지름길 입구에 정확히 도착하는 경우
            for (char c : shape.getDiagNames()) {
                int eIdx = outer.indexOf(shape.getDiagPath(c).get(0));
                if (dest == eIdx) {
                    out.add(shape.getDiagPath(c).get(0));
                    return out;
                }
            }
            
            // 지름길 입구를 지나쳐 가는 경우 - 일반 경로로 진행
            if (dest < outer.size()) {
                out.add(outer.get(dest));
            } else {
                out.add(Position.END);
            }
        } else {
            out.add(cur);
        }
        return out;
    }

    private static List<Position> advanceWithPathCheck(List<Position> seq,
                                                      int idx,
                                                      List<Position> outer,
                                                      BoardShape shape) {
        List<Position> out = new ArrayList<>();
        int limit = seq.size();
        
        // 1. 지름길 안에서 이동이 완료되는 경우
        if (idx < limit) {
            out.add(seq.get(idx));
            return out;
        }
        
        // 2. 지름길 끝이 END인 경우 (특히 B 경로)
        Position exit = seq.get(limit - 1);
        if (exit == Position.END) {
            out.add(Position.END);
            return out;
        }
        
        // 3. 지름길을 벗어나는 경우 - 출구에서 outer 경로 따라 이동
        int over = idx - (limit - 1);
        int eIdx = outer.indexOf(exit);
        int oidx = eIdx + over;
        
        // 3.1 지름길을 벗어나 지름길 입구에 정확히 도달하는 경우
        for (char c : shape.getDiagNames()) {
            int entry = outer.indexOf(shape.getDiagPath(c).get(0));
            if (oidx % outer.size() == entry) {
                out.add(shape.getDiagPath(c).get(0));
                return out;
            }
        }
        
        // 3.2 지름길을 벗어나 END에 도달하거나 그 이상 가는 경우
        if (oidx >= outer.size()) {
            out.add(Position.END);
        } else {
            // 3.3 일반 outer 경로 상의 위치
            out.add(outer.get(oidx));
        }
        return out;
    }
    
    private static char chooseDiag(Position ctx,
                                  List<Character> diags,
                                  boolean isStop,
                                  BoardShape shape) {
        // 이전 경로 컨텍스트가 있으면 해당 경로 유지
    	if (ctx != null) {
            for (char c : diags) {
                if (ctx.name().contains("DIA_" + c)) return c;
                // CENTER에서 이동 중이고, 이전에 특정 지름길에서 왔다면 해당 경로 유지
                if (ctx == Position.DIA_A2 || ctx == Position.DIA_B2 || ctx == Position.DIA_C2) {
                    return ctx.name().charAt(4); // DIA_X2에서 X 추출
                }
            }
        }
    	
    	if (!isStop) {
            return shape.getDefaultCenterExitPath();
        }
        
        // 경로 선택: END까지 거리 기준으로 정렬
        List<Character> sorted = new ArrayList<>(diags);
        sorted.sort(Comparator.comparingInt(c -> getDistanceToEnd(c, shape)));
        
        // isStop이 false면 가장 짧은 경로 선택 (sorted.get(0))
        // isStop이 true이고 선택지가 여러 개면 두 번째로 짧은 경로 선택 (sorted.get(1))
        return sorted.get(0);
    }
    
    private static int getDistanceToEnd(char diag, BoardShape shape) {
        // Initialize cache for this shape if needed
        DIAG_ENDPOINTS.computeIfAbsent(shape, k -> new HashMap<>());
        Map<Character, Position> endpoints = DIAG_ENDPOINTS.get(shape);
        
        // Get or calculate endpoint
        if (!endpoints.containsKey(diag)) {
            List<Position> diagPath = shape.getDiagPath(diag);
            endpoints.put(diag, diagPath.get(diagPath.size() - 1));
        }
        
        Position endpoint = endpoints.get(diag);
        if (endpoint == Position.END) return 0;
        
        // Calculate distance
        List<Position> outer = shape.getOuterPath();
        int idx = outer.indexOf(endpoint);
        return outer.size() - idx;
    }
}