package com.petready.backend.domain.communication.service;

import com.petready.backend.domain.command.entity.Command;
import com.petready.backend.domain.command.repository.CommandRepository;
import com.petready.backend.domain.communication.dto.CommandResponse;
import com.petready.backend.domain.communication.dto.PetStatusRequest;
import com.petready.backend.domain.communication.dto.PetStatusResponse;
import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.log.entity.PetStatusLog;
import com.petready.backend.domain.log.repository.PetStatusLogRepository;
import com.petready.backend.domain.notification.service.FcmNotificationService;
import com.petready.backend.global.enums.NotificationType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 기기(ESP32)와의 통신 및 데이터 분석을 담당하는 서비스 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetCommunicationService {

    private final DeviceRepository deviceRepository;
    private final PetStatusLogRepository logRepository;
    private final CommandRepository commandRepository;
    private final FcmNotificationService fcmNotificationService;
    private final com.petready.backend.domain.score.service.ScoreService scoreService;
    private final com.petready.backend.domain.mission.repository.MissionRepository missionRepository;

    /**
     * 기기로부 상태 로그를 수신하여 저장하고, 기기의 실시간 상태를 업데이트합니다.
     * 수신된 데이터를 바탕으로 반려견의 상태를 분석합니다.
     * 
     * @param request 상태 수신 데이터
     * @return 상태 분석 결과
     */
    @Transactional
    public PetStatusResponse receiveStatus(PetStatusRequest request) {
        // 1. 기기 존재 여부 확인
        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new EntityNotFoundException("등록되지 않은 기기입니다: " + request.getDeviceId()));

        // 2. 로그 저장
        PetStatusLog statusLog = PetStatusLog.builder()
                .device(device)
                .batteryLevel(request.getBatteryLevel())
                .isCharging(request.getIsCharging())
                .touchActive(request.getTouchActive())
                .pressureValue(request.getPressureValue())
                .recordedAt(LocalDateTime.now())
                .build();
        logRepository.save(statusLog);

        // 3. 기기 실시간 상태 업데이트 (Heartbeat)
        // Device의 필드를 업데이트하기 위해 수동으로 업데이트 (Entity가 Immutable하지 않은 경우)
        // 여기서는 간단히 리포지토리를 통해 업데이트 로직을 태우거나, 필드 접근용 메서드 필요
        // (참고: 현 엔티티 설계상 Setter가 없으므로 Reflection이나 별도 메서드 필요)
        updateDeviceHeartbeat(device);

        // 4. 상태 분석 로직 (배터리 알림 등 포함)
        return analyzePetStatus(request, device);
    }

    /**
     * 기기가 수행해야 할 아직 확인되지 않은 최신 명령을 조회합니다.
     * 
     * @param deviceId 기기 ID
     * @return 명령 정보 (명령이 없으면 hasCommand = false)
     */
    public CommandResponse getPendingCommand(String deviceId) {
        // 해당 기기의 acked_at이 null인 가장 최신 명령 1건 조회
        List<Command> pendingCommands = commandRepository.findAllByDeviceDeviceIdOrderByCreatedAtDesc(deviceId);
        
        return pendingCommands.stream()
                .filter(c -> c.getAckedAt() == null)
                .findFirst()
                .map(command -> CommandResponse.builder()
                        .hasCommand(true)
                        .commandId(command.getId())
                        .command(command.getCommand())
                        .durationSec(command.getDurationSec())
                        .nextPollIntervalSec(5) // 명령 수행/인지 중에는 5초 주기로 단축
                        .build())
                .orElse(CommandResponse.builder()
                        .hasCommand(false)
                        .nextPollIntervalSec(30) // 기본 폴링 주기를 30초로 설정
                        .build());
    }

    /**
     * 기기에서 명령 수신을 확인(Ack)했을 때 처리합니다.
     * 
     * @param commandId 명령 ID
     */
    @Transactional
    public void acknowledgeCommand(Long commandId) {
        Command command = commandRepository.findById(commandId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 명령입니다: " + commandId));
        
        // acked_at 업데이트 (현재 엔티티 구조상 리플렉션이나 메서드 필요)
        // 실무에서는 @Setter나 update용 메서드를 엔티티에 추가함
        // 여기서는 개념적 구현을 위해 로직 기술
        updateCommandAck(command);
    }

    private void updateDeviceHeartbeat(Device device) {
        device.updateHeartbeat();
        log.info("기기 [{}] 하트비트 업데이트: Online", device.getDeviceId());
    }

    private void updateCommandAck(Command command) {
        command.acknowledge();
        log.info("명령 [{}] 수신 확인 처리", command.getId());
    }

    /**
     * 배터리, 터치, 압력 센서 데이터를 기반으로 반려견의 상태를 분석하는 로직입니다.
     */
    private PetStatusResponse analyzePetStatus(PetStatusRequest request, Device device) {
        boolean isHungry = request.getBatteryLevel() != null && request.getBatteryLevel() <= 20;
        String mood = "NORMAL";
        String healthStatus = "GOOD";
        String message = "반려견이 안정적인 상태입니다.";

        // BK-05: 짖음 달래기 정산 (터치 센서 연동)
        if (Boolean.TRUE.equals(request.getTouchActive())) {
            mood = "HAPPY";
            message = "반려견이 주인의 터치를 느껴 기분이 좋습니다!";
            
            // 활성화된 BARKING 미션이 있는지 확인하고 점수 정산
            java.util.Optional<com.petready.backend.domain.mission.entity.Mission> activeBarkingOpt = 
                    missionRepository.findFirstByDeviceDeviceIdAndTypeAndIsCompletedFalseOrderByIssuedAtDesc(
                            device.getDeviceId(), NotificationType.BARKING.name());
            
            if (activeBarkingOpt.isPresent()) {
                com.petready.backend.domain.mission.entity.Mission mission = activeBarkingOpt.get();
                long responseTimeSec = java.time.Duration.between(mission.getIssuedAt(), LocalDateTime.now()).getSeconds();
                
                int delta = 0;
                if (responseTimeSec <= 300) {
                    delta = 5;
                } else if (responseTimeSec <= 900) {
                    delta = 2;
                } else if (responseTimeSec <= 1800) {
                    delta = -3;
                } else {
                    delta = -10; // 30분 초과 시점엔 스케줄러가 잡기 전일 수도 있으므로 여기서도 예외 처리
                }
                
                mission.complete(LocalDateTime.now(), responseTimeSec);
                missionRepository.save(mission);
                
                scoreService.processScoreEvent(device.getDeviceId(), "BARK_RESPOND", delta, "짖음 미션 응답 완료 (" + responseTimeSec + "초)");
                log.info("기기 [{}] 짖음 미션 수동 완료: 응답 시간 {}초, 점수 변화 {}", device.getDeviceId(), responseTimeSec, delta);
            }
        } else if (request.getPressureValue() != null && request.getPressureValue() > 50.0) {
            mood = "STRESSED";
            healthStatus = "WARNING";
            message = "강한 압력이 감지되었습니다. 반려견의 상태를 확인해주세요.";
        }

        // BK-03: 배고픔 및 밥 주기 미션 처리
        if (isHungry) {
            message = "배터리가 부족하여 반려견의 배고픔 수치가 올라갔습니다.";
            
            // 활성화된 FEEDING 미션이 없다면 새로 생성하고 알림 발송
            java.util.Optional<com.petready.backend.domain.mission.entity.Mission> activeFeedingOpt = 
                    missionRepository.findFirstByDeviceDeviceIdAndTypeAndIsCompletedFalseOrderByIssuedAtDesc(
                            device.getDeviceId(), NotificationType.FEEDING.name());
            
            if (activeFeedingOpt.isEmpty()) {
                com.petready.backend.domain.mission.entity.Mission feedingMission = com.petready.backend.domain.mission.entity.Mission.builder()
                        .device(device)
                        .type(NotificationType.FEEDING.name())
                        .issuedAt(LocalDateTime.now())
                        .isCompleted(false)
                        .build();
                missionRepository.save(feedingMission);

                if (device.getUser() != null && device.getUser().getFcmToken() != null) {
                    String petName = device.getPetName() != null ? device.getPetName() : "반려견";
                    fcmNotificationService.sendNotification(
                            device.getUser().getFcmToken(),
                            "펫-레디 알림",
                            petName + " 배가 고파요!",
                            NotificationType.FEEDING
                    );
                }
            }
        }

        // 충전 패드에 올려 충전 시작 시 (밥 주기 완료 인식)
        if (Boolean.TRUE.equals(request.getIsCharging())) {
            java.util.Optional<com.petready.backend.domain.mission.entity.Mission> activeFeedingOpt = 
                    missionRepository.findFirstByDeviceDeviceIdAndTypeAndIsCompletedFalseOrderByIssuedAtDesc(
                            device.getDeviceId(), NotificationType.FEEDING.name());
            
            if (activeFeedingOpt.isPresent()) {
                com.petready.backend.domain.mission.entity.Mission mission = activeFeedingOpt.get();
                long responseTimeSec = java.time.Duration.between(mission.getIssuedAt(), LocalDateTime.now()).getSeconds();
                
                // 30분(1800초) 이내에 밥을 주면 +3점 가점
                if (responseTimeSec <= 1800) {
                    scoreService.processScoreEvent(device.getDeviceId(), "FEEDING_COMPLETE", 3, "30분 이내 밥주기 미션 성공");
                    log.info("기기 [{}] 밥주기 미션 성공 (+3점)", device.getDeviceId());
                } else {
                    // 30분이 넘었다면 점수는 주지 않고 미션만 종료 처리 (방전 전까지 밥을 주긴 함)
                    log.info("기기 [{}] 밥주기 미션 완료 (30분 초과로 점수 없음)", device.getDeviceId());
                }
                
                mission.complete(LocalDateTime.now(), responseTimeSec);
                missionRepository.save(mission);
            }
        }

        return PetStatusResponse.builder()
                .isHungry(isHungry)
                .mood(mood)
                .healthStatus(healthStatus)
                .analysisMessage(message)
                .build();
    }
}
