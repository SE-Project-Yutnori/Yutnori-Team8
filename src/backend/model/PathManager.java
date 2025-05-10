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
        Position fallbackCtx = piece.getLastEnteredWaypoint();
        List<Position> outer = shape.getOuterPath();
        List<Position> path = new ArrayList<>();

        System.out.println("DEBUG - Current position: " + cur + ", Context: " + ctx + ", Steps: " + steps);

        // Early exit conditions
        if (steps == 0) return path;

        if (cur == Position.OFFBOARD) {
            if (steps < (outer.size()-1)) path.add(outer.get(steps));
            else path.add(Position.END);
            return path;
        }

        // POS_0 처리
        if (cur == Position.POS_0) {
            if (steps > 0) {
                // 앞으로 가는 경우 - 종료 위치로
                path.add(Position.END);
                return path;
            } else {
                // 후진하는 경우 - outer path로 이동
                if (piece.getPathContextWaypoint() == null) {
                    int targetIdx = ((outer.size()-1) - (-steps) % (outer.size()-1)) % (outer.size()-1);
                    path.add(outer.get(targetIdx));
                    return path;
                } else {
                    Position tempContextWayPoint = piece.getPathContextWaypoint();
                    return backward(cur, -steps, ctx, fallbackCtx, outer, shape);
                }
            }
        }

        if (steps < 0) return backward(cur, -steps, ctx, fallbackCtx, outer, shape);
        else return forward(cur, steps, ctx, outer, shape);
    }

    private static List<Position> backward(Position cur, int steps,
                                          Position ctx,Position fallbackCtx, List<Position> outer,
                                          BoardShape shape) {
        List<Position> out = new ArrayList<>();
        System.out.println("DEBUG - Current position: " + cur + ", Context: " + ctx + ", Steps: " + steps);
        // 1) CENTER에서 후진
        if (cur == Position.CENTER) {
            // 컨텍스트에 기반한 지름길 찾기 - 어느 지름길로 들어왔는지 정보를 사용
            char diagChar = findDiagFromContext(cur,ctx,fallbackCtx, shape);
            System.out.println("DEBUG - Selected diag for backward: " + diagChar);
            
            List<Position> diag = shape.getDiagPath(diagChar);
            int centerIdx = diag.indexOf(Position.CENTER);
            
            // centerIdx는 항상 찾아져야 함 (지름길은 항상 CENTER를 포함하기 때문)
            if (centerIdx != -1) {
                int idx = centerIdx - steps;
                
                if (idx >= 0) {
                    // 지름길 내에서 후진 가능
                    out.add(diag.get(idx));
                } else {
                    // 지름길 시작점을 넘어서 후진하는 경우
                    int entry = outer.indexOf(diag.get(0));
                    int off = -idx;
                    int oidx = (entry - off % (outer.size()-1) + (outer.size()-1)) % (outer.size()-1);
                    out.add(outer.get(oidx));
                }
            } else {
                // 이런 경우는 없어야 하지만, 안전을 위한 예외 처리
                char defaultDiag = chooseDiag(ctx, shape.getDiagNames(), true, shape);
                List<Position> defaultPath = shape.getDiagPath(defaultDiag);
                centerIdx = defaultPath.indexOf(Position.CENTER);
                int idx = centerIdx - steps;
                
                if (idx >= 0) {
                    out.add(defaultPath.get(idx));
                } else {
                    int entry = outer.indexOf(defaultPath.get(0));
                    int off = -idx;
                    int oidx = (entry - off % (outer.size()-1) + (outer.size()-1)) % (outer.size()-1);
                    out.add(outer.get(oidx));
                }
            }
            return out;
        }

        //원점에서 빽도 처리 할 경우 (도달 안해야 정상)
        if (cur == Position.POS_0){
            if(ctx != null){

            }else{
                int oIdx = outer.indexOf(cur);
                if (oIdx != -1) {
                    int tgt = (oIdx - steps % (outer.size()-1) + (outer.size()-1)) % (outer.size()-1);
                    out.add(outer.get(tgt));
                } else {
                    out.add(cur);
                }
                return out;
            }
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
                    int oidx  = (entry - off % (outer.size()-1) + (outer.size()-1)) % (outer.size()-1);
                    out.add(outer.get(oidx));
                }
                return out;
            }
        }
        // 3) outer 후진
        int oidx = outer.indexOf(cur);
        if (oidx != -1) {
            int tgt = (oidx - steps % (outer.size()-1) + (outer.size()-1)) % (outer.size()-1);
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
            if (diag.get(0) == cur) {
                int centerIdx = diag.indexOf(Position.CENTER);
                // PENTAGON 보드에서 CENTER를 넘어설 때는 A 지름길로 스위치
                if (shape == BoardShape.PENTAGON && steps > centerIdx) {
                    List<Position> diagA = shape.getDiagPath('A');
                    // CENTER를 기준으로 넘어간 칸 수 계산
                    int over    = steps - centerIdx;
                    // A 지름길에서 CENTER 위치 인덱스 + over
                    int idxA    = diagA.indexOf(Position.CENTER) + over;
                    Position skipResult = advanceWithPathCheck(diagA, idxA, outer, shape).get(0);
                    return List.of(skipResult);
                }
                // 그 외 일반 지름길 이동
                Position result = advanceWithPathCheck(diag, steps, outer, shape).get(0);
                return List.of(result);
            }
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
                
                Position result = advanceWithPathCheck(diag, idx + steps, outer, shape).get(0);
                // 결과가 CENTER면 컨텍스트를 설정
                if (result == Position.CENTER) {
                    System.out.println("DEBUG - CENTER reached from diagonal position " + cur + " - Set context to diagonal " + c);
                    // 컨텍스트 설정은 호출자가 처리해야 함
                }
                return List.of(result);
            }
        }
        
        // 4) outer 앞으로 (일반 경로)
        int oidx = outer.indexOf(cur);
        if (oidx != -1) {
            int dest = oidx + steps;
            
            // 지름길 입구에 정확히 도착하는 경우
            for (char c : shape.getDiagNames()) {
                List<Position> diag = shape.getDiagPath(c);
                int eIdx = outer.indexOf(diag.get(0));
                if (dest == eIdx) {
                    out.add(diag.get(0));
                    return out;
                }
            }
            
            // 지름길 입구를 지나쳐 가는 경우 - 일반 경로로 진행
            if (dest < (outer.size())) {
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
            if (idx == limit - 1) {
                out.add(Position.POS_0);
            } else {
                // 초과(over)인 경우 바로 END
                out.add(Position.END);
            }
            return out;
        }
        
        // 3. 지름길을 벗어나는 경우 - 출구에서 outer 경로 따라 이동
        int over = idx - (limit - 1);
        int eIdx = outer.indexOf(exit);
        int oidx = eIdx + over;
        
        // 3.1 지름길을 벗어나 지름길 입구에 정확히 도달하는 경우
        for (char c : shape.getDiagNames()) {
            int entry = outer.indexOf(shape.getDiagPath(c).get(0));
            if (oidx % (outer.size()-1) == entry) {
                out.add(shape.getDiagPath(c).get(0));
                return out;
            }
        }
        
        // 3.2 지름길을 벗어나 END에 도달하거나 그 이상 가는 경우
        if (oidx >= (outer.size()-1)) {
            out.add(Position.END);
        } else {
            // 3.3 일반 outer 경로 상의 위치
            out.add(outer.get(oidx));
        }
        return out;
    }
    
    private static char findDiagFromContext(Position cur, Position ctx, Position fallbackCtx, BoardShape shape) {
        System.out.println("DEBUG - Finding diagonal from context: " + ctx);
        
        // 1. Try current context first
        if (ctx != null) {
            // Check if the context is a diagonal position (DIA_X2)
            if (ctx.name().startsWith("DIA_")) {
                char pathChar = ctx.name().charAt(4);  // DIA_X2에서 X 추출
                if (shape.getDiagNames().contains(pathChar)) {
                    System.out.println("DEBUG - Found diagonal character from context name: " + pathChar);
                    return pathChar;
                }
            }
            
            // Check if context belongs to a diagonal path
            for (char c : shape.getDiagNames()) {
                List<Position> diag = shape.getDiagPath(c);
                if (diag.contains(ctx)) {
                    System.out.println("DEBUG - Found diagonal character from context position: " + c);
                    return c;
                }
            }
            
            // Check if context is a diagonal entrance
            for (char c : shape.getDiagNames()) {
                if (ctx == shape.getDiagPath(c).get(0)) {
                    System.out.println("DEBUG - Context is diagonal entrance for: " + c);
                    return c;
                }
            }
            
            // If context is an outer path position, infer diagonal
            if (ctx.name().startsWith("POS_")) {
                // Special case for specific positions that are entries to diagonals
                switch (ctx.name()) {
                    case "POS_5":
                        System.out.println("DEBUG - Inferred diagonal A from POS_5");
                        return 'A';
                    case "POS_10":
                        System.out.println("DEBUG - Inferred diagonal B from POS_10");
                        return 'B';
                    case "POS_15":
                        System.out.println("DEBUG - Inferred diagonal C from POS_15");
                        return 'C';
                    // Add other cases as needed
                }
            }
        }
        
        // 2. Try fallback context if available
        if (fallbackCtx != null) {
            // Repeat similar checks with fallback context
            if (fallbackCtx.name().startsWith("DIA_")) {
                char pathChar = fallbackCtx.name().charAt(4);
                if (shape.getDiagNames().contains(pathChar)) {
                    System.out.println("DEBUG - Found diagonal from fallback context name: " + pathChar);
                    return pathChar;
                }
            }
            
            for (char c : shape.getDiagNames()) {
                List<Position> diag = shape.getDiagPath(c);
                if (diag.contains(fallbackCtx)) {
                    System.out.println("DEBUG - Found diagonal from fallback context: " + c);
                    return c;
                }
            }
            
            // Infer from fallback positions
            if (fallbackCtx.name().startsWith("POS_")) {
                switch (fallbackCtx.name()) {
                    case "POS_5":
                        return 'A';
                    case "POS_10":
                        return 'B';
                    case "POS_15":
                        return 'C';
                    // Add other cases as needed
                }
            }
        }
        
        // 3. Default: Use the board's default center exit path
        char defaultDiag = shape.getDefaultCenterExitPath();
        System.out.println("DEBUG - Using default diagonal: " + defaultDiag);
        return defaultDiag;
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
            
            // ctx가 지름길 입구인지 확인
            for (char c : diags) {
                if (ctx == shape.getDiagPath(c).get(0)) {
                    return c;
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
        return (outer.size()-1) - idx;
    }
}