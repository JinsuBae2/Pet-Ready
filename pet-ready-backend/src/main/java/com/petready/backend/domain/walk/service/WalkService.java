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
    private final com.petready.backend.domain.score.service.ScoreService scoreService;

    /**
     * 산책 기록을 저장하고 즉시 리포트를 업데이트합니다.
     */
    @Transactional
    public void endWalk(WalkEndRequest request, String email) {
        // 1. 기기 정보 및 사용자 정보 조회
        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new EntityNotFoundException("등록되지 않은 기기입니다: " + request.getDeviceId()));
        
        User user = device.getUser();

        // 2. 로그인된 사용자 정보와 기기 소유자가 일치하는지 이메일 검증
        if (!user.getEmail().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("기기 소유자 정보와 로그인된 사용자 정보가 일치하지 않습니다.");
        }

        // 3. Division by Zero 방어 로직 및 목표 거리 치팅 방지 (클라이언트가 아닌 DB 설정값 기준)
        BigDecimal walkGoal = BigDecimal.valueOf(device.getWalkGoalKm());
        if (walkGoal.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("사용자 [{}]의 산책 목표가 0입니다. 패널티 산출 시 주의가 필요합니다.", user.getEmail());
        }

        // 4. 경로 데이터 직렬화 (List 객체를 JSON 문자열로 변환)
        String routeJsonString = "[]";
        try {
            if (request.getRoute() != null) {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                routeJsonString = objectMapper.writeValueAsString(request.getRoute());
            }
        } catch (Exception e) {
            log.error("산책 경로 데이터 직렬화 중 에러 발생", e);
        }

        // 5. 산책 엔티티 생성 및 저장
        Walk walk = Walk.builder()
                .user(user)
                .device(device)
                .distanceKm(request.getDistanceKm())
                .walkGoalKm(walkGoal)
                .durationSec(request.getDurationSec())
                .startedAt(request.getStartedAt())
                .endedAt(request.getEndedAt())
                .routeJson(routeJsonString)
                .build();
        
        walkRepository.save(walk);
        log.info("사용자 [{}]의 산책 기록 저장 완료 ({}km, 시작: {})", user.getEmail(), request.getDistanceKm(), request.getStartedAt());

        // 6. BK-07 실시간 달성률 계산 및 점수 정산 (ScoreService 연동)
        if (device.getWalkGoalKm() != null && device.getWalkGoalKm() > 0) {
            double ratio = request.getDistanceKm().doubleValue() / device.getWalkGoalKm();
            
            if (ratio >= 1.0) {
                scoreService.processScoreEvent(device.getDeviceId(), "WALK_FULL", 5, "산책 목표 100% 달성");
            } else if (ratio >= 0.7) {
                scoreService.processScoreEvent(device.getDeviceId(), "WALK_PARTIAL_HIGH", 2, "산책 목표 70% 이상 달성");
            } else if (ratio >= 0.3) {
                scoreService.processScoreEvent(device.getDeviceId(), "WALK_PARTIAL_LOW", -3, "산책 목표 미흡 (30~70%)");
            }
        }

        // 7. 성적표(PetReport) 즉시 업데이트 연동
        scoringService.updateReport(user, walk);
    }
}
