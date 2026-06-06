package com.petready.backend.domain.dashboard.api;

import com.petready.backend.domain.dashboard.dto.DashboardResponse;
import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.score.entity.RealTimeScore;
import com.petready.backend.domain.score.entity.ScoreEvent;
import com.petready.backend.domain.score.repository.RealTimeScoreRepository;
import com.petready.backend.domain.score.repository.ScoreEventRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
 * 모바일 앱 메인 대시보드 화면에 노출될 실시간 점수 및 상태 변동 통계를 조회하는 컨트롤러 클래스입니다.
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
     * 메인 화면 진입 시 호출하여 펫의 실시간 양육 점수와 직전 점수 가감 정보를 조회합니다.
     */
    @Operation(
        summary = "메인 대시보드 상태 조회 API", 
        description = "기기 식별자(deviceId)를 받아 현재 시뮬레이션 양육 점수(currentScore, 0~100점), 직전 점수 변동치(lastScoreDelta, 예: +5, -10), 그리고 점수를 가감시킨 직전 원인 이벤트명(lastScoreEvent, 예: BARK_RESPOND, WALK_FULL)을 취합하여 한 번에 반환합니다. 앱은 이 정보를 30초 주기로 폴링하여 화면에 노출합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "대시보드 응답 DTO 정상 매핑 반환 성공"),
        @ApiResponse(responseCode = "404", description = "요청된 기기 ID가 시스템에 존재하지 않는 경우")
    })
    @GetMapping("/{deviceId}")
    public ResponseEntity<DashboardResponse> getDashboard(@PathVariable("deviceId") String deviceId) {
        
        // 1. 등록된 기기가 존재하는지 DB에서 검증합니다.
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("등록되지 않은 기기입니다."));

        // 2. 실시간 현재 점수를 조회합니다. 기록이 없다면 디폴트 점수인 100점을 사용합니다.
        Optional<RealTimeScore> scoreOpt = realTimeScoreRepository.findById(deviceId);
        Integer currentScore = scoreOpt.map(RealTimeScore::getCurrentScore).orElse(100);

        // 3. 점수 가감 이력(ScoreEvent) 테이블에서 이 기기의 가장 최신 이벤트 1건을 조회합니다.
        Optional<ScoreEvent> lastEventOpt = scoreEventRepository.findFirstByDeviceDeviceIdOrderByOccurredAtDesc(deviceId);
        
        // 이력이 존재한다면 가감 변동 점수와 원인 코드명을 추출하고, 없다면 0점 및 NONE으로 대체합니다.
        Integer lastScoreDelta = lastEventOpt.map(ScoreEvent::getDelta).orElse(0);
        String lastScoreEvent = lastEventOpt.map(ScoreEvent::getEventType).orElse("NONE");

        // 4. 대시보드 응답 DTO를 매핑하여 클라이언트에 전달합니다.
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
