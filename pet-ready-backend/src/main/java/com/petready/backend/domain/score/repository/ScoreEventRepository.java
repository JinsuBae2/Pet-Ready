package com.petready.backend.domain.score.repository;

import com.petready.backend.domain.score.entity.ScoreEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 점수 변동 이벤트 로그(ScoreEvent) 엔티티에 대한 데이터베이스 접근을 담당합니다.
 */
@Repository
public interface ScoreEventRepository extends JpaRepository<ScoreEvent, Long> {
    
    /**
     * 특정 기기의 가장 최근에 발생한 점수 이벤트 로그를 조회합니다.
     * 
     * @param deviceId 기기 고유 ID
     * @return 가장 최근의 ScoreEvent
     */
    Optional<ScoreEvent> findFirstByDeviceDeviceIdOrderByOccurredAtDesc(String deviceId);

    /**
     * 특정 기기에서 특정 이벤트 타입이 특정 시간 이후로 발생한 횟수를 카운트합니다.
     * 
     * @param deviceId 기기 고유 ID
     * @param eventType 이벤트 타입 코드
     * @param occurredAt 시작 기준 시간
     * @return 발생 횟수
     */
    long countByDeviceDeviceIdAndEventTypeAndOccurredAtAfter(String deviceId, String eventType, java.time.LocalDateTime occurredAt);

    /**
     * 특정 기기의 전체 점수 변동 이벤트 로그를 조회합니다.
     */
    java.util.List<ScoreEvent> findAllByDeviceDeviceId(String deviceId);
}
