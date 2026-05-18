package com.petready.backend.domain.walk.service;

import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.report.service.ScoringService;
import com.petready.backend.domain.user.entity.User;
import com.petready.backend.domain.walk.dto.WalkEndRequest;
import com.petready.backend.domain.walk.entity.Walk;
import com.petready.backend.domain.walk.repository.WalkRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 사용자의 산책 기록을 저장하고 사후 처리를 담당하는 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalkService {

    private final WalkRepository walkRepository;
    private final DeviceRepository deviceRepository;
    private final ScoringService scoringService;

    /**
     * 산책 기록을 저장하고 즉시 리포트를 업데이트합니다.
     */
    @Transactional
    public void endWalk(WalkEndRequest request) {
        // 1. 기기 정보 및 사용자 정보 조회
        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new EntityNotFoundException("등록되지 않은 기기입니다: " + request.getDeviceId()));
        
        User user = device.getUser();

        // 2. Division by Zero 방어 로직 (목표 거리가 0이면 달성률 0% 처리를 위해 데이터를 정제하지 않고 저장 시 참고)
        BigDecimal walkGoal = request.getWalkGoalKm();
        if (walkGoal.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("사용자 [{}]의 산책 목표가 0입니다. 패널티 산출 시 주의가 필요합니다.", user.getEmail());
        }

        // 3. 산책 엔티티 생성 및 저장
        Walk walk = Walk.builder()
                .user(user)
                .device(device)
                .distanceKm(request.getDistanceKm())
                .walkGoalKm(request.getWalkGoalKm())
                .durationSec(request.getDurationSec())
                .routeJson(request.getRouteJson())
                .build();
        
        walkRepository.save(walk);
        log.info("사용자 [{}]의 산책 기록 저장 완료 ({}km)", user.getEmail(), request.getDistanceKm());

        // 4. 성적표(PetReport) 즉시 업데이트 연동
        scoringService.updateReport(user, walk);
    }
}
