package com.petready.backend.domain.mission.service;

import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.mission.dto.MissionResponse;
import com.petready.backend.domain.mission.entity.Mission;
import com.petready.backend.domain.mission.repository.MissionRepository;
import com.petready.backend.domain.medical.entity.MedicalFeeCache;
import com.petready.backend.domain.medical.repository.MedicalFeeCacheRepository;
import com.petready.backend.domain.report.entity.PetReport;
import com.petready.backend.domain.report.repository.PetReportRepository;
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

    /**
     * 사용자가 미션을 완료(응답) 처리합니다.
     * 
     * @param missionId 완료할 미션의 식별자
     */
    @Transactional
    public void completeMission(Long missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 미션입니다."));

        if (Boolean.TRUE.equals(mission.getIsCompleted())) {
            throw new IllegalStateException("이미 완료된 미션입니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        long responseTimeSec = ChronoUnit.SECONDS.between(mission.getIssuedAt(), now);

        mission.complete(now, responseTimeSec);
        missionRepository.save(mission);
        log.info("미션 [{}] 완료 처리 완료. 응답 시간: {}초", missionId, responseTimeSec);

        // [고도화] 완료된 미션의 타입이 "MEDICAL"인 경우, 진료비 목록에서 랜덤하게 진료비 영수증 청구 (방어벽 2 반영)
        if ("MEDICAL".equalsIgnoreCase(mission.getType())) {
            // 해당 유저의 PetReport 조회
            String email = mission.getDevice().getUser().getEmail();
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
