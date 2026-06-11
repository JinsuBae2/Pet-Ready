package com.petready.backend.domain.analysis.service;

import com.petready.backend.domain.analysis.entity.UserAnalysisResult;
import com.petready.backend.domain.analysis.repository.UserAnalysisResultRepository;
import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.mission.entity.Mission;
import com.petready.backend.domain.mission.repository.MissionRepository;
import com.petready.backend.domain.score.entity.RealTimeScore;
import com.petready.backend.domain.score.repository.RealTimeScoreRepository;
import com.petready.backend.domain.user.entity.User;
import com.petready.backend.domain.user.repository.UserRepository;
import com.petready.backend.domain.walk.entity.Walk;
import com.petready.backend.domain.walk.repository.WalkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자의 시뮬레이션 지표를 분석하고 AI 기반 양육 성향 유형 및 맞춤 추천 결과를 관리하는 서비스 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final WalkRepository walkRepository;
    private final MissionRepository missionRepository;
    private final RealTimeScoreRepository realTimeScoreRepository;
    private final UserAnalysisResultRepository userAnalysisResultRepository;
    private final WekaClassifierHelper wekaClassifierHelper;

    /**
     * 사용자의 최신 시뮬레이션 지표를 수집하여 AI 분석(Weka RF & K-Means)을 수행하고 결과를 데이터베이스에 영속화합니다.
     *
     * @param email 사용자 이메일
     * @return 생성된 분석 결과 엔티티
     */
    @Transactional
    public UserAnalysisResult performAnalysis(String email) {
        log.info("[AI 분석 엔진] 분석 시작 - 유저 이메일: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. Email: " + email));

        // 1. 유저 기기 조회
        Device device = deviceRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저에게 등록된 기기가 없습니다."));

        String deviceId = device.getDeviceId();

        // 2. 누적 지표 데이터베이스 조회
        List<Walk> walks = walkRepository.findAllByUserEmail(email);
        List<Mission> missions = missionRepository.findAllByDeviceUserEmail(email);
        
        RealTimeScore realTimeScore = realTimeScoreRepository.findById(deviceId)
                .orElseGet(() -> RealTimeScore.builder()
                        .device(device)
                        .currentScore(100)
                        .build());
        
        int currentScore = realTimeScore.getCurrentScore();

        // 3. 5대 분석 특성 벡터(F1 ~ F5) 산출
        // F1: 미션 완료율 (0.0 ~ 1.0)
        double f1 = 1.0;
        long totalMissions = missions.size();
        if (totalMissions > 0) {
            long completedMissions = missions.stream().filter(Mission::getIsCompleted).count();
            f1 = (double) completedMissions / totalMissions;
        }

        // F2: 평균 응답 속도 감점률 (0.0 ~ 1.0, 1800초(30분) 기준 정규화)
        double f2 = 0.0;
        double avgResponseSec = 0.0;
        long respondedCount = missions.stream()
                .filter(m -> m.getIsCompleted() && m.getResponseTimeSec() != null)
                .count();
        if (respondedCount > 0) {
            long sumResponseSec = missions.stream()
                    .filter(m -> m.getIsCompleted() && m.getResponseTimeSec() != null)
                    .mapToLong(Mission::getResponseTimeSec)
                    .sum();
            avgResponseSec = (double) sumResponseSec / respondedCount;
            f2 = avgResponseSec / 1800.0;
            f2 = Math.min(1.0, f2); // 최대 1.0 한계 보정
        }

        // F3: 산책 목표 달성률 (0.0 ~ 2.0, 실제거리 / 목표거리)
        double f3 = 0.0;
        double totalActualWalk = walks.stream().mapToDouble(w -> w.getDistanceKm().doubleValue()).sum();
        double totalGoalWalk = walks.stream().mapToDouble(w -> w.getWalkGoalKm().doubleValue()).sum();
        if (totalGoalWalk > 0) {
            f3 = totalActualWalk / totalGoalWalk;
            f3 = Math.min(2.0, f3); // 최대 2.0 한계 보정
        }

        // F4: 방전 벌점 페널티 (0.0 ~ 1.0, N_sick * 0.1)
        double f4 = device.getSickCount() * 0.1;
        f4 = Math.min(1.0, f4);

        // F5: 최근 점수 상태 트렌드 (0.0 ~ 1.0, currentScore / 100.0)
        double f5 = currentScore / 100.0;
        f5 = Math.min(1.0, Math.max(0.0, f5));

        log.info("[AI 분석 엔진] 특성 벡터 산출 - F1(완료율): {}, F2(응답속도): {} (평균: {}초), F3(산책달성): {}, F4(방전): {}, F5(점수): {}",
                f1, f2, avgResponseSec, f3, f4, f5);

        // 4. ML 파이프라인(Weka RF & K-Means) 예측 수행 (예외 발생 시 규칙 기반 Fallback 모드로 완벽 방어)
        String userType;
        try {
            WekaClassifierHelper.MlResult mlResult = wekaClassifierHelper.analyzeParentingBehavior(f1, f2, f3, f4, f5);
            
            // 예측 분류 결과와 군집 결과를 결합하여 최종 v2.1 6대 유형으로 매핑
            if ("READY".equals(mlResult.predictedClass)) {
                if (f3 >= 0.8) {
                    userType = "READY_ACTIVE";
                } else {
                    userType = "READY_CALM";
                }
            } else if ("NEED_WORK".equals(mlResult.predictedClass)) {
                if (f3 < 0.6) {
                    userType = "NEED_WALK";
                } else if (avgResponseSec > 900) {
                    userType = "NEED_RESPONSE";
                } else {
                    userType = "CAUTION";
                }
            } else {
                userType = "NOT_READY";
            }
            log.info("[AI 분석 엔진] Weka ML 기반 유형 분류 결과: {}", userType);

        } catch (Exception e) {
            log.warn("[AI 분석 엔진] Weka 분석 중 예외 발생 - 규칙 기반(Fallback) 판별 모드로 전 전환합니다. 사유: {}", e.getMessage());
            
            // v2.1 규칙 기반 분류 방어벽 (Fallback)
            if (currentScore >= 80) {
                if (f3 >= 0.8) {
                    userType = "READY_ACTIVE";
                } else {
                    userType = "READY_CALM";
                }
            } else if (currentScore >= 60) {
                if (f3 < 0.6) {
                    userType = "NEED_WALK";
                } else if (avgResponseSec > 900) {
                    userType = "NEED_RESPONSE";
                } else {
                    userType = "CAUTION";
                }
            } else if (currentScore >= 50) {
                userType = "CAUTION";
            } else {
                userType = "NOT_READY";
            }
            log.info("[AI 분석 엔진] 규칙 기반(Fallback) 분류 결과: {}", userType);
        }

        // 5. 유형에 따른 라벨, 추천 대표 품종 및 이유, 맥락 메시지 작성
        String userTypeLabel = determineUserTypeLabel(userType);
        String breedType = determineBreedType(userType);
        String breedExamples = determineBreedExamples(userType);
        String breedReason = determineBreedReason(userType);
        String contextMessage = buildContextMessage(device.getSickCount(), f3, userType);

        // 6. DB 저장 (기존 데이터와 누적하여 쌓이도록 INSERT)
        UserAnalysisResult analysisResult = UserAnalysisResult.builder()
                .user(user)
                .userType(userType)
                .userTypeLabel(userTypeLabel)
                .breedType(breedType)
                .breedExamples(breedExamples)
                .breedReason(breedReason)
                .contextMessage(contextMessage)
                .analyzedAt(LocalDateTime.now())
                .build();

        return userAnalysisResultRepository.save(analysisResult);
    }

    private String determineUserTypeLabel(String userType) {
        switch (userType) {
            case "READY_ACTIVE": return "준비된 활동가형";
            case "READY_CALM": return "준비된 동반자형";
            case "NEED_WALK": return "산책 부족형";
            case "NEED_RESPONSE": return "피드백 지연형";
            case "CAUTION": return "주의 요망형";
            case "NOT_READY": return "양육 준비 미흡형";
            default: return "알 수 없음";
        }
    }

    private String determineBreedType(String userType) {
        switch (userType) {
            case "READY_ACTIVE": return "대형견 / 활동견";
            case "READY_CALM": return "소형견 / 차분형";
            case "NEED_WALK": return "소형견 / 활동성 보통";
            case "NEED_RESPONSE": return "독립적 성향 소형견";
            case "CAUTION": return "소형 믹스견";
            case "NOT_READY": return "추천 품종 없음";
            default: return "알 수 없음";
        }
    }

    private String determineBreedExamples(String userType) {
        switch (userType) {
            case "READY_ACTIVE": return "골든리트리버, 라브라도리트리버, 보더콜리";
            case "READY_CALM": return "푸들, 말티즈, 비숑프리제";
            case "NEED_WALK": return "시추, 요크셔테리어";
            case "NEED_RESPONSE": return "치와와, 닥스훈트";
            case "CAUTION": return "믹스견";
            case "NOT_READY": return "없음";
            default: return "없음";
        }
    }

    private String determineBreedReason(String userType) {
        switch (userType) {
            case "READY_ACTIVE":
                return "사용자님은 매우 뛰어난 야외 산책 달성률과 적극적인 돌봄 태도를 보여주어 활동량이 많고 활발한 성격의 대형견이나 목양견 그룹을 기르기에 최적화된 양육 환경을 제공하고 있습니다.";
            case "READY_CALM":
                return "사용자님은 규칙적이고 높은 미션 반응 속도를 보여주며 건강 관리가 우수합니다. 활동량이 보통 수준이므로, 실내 활동과 차분한 교감을 즐기는 소형견이나 차분한 성향의 품종을 권장합니다.";
            case "NEED_WALK":
                return "신속한 미션 해결 능력에 비해 산책 빈도 및 활동량이 다소 부족합니다. 에너지가 적당하고 실내 적응력이 높은 소형견 그룹을 권장하며, 산책 루틴을 조금 더 안정화시키는 노력이 필요합니다.";
            case "NEED_RESPONSE":
                return "산책 달성 수준은 양호하나, 실시간 반려견 짖음/배고픔 반응 속도가 느린 편입니다. 주인과의 교감 빈도가 아주 조밀하지 않아도 잘 지낼 수 있는 비교적 독립적인 성향을 가진 치와와나 닥스훈트 등을 권장합니다.";
            case "CAUTION":
                return "산책과 미션 반응 빈도가 다소 우려되는 상태입니다. 입양 전에 반려견과의 약속을 더욱 세심하게 돌볼 수 있도록 시간 관리와 마음의 준비가 선행되어야 합니다.";
            case "NOT_READY":
                return "현재 시뮬레이션 돌봄 지표(산책 및 알림 반응)가 입양 적격 수준에 미치지 못합니다. 생명을 집안에 맞이하기 전 반려견 양육을 위한 일상 루틴 개선이 강력하게 요구됩니다.";
            default:
                return "";
        }
    }

    private String buildContextMessage(int sickCount, double walkRatio, String userType) {
        StringBuilder sb = new StringBuilder();
        sb.append("KOSIS 국가통계 및 유기동물 조사 자료에 따르면 매년 약 10만 마리 이상의 구조동물이 발생하며, 이 중 20% 이상이 입양되지 못하고 인도적 처리(안락사)됩니다. 유기동물의 주요 원인은 경제적 부담 및 돌봄 부재, 그리고 이상 행동 문제입니다.\n\n");

        if (sickCount >= 2) {
            sb.append("⚠️ [방임 방지 경고] 시뮬레이션 중 기기 방전으로 인한 아픔 횟수가 ").append(sickCount).append("회 기록되었습니다. 현실에서 기기 방전은 심각한 영양 결핍이나 질환의 장기간 방치와 동일하며, 이는 생명을 다루는 상황에서는 치료비 급증 및 파양으로 이어지는 악순환이 될 수 있습니다. 매 순간의 돌봄 책임이 생명을 살립니다.\n\n");
        }
        
        if (walkRatio < 0.6) {
            sb.append("🐾 [산책 의무 경고] 누적 산책 달성율이 ").append(Math.round(walkRatio * 100)).append("%로 매우 저조합니다. 많은 반려견의 행동 문제(짖음, 파괴 행동 등)는 스트레스 해소가 되지 않아 유발되며, 이는 유기 및 파양의 핵심적인 원인이 됩니다. 주기적인 산책은 단순히 놀이가 아닌 의무적인 복지 수단입니다.\n\n");
        }

        if ("READY_ACTIVE".equals(userType) || "READY_CALM".equals(userType)) {
            sb.append("축하드립니다! 사용자님은 펫-레디 시뮬레이션을 통해 생명을 책임질 높은 준비도를 입증하셨습니다. 앞으로 동물의 평생을 함께할 굳은 책임감을 실천해주시길 기대합니다.");
        } else {
            sb.append("시뮬레이션을 통해 점검된 돌봄 루틴의 취약 요소를 먼저 극복하신 후 입양을 진지하게 고려해 보시기를 권장합니다. 준비된 입양만이 파양을 예방합니다.");
        }

        return sb.toString();
    }
}
