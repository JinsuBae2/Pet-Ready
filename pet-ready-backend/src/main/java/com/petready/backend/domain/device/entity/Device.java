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
     * 반려견의 이름(닉네임)을 업데이트합니다.
     * @param petName 새로운 반려견 이름
     */
    public void updatePetName(String petName) {
        if (petName == null || petName.trim().isEmpty()) {
            throw new IllegalArgumentException("반려견 이름은 필수입니다.");
        }
        this.petName = petName;
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

    /**
     * 방전 패널티로 인한 굶김 횟수 (N_sick)
     */
    @Builder.Default
    @Column(name = "sick_count", nullable = false)
    private Integer sickCount = 0;

    /**
     * 마지막으로 방전 패널티를 받은 시각 (중복 패널티 방지용)
     */
    @Column(name = "last_discharge_penalty_at")
    private LocalDateTime lastDischargePenaltyAt;

    /**
     * 가상 피딩 편법 차단용: 앱 피딩 터치 활성화 여부
     */
    @Builder.Default
    @Column(name = "app_feed_clicked", nullable = false)
    private Boolean appFeedClicked = false;

    /**
     * 가상 피딩 편법 차단용: 실물 밥그릇 비전 인식 여부
     */
    @Builder.Default
    @Column(name = "bowl_detected", nullable = false)
    private Boolean bowlDetected = false;

    /**
     * 가상 반려견 상태 관리를 위한 평상시 상태 유형
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "routine_status", nullable = false, length = 50)
    private RoutineStatus routineStatus = RoutineStatus.HAPPY;

    /**
     * 마지막 급여 완료 시각
     */
    @Column(name = "last_feed_time")
    private LocalDateTime lastFeedTime;

    /**
     * 굶김 횟수를 1 증가시킵니다.
     */
    public void incrementSickCount() {
        this.sickCount += 1;
        this.lastDischargePenaltyAt = LocalDateTime.now();
    }

    public void updateAppFeedClicked(boolean clicked) {
        this.appFeedClicked = clicked;
    }

    public void updateBowlDetected(boolean detected) {
        this.bowlDetected = detected;
        if (detected) {
            this.lastFeedTime = LocalDateTime.now();
        }
    }

    public void updateRoutineStatus(RoutineStatus status) {
        if (status != null) {
            this.routineStatus = status;
        }
    }

    public void updateLastFeedTime(LocalDateTime feedTime) {
        this.lastFeedTime = feedTime;
    }

    public void resetFeedingLock() {
        this.appFeedClicked = false;
        this.bowlDetected = false;
    }

    /**
     * 굶김 횟수(아픔 횟수) 및 마지막 방전 패널티 시각을 초기화합니다.
     */
    public void resetSickCount() {
        this.sickCount = 0;
        this.lastDischargePenaltyAt = null;
    }
}
