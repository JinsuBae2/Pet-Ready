package com.petready.backend.domain.mission.service;

import com.petready.backend.domain.command.entity.Command;
import com.petready.backend.domain.command.repository.CommandRepository;
import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.mission.dto.MissionResponse;
import com.petready.backend.domain.mission.entity.Mission;
import com.petready.backend.domain.mission.entity.MissionStatus;
import com.petready.backend.domain.mission.repository.MissionRepository;
import com.petready.backend.domain.medical.entity.MedicalFeeCache;
import com.petready.backend.domain.medical.repository.MedicalFeeCacheRepository;
import com.petready.backend.domain.notification.service.FcmNotificationService;
import com.petready.backend.domain.report.entity.PetReport;
import com.petready.backend.domain.report.repository.PetReportRepository;
import com.petready.backend.domain.user.entity.User;
import com.petready.backend.global.enums.NotificationType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 사용자 미션 관련 비즈니스 로직을 처리하는 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissionService {

    private final MissionRepository missionRepository;
    private final DeviceRepository deviceRepository;
    private final MedicalFeeCacheRepository medicalFeeCacheRepository;
    private final PetReportRepository reportRepository;
    private final ObjectMapper objectMapper;
    private final FcmNotificationService fcmNotificationService;
    private final CommandRepository commandRepository;

    /**
     * 사용자가 미션을 완료(응답) 처리합니다.
     * 
     * @param missionId 완료할 미션의 식별자
     * @param email 사용자 이메일 (소유권 검증용)
     */
    @Transactional
    public void completeMission(Long missionId, String email) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 미션입니다."));

        // 1. 소유권 검증
        validateMissionOwnership(mission, email);

        // 2. 멱등성 및 중복 완료 처리 방어: 이미 완료된 미션인 경우 로직 스킵하여 FCM 중복 발송 차단
        if (MissionStatus.COMPLETED == mission.getStatus() || Boolean.TRUE.equals(mission.getIsCompleted())) {
            log.info("미션 [{}]은 이미 완료된 상태이므로 완료 및 FCM 발송 로직을 스킵합니다.", missionId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        long responseTimeSec = ChronoUnit.SECONDS.between(mission.getIssuedAt(), now);

        mission.complete(now, responseTimeSec);
        missionRepository.save(mission);
        log.info("미션 [{}] 완료 처리 완료. 응답 시간: {}초", missionId, responseTimeSec);

        // [이벤트 기반 제어] FEEDING 미션 완료 시 비전 종료 명령(STOP_VISION) 주입
        if ("FEEDING".equalsIgnoreCase(mission.getType()) && mission.getDevice() != null) {
            Command stopVisionCommand = Command.builder()
                    .device(mission.getDevice())
                    .command("STOP_VISION")
                    .durationSec(0)
                    .build();
            commandRepository.save(stopVisionCommand);
            log.info("기기 [{}] - 미션 완료에 따른 비전 종료 명령어(STOP_VISION) 생성 완료.", mission.getDevice().getDeviceId());
        }

        // 3. 완료 시 FCM 알림 전송 (MISSION_COMPLETED 전용 JSON 데이터 페이로드 전송)
        User user = mission.getDevice().getUser();
        if (user != null && user.getFcmToken() != null && !user.getFcmToken().isEmpty()) {
            fcmNotificationService.sendNotification(
                    user.getFcmToken(),
                    "미션 성공 완료!",
                    "[" + mission.getType() + "] 미션을 성공적으로 완료했습니다.",
                    NotificationType.MISSION_COMPLETED,
                    missionId
            );
        } else {
            log.warn("사용자 [{}]의 FCM 토큰이 존재하지 않아 미션 완료 알림을 전송하지 못했습니다.", user != null ? user.getEmail() : "null");
        }

        // [고도화] 완료된 미션의 타입이 "MEDICAL"인 경우, 진료비 목록에서 랜덤하게 진료비 영수증 청구 (방어벽 2 반영)
        if ("MEDICAL".equalsIgnoreCase(mission.getType())) {
            // 해당 유저의 PetReport 조회
            PetReport report = reportRepository.findByUserEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("해당 사용자의 리포트가 존재하지 않습니다."));

            // 캐시된 전체 진료비 목록 로드
            List<MedicalFeeCache> feeCaches = medicalFeeCacheRepository.findAll();
            if (!feeCaches.isEmpty()) {
                Random random = new Random();
                // 1~2개 항목 랜덤 추첨
                int selectCount = random.nextInt(2) + 1; 
                long totalAddedAmount = 0;
                List<Map<String, Object>> newItems = new ArrayList<>();

                for (int i = 0; i < selectCount; i++) {
                    MedicalFeeCache selected = feeCaches.get(random.nextInt(feeCaches.size()));
                    long amount = selected.getFeeAmount().longValue();
                    totalAddedAmount += amount;

                    Map<String, Object> item = new HashMap<>();
                    item.put("item", selected.getItemName());
                    item.put("amount", amount);
                    item.put("reason", "돌발 아픔 미션 진료비");
                    newItems.add(item);
                }

                // 기존 JSON Array를 ObjectMapper를 활용해 안정적으로 파싱 및 Append (단순 String 가산 금지 제약 준수)
                List<Map<String, Object>> existingItems = new ArrayList<>();
                String existingJson = report.getReceiptDetailsJson();
                if (existingJson != null && !existingJson.isBlank()) {
                    try {
                        existingItems = objectMapper.readValue(existingJson, new TypeReference<List<Map<String, Object>>>() {});
                    } catch (Exception e) {
                        log.error("기존 영수증 JSON 파싱 실패: {}", e.getMessage());
                        existingItems = new ArrayList<>();
                    }
                }

                // 새 아이템 병합
                existingItems.addAll(newItems);

                // 다시 Serialize
                String updatedJson = "[]";
                try {
                    updatedJson = objectMapper.writeValueAsString(existingItems);
                } catch (Exception e) {
                    log.error("영수증 JSON 직렬화 실패: {}", e.getMessage());
                }

                // 리포트 엔티티 업데이트 (기존 총액에 누적 합산)
                report.updateReport(report.getTotalScore(), report.getGrade(), totalAddedAmount, updatedJson);
                reportRepository.save(report);

                log.info("아픔 미션 [{}] 진료비 청구 성공 - 추가액: {}원, 총액: {}원", 
                        missionId, totalAddedAmount, report.getTotalReceiptAmount());
            }
        }
    }

    /**
     * 미션의 소유주 이메일과 인증된 이메일이 일치하는지 검증합니다.
     */
    private void validateMissionOwnership(Mission mission, String email) {
        if (mission.getDevice() == null || mission.getDevice().getUser() == null || 
                !mission.getDevice().getUser().getEmail().equals(email)) {
            throw new org.springframework.security.access.AccessDeniedException("해당 미션에 대한 접근 권한이 없습니다.");
        }
    }

    /**
     * 사용자가 미션의 진행을 시작합니다. (status: IN_PROGRESS, startedAt 기록)
     * 
     * @param missionId 시작할 미션 식별자
     * @param email 사용자 이메일 (소유권 검증용)
     * @return 갱신된 미션 DTO
     */
    @Transactional
    public MissionResponse startMission(Long missionId, String email) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 미션입니다."));

        // 소유권 검증
        validateMissionOwnership(mission, email);

        // 멱등성 보장: 중복 시작 요청 시 기존 IN_PROGRESS 또는 COMPLETED 반환
        if (mission.getStatus() == MissionStatus.IN_PROGRESS || mission.getStatus() == MissionStatus.COMPLETED) {
            log.info("미션 [{}]이 이미 진행 중이거나 완료되었습니다. 기존 상태를 그대로 반환합니다.", missionId);
            return MissionResponse.from(mission);
        }

        mission.start(LocalDateTime.now());
        missionRepository.save(mission);
        log.info("미션 [{}] 진행 시작 처리 완료.", missionId);

        // [이벤트 기반 제어] FEEDING 미션인 경우, 비전 기동 명령(START_VISION)을 디바이스 명령어 큐에 주입
        if ("FEEDING".equalsIgnoreCase(mission.getType()) && mission.getDevice() != null) {
            Command startVisionCommand = Command.builder()
                    .device(mission.getDevice())
                    .command("START_VISION")
                    .durationSec(1800) // 30분 타임아웃
                    .build();
            commandRepository.save(startVisionCommand);
            log.info("기기 [{}] - 미션 시작에 따른 비전 기동 명령어(START_VISION) 생성 완료.", mission.getDevice().getDeviceId());
        }

        return MissionResponse.from(mission);
    }

    /**
     * 단일 미션의 상세 정보 및 진행 상태를 조회합니다. (안드 폴링 대응용)
     * 
     * @param missionId 조회할 미션 식별자
     * @param email 사용자 이메일 (소유권 검증용)
     * @return 미션 상세 응답 DTO
     */
    public MissionResponse getMission(Long missionId, String email) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 미션입니다."));

        // 소유권 검증
        validateMissionOwnership(mission, email);

        return MissionResponse.from(mission);
    }

    /**
     * 특정 사용자가 등록한 기기의 오늘(자정 이후) 발급된 미션 목록을 조회합니다.
     * 오늘 발급된 필수 일일 미션 3종(WALK, FEEDING, ROBOT_PLAY)이 없다면 자동 초기화 생성합니다. (방어벽 1 반영)
     * 
     * @param email 사용자 이메일
     * @return 오늘의 미션 응답 DTO 리스트
     */
    @Transactional
    public List<MissionResponse> getTodayMissions(String email) {
        // 로그인된 유저 이메일로 매핑된 기기를 조회하여 존재 여부를 확인합니다.
        Device device = deviceRepository.findByUserEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("등록된 기기가 존재하지 않습니다."));

        // 오늘 자정 시각(00:00:00)을 구하여 기준 시간으로 설정합니다.
        LocalDateTime todayStart = LocalDateTime.now().with(java.time.LocalTime.MIN);

        // 필수 일일 미션 3종 목록
        String[] requiredTypes = {"WALK", "FEEDING", "ROBOT_PLAY"};

        // 동시성 호출 시 중복 생성을 막기 위해 개별 타입별로 exists 체크를 먼저 수행합니다.
        for (String type : requiredTypes) {
            boolean exists = missionRepository.existsByDeviceDeviceIdAndTypeAndIssuedAtAfter(
                    device.getDeviceId(), type, todayStart);
            
            if (!exists) {
                // 트랜잭션 격리수준 및 동시 다발 호출에 대비해 더블 체크를 위한 동기화 블록 적용
                synchronized (this) {
                    boolean doubleCheckExists = missionRepository.existsByDeviceDeviceIdAndTypeAndIssuedAtAfter(
                            device.getDeviceId(), type, todayStart);
                    if (!doubleCheckExists) {
                        Mission dailyMission = Mission.builder()
                                .device(device)
                                .type(type)
                                .issuedAt(LocalDateTime.now())
                                .status(MissionStatus.PENDING)
                                .isCompleted(false)
                                .build();
                        missionRepository.save(dailyMission);
                        log.info("기기 [{}] - 오늘자 일일 미션 [{}] 자동 생성 완료", device.getDeviceId(), type);
                    }
                }
            }
        }

        // 오늘 자정 이후 기기에 발급된 최종 미션 목록을 다시 조회합니다.
        List<Mission> missions = missionRepository.findAllByDeviceDeviceIdAndIssuedAtAfter(device.getDeviceId(), todayStart);

        // 엔티티 목록을 DTO 목록으로 변환하여 반환합니다.
        return missions.stream()
                .map(MissionResponse::from)
                .collect(java.util.stream.Collectors.toList());
    }
}
