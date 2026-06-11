package com.petready.backend.domain.report.service;

import com.petready.backend.domain.analysis.entity.UserAnalysisResult;
import com.petready.backend.domain.report.entity.PetReport;
import com.petready.backend.domain.report.repository.PetReportRepository;
import com.petready.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petready.backend.domain.user.repository.UserRepository;
import java.math.BigDecimal;

/**
 * 최종 리포트에 연관된 비즈니스 로직 및 AI 피드백 문구 영속화 관리를 수행하는 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final PetReportRepository reportRepository;
    private final UserRepository userRepository;
    private final GeminiService geminiService;

    /**
     * 최종 리포트 내의 AI 피드백 문구를 조회합니다.
     * 캐시된 문구가 없으면 실시간으로 Gemini API를 연동하여 생성 및 캐싱(영속화)합니다.
     * Dirty Checking이 정상 작동하도록 @Transactional 내에서 연산이 이루어집니다.
     */
    @Transactional
    public String getOrGenerateAiFeedback(String email, UserAnalysisResult analysisResult, int finalScore, double walkRatio, int completedMissions, int totalMissions, int sickCount, long totalTraining, double trainingSuccessRate, long confusedCount) {
        // 1. 유저의 PetReport 조회 (없을 시 H2 테스트 환경 등을 고려해 자동 생성 처리 방어)
        PetReport report = reportRepository.findByUserEmail(email)
                .orElseGet(() -> {
                    log.info("[ReportService] 유저의 기존 PetReport가 없어 신규 생성합니다. Email: {}", email);
                    User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. Email: " + email));
                    PetReport newReport = PetReport.builder()
                            .user(user)
                            .totalScore(BigDecimal.valueOf(100))
                            .grade("A+")
                            .totalReceiptAmount(0L)
                            .receiptDetailsJson("[]")
                            .totalWalkCount(0)
                            .totalMissionCount(0)
                            .build();
                    return reportRepository.save(newReport);
                });

        // 2. 이미 캐시된 AI 피드백이 존재하면 즉시 리턴 (Lazy Caching)
        if (report.getAiFeedback() != null && !report.getAiFeedback().trim().isEmpty()) {
            log.info("[ReportService] 캐시된 AI 피드백이 존재하여 즉시 반환합니다. Email: {}", email);
            return report.getAiFeedback();
        }

        // 3. 없으면 Gemini API 호출하여 생성
        log.info("[ReportService] 캐시된 AI 피드백이 없으므로 Gemini API를 호출하여 동적 생성합니다. Email: {}", email);
        User user = report.getUser();
        String userName = (user.getNickname() != null && !user.getNickname().trim().isEmpty()) 
                ? user.getNickname() 
                : user.getEmail();
        
        String generatedFeedback = geminiService.generateFeedback(
                userName,
                analysisResult.getUserTypeLabel(),
                finalScore,
                walkRatio,
                completedMissions,
                totalMissions,
                sickCount,
                analysisResult.getBreedExamples(),
                totalTraining,
                trainingSuccessRate,
                confusedCount
        );

        // 4. Dirty Checking 및 명시적 저장(테스트 Mocking 검증 호환)을 통해 변경 사항을 DB에 영속화
        report.updateAiFeedback(generatedFeedback);
        reportRepository.save(report);
        
        log.info("[ReportService] 생성된 AI 피드백을 PetReport에 캐싱 영속화 완료했습니다.");

        return generatedFeedback;
    }

}
