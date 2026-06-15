package com.petready.backend.domain.report.api;

import com.petready.backend.domain.analysis.entity.UserAnalysisResult;
import com.petready.backend.domain.analysis.service.AnalysisService;
import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.mission.entity.Mission;
import com.petready.backend.domain.mission.repository.MissionRepository;
import com.petready.backend.domain.report.dto.FinalReportResponse;
import com.petready.backend.domain.report.dto.ExpenseReportResponse;
import com.petready.backend.domain.report.entity.PetReport;
import com.petready.backend.domain.report.repository.PetReportRepository;
import com.petready.backend.domain.report.service.ReportService;
import com.petready.backend.domain.rescue.entity.RescueAnimalCache;
import com.petready.backend.domain.rescue.repository.RescueAnimalCacheRepository;
import com.petready.backend.domain.score.entity.RealTimeScore;
import com.petready.backend.domain.score.repository.RealTimeScoreRepository;
import com.petready.backend.domain.walk.entity.Walk;
import com.petready.backend.domain.walk.repository.WalkRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.petready.backend.domain.user.service.ResetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 시뮬레이션 최종 종료 후 사용자의 양육 성적, AI 행동 유형 분류 및 맞춤 유기견 구조 정보 매칭을 일괄 제공하는 컨트롤러입니다.
 */
