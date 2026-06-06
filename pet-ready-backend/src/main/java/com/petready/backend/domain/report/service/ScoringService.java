package com.petready.backend.domain.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petready.backend.domain.mission.entity.Mission;
import com.petready.backend.domain.mission.repository.MissionRepository;
import com.petready.backend.domain.report.entity.PetReport;
import com.petready.backend.domain.report.repository.PetReportRepository;
import com.petready.backend.domain.user.entity.User;
import com.petready.backend.domain.walk.entity.Walk;
import com.petready.backend.domain.walk.repository.WalkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 반려견 양육 점수 산출 및 가상 영수증 생성을 담당하는 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringService {

    private final WalkRepository walkRepository;
    private final MissionRepository missionRepository;
    private final PetReportRepository reportRepository;
    private final com.petready.backend.domain.device.repository.DeviceRepository deviceRepository;
    private final com.petready.backend.domain.score.repository.RealTimeScoreRepository realTimeScoreRepository;
    private final ObjectMapper objectMapper;

    /**
     * 사용자의 리포트를 누적 데이터를 바탕으로 갱신합니다.
     */
    @Transactional
    public void updateReport(User user, Walk latestWalk) {
        // 1. 기존 리포트 조회 또는 생성
        PetReport report = reportRepository.findByUserEmail(user.getEmail())
                .orElseGet(() -> PetReport.builder()
                        .user(user)
                        .totalScore(BigDecimal.ZERO)
                        .grade("F")
                        .totalReceiptAmount(0L)
                        .receiptDetailsJson("[]")
                        .totalWalkCount(0)
                        .totalMissionCount(0)
                        .build());

        // 2. 누적 통계 데이터 조회
        List<Mission> allMissions = missionRepository.findAllByDeviceUserEmail(user.getEmail());

        // 3. 기기 및 실시간 점수 조회 (레거시 가중치 공식 수식 완전 삭제 및 일원화 - BK-08)
        com.petready.backend.domain.device.entity.Device device = deviceRepository.findByUserEmail(user.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("해당 유저에게 등록된 기기가 없습니다."));
        
        com.petready.backend.domain.score.entity.RealTimeScore realTimeScore = realTimeScoreRepository.findById(device.getDeviceId())
                .orElseGet(() -> com.petready.backend.domain.score.entity.RealTimeScore.builder()
                        .device(device)
                        .currentScore(100)
                        .build());

        BigDecimal totalScore = BigDecimal.valueOf(realTimeScore.getCurrentScore());

        // 4. 가상 영수증 생성 및 패널티 합산
        List<Map<String, Object>> receiptItems = new ArrayList<>();
        long penaltyAmount = calculateReceiptAndPopulateItems(allMissions, latestWalk, receiptItems);

        // 5. 등급 판정 (A~F) - 실시간 점수 기준으로 통일
        String grade = determineGrade(totalScore);

        // 6. 리포트 업데이트
        try {
            String detailsJson = objectMapper.writeValueAsString(receiptItems);
            report.updateReport(totalScore, grade, penaltyAmount, detailsJson);
            report.incrementWalkCount();
            reportRepository.save(report);
        } catch (Exception e) {
            log.error("영수증 JSON 변환 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * 누적 산책 달성률 계산 (실제/목표 평균)
     */
    private BigDecimal calculateWalkRate(List<Walk> walks) {
        if (walks.isEmpty()) return BigDecimal.ZERO;

        BigDecimal sumRate = BigDecimal.ZERO;
        for (Walk walk : walks) {
            // Division by Zero 방어
            if (walk.getWalkGoalKm().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal rate = walk.getDistanceKm().divide(walk.getWalkGoalKm(), 4, RoundingMode.HALF_UP);
                sumRate = sumRate.add(rate.multiply(new BigDecimal("100")));
            }
        }
        return sumRate.divide(new BigDecimal(walks.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * 누적 미션 응답률 계산
     */
    private BigDecimal calculateMissionRate(List<Mission> missions) {
        if (missions.isEmpty()) return new BigDecimal("100"); // 미션이 없으면 만점

        long completedCount = missions.stream().filter(Mission::getIsCompleted).count();
        return new BigDecimal(completedCount)
                .divide(new BigDecimal(missions.size()), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 건강 패널티 계산 (이번 산책 달성률 50% 미만 등)
     */
    private BigDecimal calculateHealthPenalty(Walk latestWalk) {
        BigDecimal penalty = BigDecimal.ZERO;
        if (latestWalk.getWalkGoalKm().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal rate = latestWalk.getDistanceKm().divide(latestWalk.getWalkGoalKm(), 4, RoundingMode.HALF_UP);
            if (rate.compareTo(new BigDecimal("0.5")) < 0) {
                penalty = new BigDecimal("20.00"); // 50% 미만 시 20점 패널티
            }
        }
        return penalty;
    }

    /**
     * 가상 영수증 내역 생성 및 총액 반환
     */
    private long calculateReceiptAndPopulateItems(List<Mission> missions, Walk latestWalk, List<Map<String, Object>> items) {
        long totalPenalty = 0;

        // 1. 의료비: 미션 지연 패널티
        for (Mission m : missions) {
            if (m.getRespondedAt() != null && m.getResponseTimeSec() != null) {
                long delayMin = m.getResponseTimeSec() / 60;
                if (delayMin > 0) {
                    long amount = delayMin * 5000;
                    totalPenalty += amount;
                    addItem(items, "미션 대응 지연 진료비", amount, delayMin + "분 지연");

                    if (delayMin > 10) {
                        totalPenalty += 200000;
                        addItem(items, "응급실 긴급 이송비", 200000L, "응답 10분 초과");
                    }
                }
            }
        }

        // 2. 활동비: 산책 달성률 50% 미만
        if (latestWalk.getWalkGoalKm().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal rate = latestWalk.getDistanceKm().divide(latestWalk.getWalkGoalKm(), 4, RoundingMode.HALF_UP);
            if (rate.compareTo(new BigDecimal("0.5")) < 0) {
                totalPenalty += 30000;
                addItem(items, "건강 관리 경고비", 30000L, "산책 달성률 50% 미만");
            }
        }

        return totalPenalty;
    }

    private void addItem(List<Map<String, Object>> items, String item, long amount, String reason) {
        Map<String, Object> map = new HashMap<>();
        map.put("item", item);
        map.put("amount", amount);
        map.put("reason", reason);
        items.add(map);
    }

    private String determineGrade(BigDecimal score) {
        if (score.compareTo(new BigDecimal("90")) >= 0) return "A+";
        if (score.compareTo(new BigDecimal("80")) >= 0) return "A";
        if (score.compareTo(new BigDecimal("70")) >= 0) return "B+";
        if (score.compareTo(new BigDecimal("60")) >= 0) return "B";
        if (score.compareTo(new BigDecimal("50")) >= 0) return "C";
        return "F";
    }
}
