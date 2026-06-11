package com.petready.backend.domain.dashboard.api;

import com.petready.backend.domain.dashboard.dto.DashboardResponse;
import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.score.entity.RealTimeScore;
import com.petready.backend.domain.score.entity.ScoreEvent;
import com.petready.backend.domain.score.repository.RealTimeScoreRepository;
import com.petready.backend.domain.score.repository.ScoreEventRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * 모바일 앱의 메인 화면(대시보드)에 필요한 데이터를 제공하는 컨트롤러입니다.
 */
@Tag(name = "Dashboard", description = "메인 대시보드 API")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DeviceRepository deviceRepository;
    private final RealTimeScoreRepository realTimeScoreRepository;
    private final ScoreEventRepository scoreEventRepository;

    /**
     * 해당 기기의 최신 상태(점수 및 가장 최근 이벤트)를 조회하여 반환합니다.
     * 
     * @param deviceId 기기 고유 ID
     * @return 대시보드 상태 데이터
     */
    @Operation(summary = "대시보드 정보 조회", description = "기기의 현재 실시간 점수와 가장 최근 발생한 가감점 이벤트를 반환합니다.")
    @GetMapping("/{deviceId}")
    public ResponseEntity<DashboardResponse> getDashboard(@PathVariable("deviceId") String deviceId) {
        
        // 1. 기기 기본 정보 조회
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("등록되지 않은 기기입니다."));

        // 2. 현재 실시간 점수 조회 (존재하지 않으면 기본 100점 반환을 위한 논리)
        Optional<RealTimeScore> scoreOpt = realTimeScoreRepository.findById(deviceId);
        Integer currentScore = scoreOpt.map(RealTimeScore::getCurrentScore).orElse(100);

        // 3. 가장 최근 발생한 점수 이벤트 조회
        Optional<ScoreEvent> lastEventOpt = scoreEventRepository.findFirstByDeviceDeviceIdOrderByOccurredAtDesc(deviceId);
        
        Integer lastScoreDelta = lastEventOpt.map(ScoreEvent::getDelta).orElse(0);
        String lastScoreEvent = lastEventOpt.map(ScoreEvent::getEventType).orElse("NONE");

        // 4. 응답 DTO 매핑
        DashboardResponse response = DashboardResponse.builder()
                .deviceId(device.getDeviceId())
                .petName(device.getPetName())
                .currentScore(currentScore)
                .lastScoreDelta(lastScoreDelta)
                .lastScoreEvent(lastScoreEvent)
                .build();

        return ResponseEntity.ok(response);
    }
}
