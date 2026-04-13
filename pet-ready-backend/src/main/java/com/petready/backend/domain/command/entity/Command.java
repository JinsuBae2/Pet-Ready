package com.petready.backend.domain.command.entity;

import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자나 서버에서 기기로 전송한 제어 명령 이력을 관리하는 엔티티입니다.
 * 명령 내용, 동작 지속 시간 및 기기의 수신(Ack) 시각을 기록합니다.
 */
@Entity
@Table(name = "commands")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Command extends BaseEntity {

    /**
     * 기기로부터 명령 수신 확인을 기록합니다.
     */
    public void acknowledge() {
        this.ackedAt = LocalDateTime.now();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 명령을 수신할 대상 기기
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    /**
     * 실행할 명령의 종류 (예: BARK_START, PLAY_SOUND 등)
     */
    @Column(nullable = false, length = 100)
    private String command;

    /**
     * 명령 동작 지속 시간 (초 단위)
     */
    @Column(name = "duration_sec")
    private Integer durationSec;

    /**
     * 기기에서 명령을 성공적으로 수신하고 응답(Acknowledge)한 시각
     */
    @Column(name = "acked_at")
    private LocalDateTime ackedAt;
}
