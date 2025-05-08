// File: src/backend/model/PathManager.java
package backend.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathManager {
    
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

        // POS_0 처리는 forward/backward 로직 전에 수행
        if (cur == Position.POS_0) {
            // steps >= 1인 경우에만 END로 처리
            if (steps >= 1) {
                path.add(Position.END);
                return path;
            }
            // 후진하는 경우는 일반 로직으로 처리
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
        	char diagChar = findDiagFromContext(ctx, shape);
            System.out.println("DEBUG - Selected diag for backward: " + diagChar);
            
            List<Position> diag = shape.getDiagPath(diagChar);
            int centerIdx = diag.indexOf(Position.CENTER);
            int idx = centerIdx - steps;
            
            // 컨텍스트 정보가 있으면 해당 지름길로 나가야 함
            if (idx >= 0) {
                out.add(diag.get(idx));
            } else {
                // 지름길 시작점을 넘어서 후진하는 경우
                int entry = outer.indexOf(diag.get(0));
                int off = -idx;
                int oidx = (entry - off % outer.size() + outer.size()) % outer.size();
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
        
        // POS_0 처리는 getNextPositions 메서드로 이동했으므로 여기서 제거
        
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
        
        // 2. 지름길 끝이 POS_0인 경우
        Position exit = seq.get(limit - 1);
        if (exit == Position.POS_0) {
            out.add(Position.POS_0);
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
    
    private static char findDiagFromContext(Position ctx, BoardShape shape) {
        // 1. 컨텍스트가 직접 지름길 위치인 경우 (DIA_X2 처럼)
        if (ctx != null && ctx.name().startsWith("DIA_")) {
            char pathChar = ctx.name().charAt(4);  // DIA_X2에서 X 추출
            // 유효한 지름길 문자인지 확인
            if (shape.getDiagNames().contains(pathChar)) {
                return pathChar;
            }
        }
        
        // 2. 컨텍스트가 다른 지름길에 속하는지 확인
        if (ctx != null) {
            for (char c : shape.getDiagNames()) {
                List<Position> diag = shape.getDiagPath(c);
                if (diag.contains(ctx)) {
                    return c;
                }
            }
        }
        
        // 3. 컨텍스트가 없거나 유효하지 않은 경우, 기본값 사용
        // 이 부분이 중요! 후진 시에는 getDefaultCenterExitPath()를 사용하지 않고
        // END까지 거리가 가장 짧은 지름길을 선택
        List<Character> sorted = new ArrayList<>(shape.getDiagNames());
        sorted.sort(Comparator.comparingInt(c -> getDistanceToEnd(c, shape)));
        return sorted.get(0);
    }
    
    private static char chooseDiag(Position ctx,
                                  List<Character> diags,
                                  boolean isBackward,
                                  BoardShape shape) {
        // 이전 경로 컨텍스트가 있으면 해당 경로 유지
        if (ctx != null) {
            // 직접적으로 지름길 위치에서 온 경우
            if (ctx.name().startsWith("DIA_")) {
                char pathChar = ctx.name().charAt(4);  // DIA_X2에서 X 추출
                // 해당 문자가 유효한 지름길 이름인지 확인
                if (diags.contains(pathChar)) {
                    return pathChar;
                }
            }
            
            // ctx 위치가 특정 지름길에 속하는지 확인
            for (char c : diags) {
                List<Position> diag = shape.getDiagPath(c);
                if (diag.contains(ctx)) {
                    return c;  // 해당 지름길 사용
                }
            }
        }
    	
        if (isBackward) {
            // 컨텍스트가 없는 경우, END까지 거리가 가장 짧은 경로 선택 (기존 로직 유지)
            List<Character> sorted = new ArrayList<>(diags);
            sorted.sort(Comparator.comparingInt(c -> getDistanceToEnd(c, shape)));
            return sorted.get(0);
        } else {
            // 앞으로 가는 경우 - 보드 형태별 기본 경로 사용
            return shape.getDefaultCenterExitPath();
        }
        
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
        if (endpoint == Position.POS_0) return 0;
        
        // Calculate distance
        List<Position> outer = shape.getOuterPath();
        int idx = outer.indexOf(endpoint);
        return outer.size() - idx;
    }
}