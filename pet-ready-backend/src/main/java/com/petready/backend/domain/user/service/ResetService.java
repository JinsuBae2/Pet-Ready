package com.petready.backend.domain.user.service;

import com.petready.backend.domain.analysis.repository.UserAnalysisResultRepository;
import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.mission.repository.MissionRepository;
import com.petready.backend.domain.mission.repository.TrainingLogRepository;
import com.petready.backend.domain.report.repository.PetReportRepository;
import com.petready.backend.domain.score.entity.RealTimeScore;
import com.petready.backend.domain.score.repository.RealTimeScoreRepository;
import com.petready.backend.domain.user.entity.User;
import com.petready.backend.domain.user.repository.UserRepository;
import com.petready.backend.domain.walk.repository.WalkRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 사용자 시뮬레이션 지표 초기화 및 회원 탈퇴(완전 삭제) 처리를 수행하는 서비스 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResetService {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final WalkRepository walkRepository;
    private final MissionRepository missionRepository;
    private final TrainingLogRepository trainingLogRepository;
    private final PetReportRepository reportRepository;
    private final UserAnalysisResultRepository analysisResultRepository;
    private final RealTimeScoreRepository scoreRepository;
    private final EntityManager entityManager;

    /**
     * 사용자의 3주 양육 시뮬레이션 지표 데이터를 모두 삭제하고, 기기 센서 통계 및 실시간 점수를 최초 상태(100점)로 초기화합니다.
     *
     * @param email 사용자 이메일
     */
    @Transactional
    public void resetSimulation(String email) {
        log.info("[ResetService] 시뮬레이션 데이터 초기화 시작 - 대상 이메일: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. Email: " + email));

        // 조인 꼬임 방지를 위해 유저 ID 기반으로 기기 직접 조회
        Device device = deviceRepository.findAllByUserId(user.getId()).stream()
                .findFirst()
                .orElse(null);

        // 1. 산책 기록 일괄 삭제 (JPA 레벨)
        walkRepository.deleteAllByUser(user);
        log.info("[ResetService] 산책 기록 일괄 삭제 완료 (JPA)");

        // 2. 디바이스 관련 데이터 일괄 삭제 (JPA 레벨)
        if (device != null) {
            String deviceId = device.getDeviceId();

            // 점수 변동 히스토리도 함께 초기화
            deviceRepository.deleteScoreEventsByDeviceId(deviceId);

            // 미션 로그 및 가상 훈련 기록 삭제
            missionRepository.deleteAllByDevice(device);
            trainingLogRepository.deleteAllByDevice(device);
            log.info("[ResetService] 미션 및 가상 훈련 로그 삭제 완료 (JPA)");

            // 아픔 횟수(배터리 방전) 0으로 초기화
            device.resetSickCount();
            deviceRepository.save(device);
            log.info("[ResetService] 디바이스 아픔 횟수 리셋 완료");

            // 실시간 점수 100점으로 리셋 (존재 시 값 변경, 미존재 시 신규 생성)
            RealTimeScore score = scoreRepository.findById(deviceId).orElse(null);
            if (score != null) {
                score.resetScoreToMax();
                log.info("[ResetService] 기존 실시간 점수 레코드 100점 초기화 완료 (Dirty Checking)");
            } else {
                RealTimeScore initialScore = RealTimeScore.builder()
                        .deviceId(deviceId)
                        .device(device)
                        .currentScore(100)
                        .lastUpdatedAt(LocalDateTime.now())
                        .build();
                scoreRepository.save(initialScore);
                log.info("[ResetService] 신규 실시간 점수 레코드 100점 생성 완료");
            }
        }

        // 3. 최종 보고서 및 성향 분석 캐시 삭제
        reportRepository.deleteByUser(user);
        analysisResultRepository.deleteByUser(user);
        log.info("[ResetService] 최종 리포트 및 AI 성향 분석 결과 캐시 삭제 완료");

        // 1차 캐시 찌꺼기를 날려 영속성 불일치 예방 (동기화 후 비우기)
        entityManager.flush();
        entityManager.clear();

        log.info("[ResetService] 시뮬레이션 데이터 초기화 성공 - 대상 이메일: {}", email);
    }

    /**
     * 유저 회원 정보 및 연동된 기기, 실시간 점수 찌꺼기를 포함한 모든 연관 지표 데이터를 데이터베이스에서 일괄 삭제(탈퇴)합니다.
     *
     * @param email 사용자 이메일
     */
    @Transactional
    public void withdrawUser(String email) {
        log.info("[ResetService] 회원 탈퇴 및 데이터 완전 청소 시작 - 대상 이메일: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. Email: " + email));

        // 조인 꼬임 방지를 위해 유저 ID 기반으로 기기 직접 조회
        Device device = deviceRepository.findAllByUserId(user.getId()).stream()
                .findFirst()
                .orElse(null);

        // 1. 산책, 리포트 및 분석 결과 삭제
        walkRepository.deleteAllByUser(user);
        reportRepository.deleteByUser(user);
        analysisResultRepository.deleteByUser(user);
        log.info("[ResetService] 산책, 리포트 및 AI 성향 분석 결과 삭제 완료 (JPA)");

        if (device != null) {
            String deviceId = device.getDeviceId();

            // FK 연관 데이터 순차 삭제
            deviceRepository.deleteScoreEventsByDeviceId(deviceId);
            deviceRepository.deleteCommandsByDeviceId(deviceId);
            deviceRepository.deletePetStatusLogsByDeviceId(deviceId);

            // 미션 및 가상 훈련 로그 삭제 (JPA 레벨)
            missionRepository.deleteAllByDevice(device);
            trainingLogRepository.deleteAllByDevice(device);
            log.info("[ResetService] 미션 및 가상 훈련 로그 삭제 완료 (JPA)");

            // 실시간 점수 테이블 정보 완전 삭제
            scoreRepository.findById(deviceId).ifPresent(scoreRepository::delete);
            log.info("[ResetService] 실시간 점수 레코드 삭제 완료");

            // 디바이스 기기 연결 정보 삭제
            deviceRepository.delete(device);
            log.info("[ResetService] 등록된 기기 정보 삭제 완료");
        }

        // 2. 최종적으로 회원 엔티티 영구 삭제
        userRepository.delete(user);
        log.info("[ResetService] 회원 계정 정보 영구 삭제 완료");

        // 1차 캐시 찌꺼기를 날려 영속성 불일치 예방 (동기화 후 비우기)
        entityManager.flush();
        entityManager.clear();

        log.info("[ResetService] 회원 탈퇴 및 데이터 완전 청소 성공 - 대상 이메일: {}", email);
    }
}
