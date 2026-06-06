package com.petready.backend.domain.mission.service;

import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.mission.dto.MissionResponse;
import com.petready.backend.domain.mission.entity.Mission;
import com.petready.backend.domain.mission.repository.MissionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 사용자 미션 관련 비즈니스 로직을 처리하는 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissionService {

    private final MissionRepository missionRepository;
    private final DeviceRepository deviceRepository;

    /**
     * 특정 사용자가 등록한 기기의 오늘(자정 이후) 발급된 미션 목록을 조회합니다.
     * 
     * @param email 사용자 이메일
     * @return 오늘의 미션 응답 DTO 리스트
     */
    public List<MissionResponse> getTodayMissions(String email) {
        // 로그인된 유저 이메일로 매핑된 기기를 조회하여 존재 여부를 확인합니다.
        Device device = deviceRepository.findByUserEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("등록된 기기가 존재하지 않습니다."));

        // 오늘 자정 시각(00:00:00)을 구하여 기준 시간으로 설정합니다.
        LocalDateTime todayStart = LocalDateTime.now().with(java.time.LocalTime.MIN);

        // 오늘 자정 이후 기기에 발급된 미션 목록을 조회합니다.
        List<Mission> missions = missionRepository.findAllByDeviceDeviceIdAndIssuedAtAfter(device.getDeviceId(), todayStart);

        // 엔티티 목록을 DTO 목록으로 변환하여 반환합니다.
        return missions.stream()
                .map(MissionResponse::from)
                .collect(java.util.stream.Collectors.toList());
    }

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