@Tag(name = "Report", description = "최종 양육 리포트 및 유기동물 매칭 추천 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportController {

    private final AnalysisService analysisService;
    private final ReportService reportService;
    private final DeviceRepository deviceRepository;
    private final WalkRepository walkRepository;
    private final MissionRepository missionRepository;
    private final RealTimeScoreRepository realTimeScoreRepository;
    private final PetReportRepository reportRepository;
    private final RescueAnimalCacheRepository rescueAnimalCacheRepository;
    private final com.petready.backend.domain.mission.repository.TrainingLogRepository trainingLogRepository;
    private final ResetService resetService;

    /**
     * 사용자의 최종 양육 리포트와 개인별 맞춤 유기견 리스트를 융합하여 가져옵니다.
     */
    @Operation(
        summary = "최종 양육 성적 리포트 및 추천견 조회 API", 
        description = "사용자의 전체 시뮬레이션 지표를 기반으로 AI 분석(Weka ML)을 실행하여 성향 유형을 재판별합니다. 그 후 실시간 최종 점수와 등급(A+ ~ F)을 정산하고, AI가 추천한 반려견 품종 키워드와 DB 유기견 캐시를 매칭시켜 최적의 구조견 최대 5건을 취합해 최종 리포트 패키지로 반환합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "최종 리포트 DTO 생성 및 매칭 유기견 리스트 융합 반환 완료"),
        @ApiResponse(responseCode = "400", description = "유저 계정에 연결된 로봇 기기가 없어 보고서 생성이 불가능한 경우"),
        @ApiResponse(responseCode = "401", description = "인증 토큰 누락 또는 유효 만료 상태")
    })
    @GetMapping("/final")
    public ResponseEntity<FinalReportResponse> getFinalReport(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        log.info("[최종 리포트 API 호출] 사용자 이메일: {}", email);

        // 1. 최신 시뮬레이션 통계를 기준으로 Weka AI 분석을 재수행하여 유형 및 추천 품종을 갱신합니다. (BK-13)
        UserAnalysisResult analysisResult = analysisService.performAnalysis(email);

        // 2. 기기 및 훈련 로그를 조회하여 5:5 비율 종합 양육 점수를 계산합니다.
        Device device = deviceRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저에게 등록된 기기가 없습니다."));

        // 2-1. 돌봄 달성도 점수 계산 (산책 + 미션 반응 비율)
        List<Walk> walks = walkRepository.findAllByUserEmail(email);
        List<Mission> missions = missionRepository.findAllByDeviceUserEmail(email);

        // 산책 점수 정산: 실제 총 거리와 설정 목표 거리의 백분율 비율로 변환 (최대 100점 클램핑)
        double totalActualWalk = walks.stream().mapToDouble(w -> w.getDistanceKm().doubleValue()).sum();
        double totalGoalWalk = walks.stream().mapToDouble(w -> w.getWalkGoalKm().doubleValue()).sum();
        int walkScore = totalGoalWalk == 0 ? 0 : Math.min(100, (int) ((totalActualWalk / totalGoalWalk) * 100));

        // 미션 반응 점수 정산: 전체 발급된 알림 대비 해결 완료한 미션의 백분율 비율로 변환
        long totalMissions = missions.size();
        long completedMissions = missions.stream().filter(Mission::getIsCompleted).count();
        int responseScore = totalMissions == 0 ? 100 : (int) (((double) completedMissions / totalMissions) * 100);

        // 건강 벌점 정산: 배터리 방전(아픔 횟수) 당 15점씩 패널티를 계산
        int healthPenalty = device.getSickCount() * 15;

        // 돌봄 점수 = Math.max(0, ((walkScore + responseScore) / 2.0) - healthPenalty)
        double careScore = Math.max(0, ((walkScore + responseScore) / 2.0) - healthPenalty);

        // 2-2. 가상 훈련 점수 계산 (100 - CONFUSED*5 - SAD*10)
        long confusedCount = trainingLogRepository.countByDeviceDeviceIdAndStatus(device.getDeviceId(), "CONFUSED");
        long sadCount = trainingLogRepository.countByDeviceDeviceIdAndStatus(device.getDeviceId(), "SAD");
        long successCount = trainingLogRepository.countByDeviceDeviceIdAndStatus(device.getDeviceId(), "SUCCESS");
        long totalTraining = trainingLogRepository.countByDeviceDeviceId(device.getDeviceId());
        
        double trainScore = Math.max(0, 100 - (confusedCount * 5) - (sadCount * 10));

        // 2-3. 최종 종합 점수 산출 (돌봄 50% + 훈련 50%), 0점 이하 클램핑 및 DB 영속화 반영
        int computedScore = (int) Math.round((careScore * 0.5) + (trainScore * 0.5));
        final int finalScore = Math.max(0, computedScore);
        String grade = determineGrade(finalScore);

        // PetReport 테이블에 total_score 및 grade 업데이트 반영
        PetReport report = reportRepository.findByUserEmail(email)
                .orElseGet(() -> {
                    com.petready.backend.domain.user.entity.User user = device.getUser();
                    PetReport newReport = PetReport.builder()
                            .user(user)
                            .totalScore(BigDecimal.valueOf(finalScore))
                            .grade(grade)
                            .totalReceiptAmount(0L)
                            .receiptDetailsJson("[]")
                            .totalWalkCount(walks.size())
                            .totalMissionCount(missions.size())
                            .build();
                    return reportRepository.save(newReport);
                });
        report.updateScoreAndGrade(BigDecimal.valueOf(finalScore), grade);
        reportRepository.save(report);

        // 3. 누적 데이터를 활용해 세부 성적표 지표들을 빌드합니다.
        // 평균 응답 시간 정산: 미션 완료 데이터의 초 단위 응답 속도 평균값 도출
        long respondedCount = missions.stream()
                .filter(m -> m.getIsCompleted() && m.getResponseTimeSec() != null)
                .count();
        int avgResponseSec = 0;
        if (respondedCount > 0) {
            long sumResponseSec = missions.stream()
                    .filter(m -> m.getIsCompleted() && m.getResponseTimeSec() != null)
                    .mapToLong(Mission::getResponseTimeSec)
                    .sum();
            avgResponseSec = (int) (sumResponseSec / respondedCount);
        }

        // 총 가상 누적 영수증 금액을 가져옵니다.
        int totalMedicalFee = report.getTotalReceiptAmount().intValue();

        // 4. 추천 품종에 잘 부합하는 실물 유기견 리스트 최대 5건을 캐시 테이블에서 매칭 및 추출합니다. (BK-10/BK-12)
        List<FinalReportResponse.RecommendedAnimalDto> recommendedAnimals = matchRescueAnimals(analysisResult);

        // 5. 생성형 AI 피드백을 Lazy Caching하여 융합합니다.
        double walkRatio = totalGoalWalk == 0 ? 0.0 : totalActualWalk / totalGoalWalk;
        double trainingSuccessRate = totalTraining == 0 ? 100.0 : ((double) successCount / totalTraining) * 100.0;
        
        String aiFeedback = reportService.getOrGenerateAiFeedback(
                email,
                analysisResult,
                finalScore,
                walkRatio,
                (int) completedMissions,
                (int) totalMissions,
                device.getSickCount(),
                totalTraining,
                trainingSuccessRate,
                confusedCount
        );

        // 6. 응답 JSON 조립 및 반환 처리를 수행합니다.
        FinalReportResponse.BreedRecommendation breedRecDto = FinalReportResponse.BreedRecommendation.builder()
                .type(analysisResult.getBreedType())
                .examples(analysisResult.getBreedExamples())
                .reason(analysisResult.getBreedReason())
                .build();

        FinalReportResponse response = FinalReportResponse.builder()
                .finalScore(finalScore)
                .grade(grade)
                .walkScore(walkScore)
                .responseScore(responseScore)
                .healthPenalty(healthPenalty)
                .totalWalkKm(totalActualWalk)
                .avgResponseSec(avgResponseSec)
                .totalMedicalFee(totalMedicalFee)
                .userType(analysisResult.getUserType())
                .userTypeLabel(analysisResult.getUserTypeLabel())
                .breedRecommendation(breedRecDto)
                .contextMessage(aiFeedback)
                .recommendedAnimals(recommendedAnimals)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 점수를 기반으로 실시간 감점 학점 등급을 판정합니다. (BK-08)
     */
    private String determineGrade(int score) {
        if (score >= 90) return "A+";
        if (score >= 80) return "A";
        if (score >= 70) return "B+";
        if (score >= 60) return "B";
        if (score >= 50) return "C";
        return "F";
    }

    /**
     * AI의 성향 추천 품종 키워드를 활용해 유기견 DB 캐시 테이블에서 매칭 조건에 근접하고 최근 구조된 동물 최대 5마리를 조회합니다. (BK-10)
     */
    private List<FinalReportResponse.RecommendedAnimalDto> matchRescueAnimals(UserAnalysisResult analysisResult) {
        String examples = analysisResult.getBreedExamples();
        List<RescueAnimalCache> allCached = rescueAnimalCacheRepository.findAll();

        // 추천견 예시 목록이 없는 유형(예: NOT_READY)일 경우 빈 리스트를 조기 리턴합니다.
        if (examples == null || "없음".equals(examples) || examples.trim().isEmpty()) {
            return List.of();
        }

        // 콤마(,) 단위로 추천 품종 리스트를 분리하고 다듬어 키워드 컬렉션을 수집합니다.
        List<String> keywords = Arrays.stream(examples.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        // 캐시 테이블 내역에서 키워드 포함 조건 필터링 및 구조 날짜 최신 순(내림차순) 정렬 후 5마리로 제한합니다.
        List<RescueAnimalCache> matched = allCached.stream()
                .filter(animal -> keywords.stream().anyMatch(kw -> 
                        animal.getBreed() != null && (animal.getBreed().contains(kw) || kw.contains(animal.getBreed()))))
                .sorted(Comparator.comparing(RescueAnimalCache::getRescueDate).reversed())
                .limit(5)
                .collect(Collectors.toList());

        // [방어 코드] 만약 해당 추천견 품종 데이터가 전혀 조회되지 않는다면, 
        // 화면 노출 누락 방지를 위해 최신 구조된 동물 5마리를 기본 노출하도록 Fallback 적용합니다.
        if (matched.isEmpty() && !allCached.isEmpty()) {
            matched = allCached.stream()
                    .sorted(Comparator.comparing(RescueAnimalCache::getRescueDate).reversed())
                    .limit(5)
                    .collect(Collectors.toList());
        }

        return matched.stream()
                .map(animal -> {
                    boolean isExact = keywords.stream().anyMatch(kw -> 
                            animal.getBreed() != null && (animal.getBreed().contains(kw) || kw.contains(animal.getBreed())));
                    String matchReason = isExact 
                            ? "사용자님의 성향 분석 매칭에 따라 적합한 추천 품종 구조견입니다."
                            : "추천 품종의 데이터가 부족하여 대체 매칭된 최신 구조동물 정보입니다.";
                            
                    return FinalReportResponse.RecommendedAnimalDto.builder()
                            .animalId(animal.getAnimalId())
                            .breed(animal.getBreed())
                            .age(animal.getAge())
                            .shelterName(animal.getShelterName())
                            .region(animal.getRegion())
                            .imageUrl(animal.getImageUrl())
                            .isFallback(animal.getIsFallback() != null && animal.getIsFallback())
                            .matchReason(matchReason)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 누적 영수증 지출 내역을 조회합니다.
     */
    @Operation(
        summary = "누적 영수증 지출 내역 조회 API",
        description = "사용자의 전체 시뮬레이션 지출 내역(총 지출 금액 및 세부 영수증 리스트)을 반환합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "지출 내역 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 토큰 누락 또는 유효 만료 상태")
    })
    @GetMapping("/expenses")
    public ResponseEntity<ExpenseReportResponse> getExpenses(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        log.info("[지출 내역 조회 API 호출] 이메일: {}", email);
        return ResponseEntity.ok(reportService.getExpenses(email));
    }

    /**
     * 사용자의 모든 양육 시뮬레이션 관련 지표 데이터를 데이터베이스에서 초기화합니다.
     */
    @Operation(
        summary = "시뮬레이션 데이터 초기화 API",
        description = "사용자의 산책, 미션 반응, 가상 훈련 기록을 초기화하고 실시간 점수와 아픔 횟수를 셋업 초기 상태로 재설정합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "시뮬레이션 초기화 성공"),
        @ApiResponse(responseCode = "401", description = "인증 토큰 누락 또는 유효 만료 상태"),
        @ApiResponse(responseCode = "500", description = "서버 내부 처리 오류")
    })
    @PostMapping("/reset")
    public ResponseEntity<Void> resetSimulation(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        log.info("[시뮬레이션 초기화 API 호출] 사용자 이메일: {}", email);
        
        resetService.resetSimulation(email);
        
        return ResponseEntity.ok().build();
    }
}
