package com.petready.backend.global.enums;

/**
 * 알림의 종류를 정의하는 Enum 클래스입니다.
 */
public enum NotificationType {
    /**
     * 새벽 짖음 등 미션 알림
     */
    BARKING,
    
    /**
     * 배터리 20% 이하 알림
     */
    FEEDING,
    
    /**
     * 향후 의료비 알림
     */
    MEDICAL
}
