package backend.model;

import java.util.*;
import java.util.stream.*;

/**
* 보드 형태별 외곽 경로와 지름길 경로를 관리하는 열거형
*/
public enum BoardShape {
    TRADITIONAL(20, List.of('A','B')),
    PENTAGON(25, List.of('A','B','C')),
    HEXAGON(30, List.of('A','B','C'));

    private final int outerCount;
    private final List<Character> diagNames;
    private final Map<Character, Integer> distanceToEndCache = new HashMap<>();

    BoardShape(int outerCount, List<Character> diagNames) {
        this.outerCount = outerCount;
        this.diagNames = diagNames;
    }

    /** 외곽 경로: POS_0 ~ POS_{outerCount-1} */
    public List<Position> getOuterPath() {
        return IntStream.range(0, outerCount)
                .mapToObj(i -> Position.valueOf("POS_" + i))
                .collect(Collectors.toList());
    }

    /**
    * 대각선 경로: [입구, DIA_x1, DIA_x2, CENTER, DIA_x3, DIA_x4, 출구]
    */
    public List<Position> getDiagPath(char c) {
        // 사각형 보드 전용: POS_5->CENTER->POS_15 (A), POS_10->CENTER->END (B)
        if (this == TRADITIONAL) {
            if (c == 'A') {
                return List.of(
                        Position.POS_5,
                        Position.DIA_A1, Position.DIA_A2,
                        Position.CENTER,
                        Position.DIA_A3, Position.DIA_A4,
                        Position.POS_15
                );
            } else if (c=='B') { // 'B'
                return List.of(
                        Position.POS_10,
                        Position.DIA_B1, Position.DIA_B2,
                        Position.CENTER,
                        Position.DIA_B3, Position.DIA_B4,
                        Position.POS_0
                );
            }
        } else if (this == PENTAGON) {
            // 오각형 보드 전용 경로
            switch (c) {
                case 'A':
                    return List.of(
                            Position.POS_5,
                            Position.valueOf("DIA_A1"), Position.valueOf("DIA_A2"),
                            Position.CENTER,
                            Position.valueOf("DIA_A3"), Position.valueOf("DIA_A4"),
                            Position.valueOf("POS_20")
                    );
                case 'B':
                    return List.of(
                            Position.POS_10,
                            Position.valueOf("DIA_B1"), Position.valueOf("DIA_B2"),
                            Position.CENTER,
                            Position.valueOf("DIA_B3"), Position.valueOf("DIA_B4"),
                            Position.END
                    );
                case 'C':
                    return List.of(
                            Position.valueOf("POS_15"),
                            Position.valueOf("DIA_C1"), Position.valueOf("DIA_C2"),
                            Position.CENTER,
                            Position.valueOf("DIA_C3"), Position.valueOf("DIA_C4"),
                            Position.valueOf("POS_0")
                    );
                default:
                    // 나머지 대각선(D, E)은 기본 계산 방식 사용
                    int step = outerCount / diagNames.size();
                    int entryIdx = (c - 'C') * step + 15; // C 이후부터 계산
                    int exitIdx = (entryIdx + outerCount / 2) % outerCount;
                    Position exit = Position.valueOf("POS_" + exitIdx);

                    return List.of(
                            Position.valueOf("POS_" + entryIdx),
                            Position.valueOf("DIA_" + c + "1"),
                            Position.valueOf("DIA_" + c + "2"),
                            Position.CENTER,
                            Position.valueOf("DIA_" + c + "3"),
                            Position.valueOf("DIA_" + c + "4"),
                            exit
                    );
            }
        } else if (this == HEXAGON) {
            // 육각형 보드 전용 경로
            switch (c) {
                case 'A':
                    return List.of(
                            Position.POS_5,
                            Position.valueOf("DIA_A1"), Position.valueOf("DIA_A2"),
                            Position.CENTER,
                            Position.valueOf("DIA_A3"), Position.valueOf("DIA_A4"),
                            Position.valueOf("POS_20")
                    );
                case 'B':
                    return List.of(
                            Position.POS_10,
                            Position.valueOf("DIA_B1"), Position.valueOf("DIA_B2"),
                            Position.CENTER,
                            Position.valueOf("DIA_B3"), Position.valueOf("DIA_B4"),
                            Position.valueOf("POS_25")
                    );
                case 'C':
                    return List.of(
                            Position.valueOf("POS_15"),
                            Position.valueOf("DIA_C1"), Position.valueOf("DIA_C2"),
                            Position.CENTER,
                            Position.valueOf("DIA_C3"), Position.valueOf("DIA_C4"),
                            Position.POS_0
                    );
                default:
                    // 나머지 대각선(D, E, F)은 기본 계산 방식 사용
                    int step = outerCount / diagNames.size();
                    int entryIdx = (c - 'C') * step + 15; // C 이후부터 계산
                    int exitIdx = (entryIdx + outerCount / 2) % outerCount;
                    Position exit = Position.valueOf("POS_" + exitIdx);

                    return List.of(
                            Position.valueOf("POS_" + entryIdx),
                            Position.valueOf("DIA_" + c + "1"),
                            Position.valueOf("DIA_" + c + "2"),
                            Position.CENTER,
                            Position.valueOf("DIA_" + c + "3"),
                            Position.valueOf("DIA_" + c + "4"),
                            exit
                    );
            }
        }

        // 기본 처리 (일반적인 경우)
        int segments = diagNames.size();
        int step = outerCount / segments;
        int entryIdx = (c - 'A') * step;
        int exitIdx = ((c - 'A' + segments/2) % segments) * step;
        Position exit = Position.valueOf("POS_" + exitIdx);

        return List.of(
                Position.valueOf("POS_" + entryIdx),
                Position.valueOf("DIA_" + c + "1"),
                Position.valueOf("DIA_" + c + "2"),
                Position.CENTER,
                Position.valueOf("DIA_" + c + "3"),
                Position.valueOf("DIA_" + c + "4"),
                exit
        );
    }

    /**
     * CENTER에서 나가는 기본 경로 반환 (보드 형태별로 다름)
     */
    public char getDefaultCenterExitPath() {
        if (this == TRADITIONAL) {
            return 'B'; // 전통 보드에서는 B 경로로 나감
        } else if (this == PENTAGON) {
            return 'B'; // 오각형 보드에서는 B 경로로 나감
        } else if (this == HEXAGON) {
            return 'C'; // 육각형 보드에서는 C 경로로 나감
        }
        return diagNames.get(0); // 기본값
    }

    public List<Character> getDiagNames() {
        return Collections.unmodifiableList(diagNames);
    }

    /**
    * 해당 대각선의 출구(outer)에서 END까지 남은 칸 수 계산
    */
    public int distanceToEnd(char c) {
        // Check cache first
        if (distanceToEndCache.containsKey(c)) {
            return distanceToEndCache.get(c);
        }

        List<Position> diag = getDiagPath(c);
        Position last = diag.get(diag.size() - 1);
        if (last == Position.END) {
            distanceToEndCache.put(c, 0);
            return 0;
        }
        int idx = getOuterPath().indexOf(last);
        int distance = outerCount - idx;

        // Cache the result
        distanceToEndCache.put(c, distance);
        return distance;
    }
}