package backend.model;

/** 전통 윷놀이 위치 정의 */
public enum Position {
    OFFBOARD,    // 손에 든 말
    // 외곽 20칸 (시계 반대 방향)
    POS_0, POS_1, POS_2, POS_3, POS_4,
    POS_5, POS_6, POS_7, POS_8, POS_9,
    POS_10, POS_11, POS_12, POS_13, POS_14,
    POS_15, POS_16, POS_17, POS_18, POS_19,
    POS_20, POS_21, POS_22, POS_23, POS_24,
    POS_25, POS_26, POS_27, POS_28, POS_29,
    POS_30,
    // 지름길 A: 오른위 → 중앙 → 왼아
    DIA_A1, DIA_A2, CENTER, DIA_A3, DIA_A4,
    // 지름길 B: 왼위 → 중앙 → 오아
    DIA_B1, DIA_B2, DIA_B3, DIA_B4,
    
    DIA_C1, DIA_C2, DIA_C3, DIA_C4,
    DIA_D1, DIA_D2, DIA_D3, DIA_D4,
    DIA_E1, DIA_E2, DIA_E3, DIA_E4,
    DIA_F1, DIA_F2, DIA_F3, DIA_F4,
    
    END          // 완전 골인
}
