package com.petready.backend.domain.score.service;

import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.score.entity.RealTimeScore;
import com.petready.backend.domain.score.entity.ScoreEvent;
import com.petready.backend.domain.score.repository.RealTimeScoreRepository;
import com.petready.backend.domain.score.repository.ScoreEventRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 기기의 실시간 점수 증감 및 이벤트 로깅을 처리하는 핵심 비즈니스 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreService {

    private final RealTimeScoreRepository realTimeScoreRepository;
    private final ScoreEventRepository scoreEventRepository;
    private final DeviceRepository deviceRepository;

    /**
     * 가감점 이벤트를 처리하고 실시간 점수 갱신 및 로그를 기록합니다.
     * 
     * @param deviceId 기기 고유 ID
     * @param eventType 이벤트 타입 코드 (예: MISSION_FAST_COMPLETE)
     * @param delta 변동 점수 (+/-)
     * @param eventDescription 이벤트에 대한 상세 설명 (로깅 용도)
     */
    @Transactional
    public void processScoreEvent(String deviceId, String eventType, int delta, String eventDescription) {
        log.info("[점수 이벤트 발생] Device: {}, Type: {}, Delta: {}, Desc: {}", deviceId, eventType, delta, eventDescription);

        // 1. 기기 존재 여부 확인
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("등록되지 않은 기기입니다. ID: " + deviceId));

        // 2. RealTimeScore 테이블에서 해당 기기의 현재 점수를 조회 (없으면 100점으로 최초 생성)
        RealTimeScore score = realTimeScoreRepository.findById(deviceId)
                .orElseGet(() -> RealTimeScore.builder()
                        .device(device)
                        .currentScore(100) // 최초 시작 점수 100점
                        .lastUpdatedAt(LocalDateTime.now())
                        .build());

        // 3. 엔티티 내부의 클램핑 메소드를 호출해 점수 반영 (0~100 사이 제한)
        score.applyScoreDelta(delta);
        
        // 점수 갱신 저장 (새로 생성된 경우 포함)
        realTimeScoreRepository.save(score);

        // 4. 변동 로그를 남기기 위해 ScoreEvent 레코드를 생성하고 저장
        ScoreEvent eventLog = ScoreEvent.builder()
                .device(device)
                .eventType(eventType)
                .delta(delta)
                .scoreAfter(score.getCurrentScore())
                .occurredAt(LocalDateTime.now())
                .build();
        
        scoreEventRepository.save(eventLog);
        
        log.info("[점수 갱신 완료] Device: {}, 최종 점수: {}", deviceId, score.getCurrentScore());
    }
}
