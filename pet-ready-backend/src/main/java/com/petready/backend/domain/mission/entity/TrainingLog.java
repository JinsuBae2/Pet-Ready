package com.petready.backend.domain.mission.entity;

import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 강아지 가상 훈련의 판정 결과를 기록하고 보존하는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "training_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TrainingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "gesture_type", length = 50)
    private String gestureType;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // SUCCESS, CONFUSED, SAD

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
