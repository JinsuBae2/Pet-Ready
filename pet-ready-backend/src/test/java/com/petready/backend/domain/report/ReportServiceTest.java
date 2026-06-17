package com.petready.backend.domain.report;

import com.petready.backend.domain.analysis.entity.UserAnalysisResult;
import com.petready.backend.domain.report.entity.PetReport;
import com.petready.backend.domain.report.repository.PetReportRepository;
import com.petready.backend.domain.report.service.GeminiService;
import com.petready.backend.domain.report.service.ReportService;
import com.petready.backend.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @Mock
    private PetReportRepository reportRepository;

    @Mock
    private com.petready.backend.domain.user.repository.UserRepository userRepository;

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private ReportService reportService;

    private User testUser;
    private PetReport testReport;
    private UserAnalysisResult analysisResult;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@petready.com")
                .nickname("테스터")
                .build();

        testReport = PetReport.builder()
                .id(1L)
                .user(testUser)
                .totalScore(BigDecimal.valueOf(90))
                .grade("A")
                .totalReceiptAmount(100000L)
                .totalWalkCount(5)
                .totalMissionCount(10)
                .build();

        analysisResult = UserAnalysisResult.builder()
                .userTypeLabel("준비된 활동가형")
                .breedExamples("푸들, 말티즈")
                .build();
    }

    @Test
    @DisplayName("최초 조회 시: 캐시된 AI 피드백이 없어 Gemini API를 호출하고 결과를 DB에 저장(캐싱)한다")
    void testGetOrGenerateAiFeedback_FirstRequest() {
        // given
        String email = "test@petready.com";
        String expectedFeedback = "Gemini가 작성한 훌륭한 피드백입니다. 푸들과 말티즈는 교감이 중요합니다.";
        
        when(reportRepository.findByUserEmail(email)).thenReturn(Optional.of(testReport));
        when(geminiService.generateFeedback(
                eq("테스터"), eq("준비된 활동가형"), eq(90), eq(0.95), eq(8), eq(10), eq(1), eq("푸들, 말티즈"), eq(5L), eq(80.0), eq(1L)
        )).thenReturn(expectedFeedback);

        // when
        String result = reportService.getOrGenerateAiFeedback(
                email, analysisResult, 90, 0.95, 8, 10, 1, 5L, 80.0, 1L
        );

        // then
        assertThat(result).isEqualTo(expectedFeedback);
        assertThat(testReport.getAiFeedback()).isEqualTo(expectedFeedback);
        
        // Dirty checking 이외의 추가적인 JPA 보증을 위한 save 호출 검증
        verify(reportRepository, times(1)).save(testReport);
        verify(geminiService, times(1)).generateFeedback(anyString(), anyString(), anyInt(), anyDouble(), anyInt(), anyInt(), anyInt(), anyString(), anyLong(), anyDouble(), anyLong());
    }

    @Test
    @DisplayName("반복 조회 시: 이미 캐시된 AI 피드백이 존재하면 Gemini API를 호출하지 않고 기존 캐시값을 즉시 리턴한다")
    void testGetOrGenerateAiFeedback_SecondRequestWithCache() {
        // given
        String email = "test@petready.com";
        String cachedFeedback = "이미 저장되어 있는 AI 평론 텍스트입니다.";
        
        // 이미 피드백이 세팅된 리포트 빌드
        PetReport cachedReport = PetReport.builder()
                .id(1L)
                .user(testUser)
                .totalScore(BigDecimal.valueOf(90))
                .grade("A")
                .totalReceiptAmount(100000L)
                .totalWalkCount(5)
                .totalMissionCount(10)
                .build();
        cachedReport.updateAiFeedback(cachedFeedback);

        when(reportRepository.findByUserEmail(email)).thenReturn(Optional.of(cachedReport));

        // when
        String result = reportService.getOrGenerateAiFeedback(
                email, analysisResult, 90, 0.95, 8, 10, 1, 5L, 80.0, 1L
        );

        // then
        assertThat(result).isEqualTo(cachedFeedback);
        
        // Gemini API가 절대 호출되지 않아야 함
        verify(geminiService, never()).generateFeedback(anyString(), anyString(), anyInt(), anyDouble(), anyInt(), anyInt(), anyInt(), anyString(), anyLong(), anyDouble(), anyLong());
        // 이미 캐시되어 있으므로 저장(save)도 수행하지 않음
        verify(reportRepository, never()).save(any(PetReport.class));
    }

    @Test
    @DisplayName("최초 조회 시 리포트가 없으면: 리포트를 신규 생성한 뒤 Gemini API를 호출하여 결과를 저장한다")
    void testGetOrGenerateAiFeedback_NoReportExists() {
        // given
        String email = "test@petready.com";
        String expectedFeedback = "신규 리포트 생성 및 피드백입니다.";
        
        when(reportRepository.findByUserEmail(email)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(reportRepository.save(any(PetReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(geminiService.generateFeedback(
                eq("테스터"), eq("준비된 활동가형"), eq(90), eq(0.95), eq(8), eq(10), eq(1), eq("푸들, 말티즈"), eq(5L), eq(80.0), eq(1L)
        )).thenReturn(expectedFeedback);

        // when
        String result = reportService.getOrGenerateAiFeedback(
                email, analysisResult, 90, 0.95, 8, 10, 1, 5L, 80.0, 1L
        );

        // then
        assertThat(result).isEqualTo(expectedFeedback);
        verify(userRepository, times(1)).findByEmail(email);
        // save()는 1) newReport 저장 시, 2) aiFeedback 캐싱 저장 시 총 2번 호출되어야 함
        verify(reportRepository, times(2)).save(any(PetReport.class));
    }

    @Test
    @DisplayName("최초 조회 시 Gemini API가 실패하여 null을 리턴하면: fallback 피드백을 반환하고 DB에는 캐싱하지 않는다")
    void testGetOrGenerateAiFeedback_ApiFailureNoCache() {
        // given
        String email = "test@petready.com";
        String expectedFallback = "기본 Fallback 피드백 텍스트입니다.";
        
        when(reportRepository.findByUserEmail(email)).thenReturn(Optional.of(testReport));
        when(geminiService.generateFeedback(
                eq("테스터"), eq("준비된 활동가형"), eq(90), eq(0.95), eq(8), eq(10), eq(1), eq("푸들, 말티즈"), eq(5L), eq(80.0), eq(1L)
        )).thenReturn(null);
        when(geminiService.generateFallbackFeedback(
                eq("준비된 활동가형"), eq(90), eq(0.95), eq(1), eq(80.0)
        )).thenReturn(expectedFallback);

        // when
        String result = reportService.getOrGenerateAiFeedback(
                email, analysisResult, 90, 0.95, 8, 10, 1, 5L, 80.0, 1L
        );

        // then
        assertThat(result).isEqualTo(expectedFallback);
        assertThat(testReport.getAiFeedback()).isNull(); // 캐싱되지 않았어야 함
        
        // save()는 리포트 조회/저장에 사용되지 않았어야 함 (캐시가 업데이트 되지 않으므로)
        verify(reportRepository, never()).save(any(PetReport.class));
    }
}
