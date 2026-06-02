package com.petready.backend.domain.report.api;

import com.petready.backend.domain.analysis.entity.UserAnalysisResult;
import com.petready.backend.domain.analysis.service.AnalysisService;
import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.mission.entity.Mission;
import com.petready.backend.domain.mission.repository.MissionRepository;
import com.petready.backend.domain.report.dto.FinalReportResponse;
import com.petready.backend.domain.report.entity.PetReport;
import com.petready.backend.domain.report.repository.PetReportRepository;
import com.petready.backend.domain.rescue.entity.RescueAnimalCache;
import com.petready.backend.domain.rescue.repository.RescueAnimalCacheRepository;
import com.petready.backend.domain.score.entity.RealTimeScore;
import com.petready.backend.domain.score.repository.RealTimeScoreRepository;
import com.petready.backend.domain.walk.entity.Walk;
import com.petready.backend.domain.walk.repository.WalkRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 최종 양육 리포트 관련 API 엔드포인트를 제공하는 컨트롤러입니다.
 */
@Tag(name = "Report", description = "최종 양육 리포트 및 매칭 추천 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportController {

    private final AnalysisService analysisService;
    private final DeviceRepository deviceRepository;
    private final WalkRepository walkRepository;
    private final MissionRepository missionRepository;
    private final RealTimeScoreRepository realTimeScoreRepository;
    private final PetReportRepository reportRepository;
    private final RescueAnimalCacheRepository rescueAnimalCacheRepository;

    /**
     * 사용자의 최종 양육 시뮬레이션 리포트 결과 및 매칭되는 실제 구조동물 목록을 반환합니다.
     *
     * @param userDetails 인증된 사용자 정보
     * @return 최종 양육 리포트 응답 DTO
     */
    @Operation(summary = "최종 양육 리포트 조회", description = "사용자의 성적표 지표, AI 분석 유형 결과, 그리고 매칭되는 실제 유기견 데이터를 동적으로 융합하여 반환합니다.")
    @GetMapping("/final")
    public ResponseEntity<FinalReportResponse> getFinalReport(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        log.info("[리포트 컨트롤러] 최종 리포트 조회 요청 - User: {}", email);

        // 1. 최신 시뮬레이션 지표 기준 AI 분석 수행 및 결과 갱신
        UserAnalysisResult analysisResult = analysisService.performAnalysis(email);

        // 2. 기기 및 실시간 점수 조회
        Device device = deviceRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저에게 등록된 기기가 없습니다."));
        
        RealTimeScore realTimeScore = realTimeScoreRepository.findById(device.getDeviceId())
                .orElseGet(() -> RealTimeScore.builder()
                        .device(device)
                        .currentScore(100)
                        .build());
        
        int finalScore = realTimeScore.getCurrentScore();

        // 3. 누적 지표 조회 및 산출
        List<Walk> walks = walkRepository.findAllByUserEmail(email);
        List<Mission> missions = missionRepository.findAllByDeviceUserEmail(email);
        
        // 산책 점수 (실제/목표 비율 백분율화)
        double totalActualWalk = walks.stream().mapToDouble(w -> w.getDistanceKm().doubleValue()).sum();
        double totalGoalWalk = walks.stream().mapToDouble(w -> w.getWalkGoalKm().doubleValue()).sum();
        int walkScore = totalGoalWalk == 0 ? 0 : Math.min(100, (int) ((totalActualWalk / totalGoalWalk) * 100));

        // 알림 반응 점수 (미션 성공률 백분율화)
        long totalMissions = missions.size();
        long completedMissions = missions.stream().filter(Mission::getIsCompleted).count();
        int responseScore = totalMissions == 0 ? 100 : (int) (((double) completedMissions / totalMissions) * 100);

        // 건강 벌점 (방전 벌점 N_sick * 15)
        int healthPenalty = device.getSickCount() * 15;

        // 평균 응답 시간
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

        // 총 가상 영수증 누적액
        int totalMedicalFee = reportRepository.findByUserEmail(email)
                .map(PetReport::getTotalReceiptAmount)
                .orElse(0L)
                .intValue();

        // 등급 산출
        String grade = determineGrade(finalScore);

        // 4. 구조동물 추천 목록 매칭 (rescue_animals_cache 조회 및 필터링)
        List<FinalReportResponse.RecommendedAnimalDto> recommendedAnimals = matchRescueAnimals(analysisResult);

        // 5. 최종 응답 조립
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
                .contextMessage(analysisResult.getContextMessage())
                .recommendedAnimals(recommendedAnimals)
                .build();

        return ResponseEntity.ok(response);
    }

    private String determineGrade(int score) {
        if (score >= 90) return "A+";
        if (score >= 80) return "A";
        if (score >= 70) return "B+";
        if (score >= 60) return "B";
        if (score >= 50) return "C";
        return "F";
    }

    /**
     * 추천된 대표 품종을 기반으로 DB 캐시 테이블에서 매칭되는 구조동물 최대 5마리를 검색합니다.
     */
    private List<FinalReportResponse.RecommendedAnimalDto> matchRescueAnimals(UserAnalysisResult analysisResult) {
        String examples = analysisResult.getBreedExamples();
        List<RescueAnimalCache> allCached = rescueAnimalCacheRepository.findAll();

        if (examples == null || "없음".equals(examples) || examples.trim().isEmpty()) {
            // 추천 동물이 없는 유형(NOT_READY 등)인 경우 빈 배열 리턴
            return List.of();
        }

        // 쉼표 기준 추천 품종 키워드 리스트 추출 (예: [골든리트리버, 라브라도리트리버, 보더콜리])
        List<String> keywords = Arrays.stream(examples.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        // 캐시 테이블 전체를 탐색하며 품종 매칭 필터 및 구조일자 역순(최근 순) 정렬
        List<RescueAnimalCache> matched = allCached.stream()
                .filter(animal -> keywords.stream().anyMatch(kw -> 
                        animal.getBreed() != null && (animal.getBreed().contains(kw) || kw.contains(animal.getBreed()))))
                .sorted(Comparator.comparing(RescueAnimalCache::getRescueDate).reversed())
                .limit(5)
                .collect(Collectors.toList());

        // 만약 매칭되는 특정 품종 구조 데이터가 캐시에 전혀 없을 경우, 
        // 무조건 빈 리스트를 주기보단 최신 구조된 동물 5마리를 대체 제공하는 견고함(Robust) 발휘
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
                            .matchReason(matchReason)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
