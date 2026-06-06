package com.petready.backend.domain.mission.entity;

/**
 * 미션의 진행 상태를 정의하는 Enum 클래스입니다.
 */
public enum MissionStatus {
    /**
     * 진행 전 (발급 직후 기본 상태)
     */
    PENDING,

    /**
     * 진행 중 (사용자가 미션을 시작한 상태)
     */
    IN_PROGRESS,

    /**
     * 미션 성공 (사용자가 미션을 성공적으로 완료한 상태)
     */
    COMPLETED,

    /**
     * 미션 실패 (기한 내에 완료하지 못하는 등 실패한 상태)
     */
    FAILED
}
