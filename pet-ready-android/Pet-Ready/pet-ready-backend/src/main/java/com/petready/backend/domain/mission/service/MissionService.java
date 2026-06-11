package com.petready.backend.domain.mission.service;

import com.petready.backend.domain.mission.entity.Mission;
import com.petready.backend.domain.mission.repository.MissionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 사용자 미션 관련 비즈니스 로직을 처리하는 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissionService {

    private final MissionRepository missionRepository;

    /**
     * 사용자가 미션을 완료(응답) 처리합니다.
     * 
     * @param missionId 완료할 미션의 식별자
     */
    @Transactional
    public void completeMission(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 미션입니다."));

        if (Boolean.TRUE.equals(mission.getIsCompleted())) {
            throw new IllegalStateException("이미 완료된 미션입니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        long responseTimeSec = ChronoUnit.SECONDS.between(mission.getIssuedAt(), now);

        // 실무에서는 별도 메서드(예: mission.complete(now, responseTimeSec))를 추가하여 엔티티 내부에서 상태를 변경하는 것을 권장합니다.
        // 현재는 JPA 변경 감지 또는 리포지토리 레벨 업데이트를 가정.
        // 엔티티에 Setter가 없으므로 Mission 엔티티 수정이 필요합니다.
        
        mission.complete(now, responseTimeSec);
        log.info("미션 [{}] 완료 처리 완료. 응답 시간: {}초", missionId, responseTimeSec);
    }
}
