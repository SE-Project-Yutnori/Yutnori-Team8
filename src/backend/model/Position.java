package backend.model;

/** 전통 윷놀이 위치 정의 */
public enum Position {
    OFFBOARD,    // 손에 든 말
    // 외곽 20칸 (시계 반대 방향)
    POS_0, POS_1, POS_2, POS_3, POS_4,
    POS_5, POS_6, POS_7, POS_8, POS_9,
    POS_10, POS_11, POS_12, POS_13, POS_14,
    POS_15, POS_16, POS_17, POS_18, POS_19,
    // 지름길 A: 오른위 → 중앙 → 왼아
    DIA_A1, DIA_A2, CENTER, DIA_A3, DIA_A4,
    // 지름길 B: 왼위 → 중앙 → 오아
    DIA_B1, DIA_B2, DIA_B3, DIA_B4,
    END          // 완전 골인
}
