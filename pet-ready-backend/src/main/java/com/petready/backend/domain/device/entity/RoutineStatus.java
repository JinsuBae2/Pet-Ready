package com.petready.backend.domain.device.entity;

/**
 * 가상 반려견의 평상시 라이프사이클 상태를 정의하는 Enum입니다.
 */
public enum RoutineStatus {
    SLEEPING, // 취침
    HUNGRY,   // 배고픔
    BARKING,  // 짖음
    SICK,     // 아픔/방전
    HAPPY,    // 행복
    BORED     // 심심함
}
