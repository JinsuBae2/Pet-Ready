package com.petready.backend.domain.device.entity;

import com.petready.backend.domain.user.entity.User;
import com.petready.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 반려견에 장착된 IoT 기기 정보를 관리하는 엔티티입니다.
 * 기기 식별자, 연결된 사용자, 반려견 이름 및 산책 목표 등을 관리합니다.
 */
@Entity
@Table(name = "devices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Device extends BaseEntity {

    /**
     * 기기의 하트비트 시각을 업데이트하고 온라인 상태로 전환합니다.
     */
    public void updateHeartbeat() {
        this.lastHeartbeat = LocalDateTime.now();
        this.isOnline = true;
    }

    /**
     * 기기의 온라인/오프라인 상태를 강제로 설정합니다.
     * @param online 온라인 여부
     */
    public void setOnlineStatus(boolean online) {
        this.isOnline = online;
    }

    /**
     * 기기의 고유 식별자 (예: DOG_01)
     */
    @Id
    @Column(name = "device_id", length = 50)
    private String deviceId;

    /**
     * 기기를 소유하고 있는 사용자 (연관 관계)
     * 지연 로딩을 활용하여 성능을 최적화하고 순환 참조를 방지합니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 기기가 장착된 반려견의 이름
     */
    @Column(name = "pet_name", nullable = false, length = 50)
    private String petName;

    /**
     * 하루 산책 목표 거리 (km 단위, 기본값 2.0, 최소 0.1)
     */
    @Builder.Default
    @DecimalMin(value = "0.1", message = "산책 목표는 최소 0.1km 이상이어야 합니다.")
    @Column(name = "walk_goal_km", nullable = false)
    private Double walkGoalKm = 2.0;

    /**
     * 기기의 현재 온라인 여부
     */
    @Column(name = "is_online", nullable = false)
    private Boolean isOnline;

    /**
     * 마지막으로 기기에서 신호(Heartbeat)를 보낸 시각
     */
    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;
}
