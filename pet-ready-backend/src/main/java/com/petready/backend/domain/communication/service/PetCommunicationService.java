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
    private final com.petready.backend.domain.score.repository.ScoreEventRepository scoreEventRepository;

    /**
     * 기기(ESP32)로부터 상태 로그를 수신하여 저장하고, 기기의 실시간 상태를 업데이트합니다.
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

        // 가상 배터리 감쇄 시뮬레이션 계산
        int battery = 100;
        java.util.Optional<PetStatusLog> lastLogOpt = logRepository.findFirstByDeviceDeviceIdOrderByRecordedAtDesc(device.getDeviceId());
        if (lastLogOpt.isPresent()) {
            PetStatusLog lastLog = lastLogOpt.get();
            int lastBattery = lastLog.getBatteryLevel() != null ? lastLog.getBatteryLevel() : 100;
            long minutesDiff = java.time.Duration.between(lastLog.getRecordedAt(), LocalDateTime.now()).toMinutes();
            // 분당 0.1% 차감 (10분당 1% 차감)
            double depletion = minutesDiff * 0.1;
            battery = Math.max(0, (int) (lastBattery - depletion));
        }

        // 2. 로그 저장
        PetStatusLog statusLog = PetStatusLog.builder()
                .device(device)
                .batteryLevel(battery)
                .headTouch(request.getHeadTouch())
                .backTouch1(request.getBackTouch1())
                .backTouch2(request.getBackTouch2())
                .recordedAt(LocalDateTime.now())
                .build();
        logRepository.save(statusLog);

        // 3. 기기 실시간 상태 업데이트 (Heartbeat)
        updateDeviceHeartbeat(device);

        // 4. 상태 분석 로직 (배터리 알림 등 포함)
        return analyzePetStatus(request, device, statusLog);
    }

    /**
     * 기기가 수행해야 할 아직 확인되지 않은 최신 명령을 조회합니다.
     * 
     * @param deviceId 기기 ID
     * @return 명령 정보 (명령이 없으면 hasCommand = false)
     */
    public CommandResponse getPendingCommand(String deviceId) {
        // 해당 기기의 acked_at이 null인 가장 오래된 미수신 명령 1건 조회 (FIFO)
        List<Command> pendingCommands = commandRepository.findAllByDeviceDeviceIdOrderByCreatedAtAsc(deviceId);
        
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
     * 3종 터치 센서 데이터를 기반으로 반려견의 상태를 분석하는 로직입니다.
     */
    private PetStatusResponse analyzePetStatus(PetStatusRequest request, Device device, PetStatusLog statusLog) {
        int virtualBattery = statusLog.getBatteryLevel() != null ? statusLog.getBatteryLevel() : 100;
        boolean isHungry = virtualBattery <= 20;
        String mood = "NORMAL";
        String healthStatus = "GOOD";
        String message = "반려견이 안정적인 상태입니다.";

        // 1. 머리 터치 센서 -> 짖음 달래기 정산
        if (Boolean.TRUE.equals(request.getHeadTouch())) {
            mood = "HAPPY";
            message = "반려견이 머리를 쓰다듬자 기분이 좋아졌습니다!";
            
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
                    delta = -10;
                }
                
                mission.complete(LocalDateTime.now(), responseTimeSec);
                missionRepository.save(mission);
                
                scoreService.processScoreEvent(device.getDeviceId(), "BARK_RESPOND", delta, "짖음 미션 응답 완료 (" + responseTimeSec + "초)");
                
                // 스피커 짖음 소리를 끄기 위해 SOUND_STOP 명령 큐 추가
                Command stopCommand = Command.builder()
                        .device(device)
                        .command("SOUND_STOP")
                        .durationSec(0)
                        .build();
                commandRepository.save(stopCommand);
                
                log.info("기기 [{}] 짖음 미션 완료: SOUND_STOP 명령 추가", device.getDeviceId());
            }
        }
        
        // 2. 등 터치 센서 -> 쓰다듬기 교감 점수 부여
        if (Boolean.TRUE.equals(request.getBackTouch1()) || Boolean.TRUE.equals(request.getBackTouch2())) {
            mood = "HAPPY";
            message = "반려견이 등을 쓰다듬어 주자 아주 기뻐합니다!";
            
            // 오늘 부여된 교감 보너스 횟수가 5회 미만인 경우에만 획득
            java.time.LocalDateTime startOfToday = java.time.LocalDate.now().atStartOfDay();
            long count = scoreEventRepository.countByDeviceDeviceIdAndEventTypeAndOccurredAtAfter(
                    device.getDeviceId(), "PET_BONDING", startOfToday);
            if (count < 5) {
                scoreService.processScoreEvent(device.getDeviceId(), "PET_BONDING", 1, "반려견 쓰다듬기 및 교감 보너스 (+1점)");
                log.info("기기 [{}] 교감 점수 획득 (+1점). 오늘 횟수: {}/5", device.getDeviceId(), count + 1);
            }
        }

        // 3. 배고픔 미션 발동 (가상 배터리 감쇄에 따른 발동)
        if (isHungry) {
            message = "배터리가 부족하여 반려견의 배고픔 수치가 올라갔습니다.";
            
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

                // 스피커 앓는 소리 WHINE_START 명령 추가
                Command whineCommand = Command.builder()
                        .device(device)
                        .command("WHINE_START")
                        .durationSec(30)
                        .build();
                commandRepository.save(whineCommand);

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

        // 4. LED 상태 결정
        boolean hasActiveMission = missionRepository.findFirstByDeviceDeviceIdAndTypeAndIsCompletedFalseOrderByIssuedAtDesc(
                device.getDeviceId(), NotificationType.BARKING.name()).isPresent()
                || missionRepository.findFirstByDeviceDeviceIdAndTypeAndIsCompletedFalseOrderByIssuedAtDesc(
                device.getDeviceId(), NotificationType.FEEDING.name()).isPresent();
        
        String ledColor = (hasActiveMission || isHungry) ? "RED" : "GREEN";

        return PetStatusResponse.builder()
                .isHungry(isHungry)
                .mood(mood)
                .healthStatus(healthStatus)
                .analysisMessage(message)
                .ledColor(ledColor)
                .build();
    }

    /**
     * 사용자가 앱 터치로 밥 주기를 수행했을 때 처리합니다. (크로스 체크 미션 락 연동)
     */
    @Transactional
    public void feedPetByApp(String deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("등록되지 않은 기기입니다: " + deviceId));

        device.updateAppFeedClicked(true);
        deviceRepository.save(device);
        log.info("기기 [{}] - 앱 피딩 터치 활성화 (appFeedClicked=true)", deviceId);

        checkAndProcessFeedingLock(device);
    }

    /**
     * 젯슨나노 비전 동기화 수신 시 처리합니다. (크로스 체크 미션 락 연동)
     */
    @Transactional
    public void syncVisionByJetson(String deviceId, boolean bowlDetected) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("등록되지 않은 기기입니다: " + deviceId));

        device.updateBowlDetected(bowlDetected);
        deviceRepository.save(device);
        log.info("기기 [{}] - 젯슨나노 비전 동기화 수신 (bowlDetected={})", deviceId, bowlDetected);

        checkAndProcessFeedingLock(device);
    }

    /**
     * '앱 터치-실물 밥그릇' 크로스 체크 미션 락 해제 및 피딩 미션 완료를 판별합니다.
     */
    @Transactional
    public void checkAndProcessFeedingLock(Device device) {
        log.info("기기 [{}] 크로스 체크 판별 - appFeedClicked: {}, bowlDetected: {}",
                device.getDeviceId(), device.getAppFeedClicked(), device.getBowlDetected());

        // 두 조건이 모두 충족되었을 때만 락 해제 및 밥 주기 완료 처리
        if (Boolean.TRUE.equals(device.getAppFeedClicked()) && Boolean.TRUE.equals(device.getBowlDetected())) {
            String deviceId = device.getDeviceId();
            
            java.util.Optional<com.petready.backend.domain.mission.entity.Mission> activeFeedingOpt = 
                    missionRepository.findFirstByDeviceDeviceIdAndTypeAndIsCompletedFalseOrderByIssuedAtDesc(
                            deviceId, NotificationType.FEEDING.name());
            
            if (activeFeedingOpt.isPresent()) {
                com.petready.backend.domain.mission.entity.Mission mission = activeFeedingOpt.get();
                long responseTimeSec = java.time.Duration.between(mission.getIssuedAt(), LocalDateTime.now()).getSeconds();
                
                // 30분(1800초) 이내에 밥을 주면 +3점 가점
                if (responseTimeSec <= 1800) {
                    scoreService.processScoreEvent(deviceId, "FEEDING_COMPLETE", 3, "밥그릇 비전 크로스 체크 성공 피딩 미션 성공 (+3점)");
                    log.info("기기 [{}] 크로스 체크 피딩 미션 성공 (+3점)", deviceId);
                } else {
                    log.info("기기 [{}] 크로스 체크 피딩 미션 완료 (30분 초과로 점수 없음)", deviceId);
                }
                
                mission.complete(LocalDateTime.now(), responseTimeSec);
                missionRepository.save(mission);
            }

            // 스피커 앓는 소리를 끄기 위해 SOUND_STOP 명령 큐 추가
            Command stopCommand = Command.builder()
                    .device(device)
                    .command("SOUND_STOP")
                    .durationSec(0)
                    .build();
            commandRepository.save(stopCommand);

            // 가상 배터리를 100% 충전 상태로 새 로그 등록 (배터리 시뮬레이션 리셋)
            PetStatusLog statusLog = PetStatusLog.builder()
                    .device(device)
                    .batteryLevel(100)
                    .headTouch(false)
                    .backTouch1(false)
                    .backTouch2(false)
                    .recordedAt(LocalDateTime.now())
                    .build();
            logRepository.save(statusLog);

            // 미션 락 해제 상태 초기화
            device.resetFeedingLock();
            deviceRepository.save(device);
            
            log.info("기기 [{}] 크로스 체크 락 해제 완료: 가상 배터리 100% 충전 및 SOUND_STOP 명령 발송", deviceId);
        }
    }

    /**
     * 아두이노(ESP32) 짖음 이벤트 수신 시 처리합니다. (하드웨어 주도형 짖음 이벤트)
     */
    @Transactional
    public void receiveBarkEvent(String deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("등록되지 않은 기기입니다: " + deviceId));

        log.info("하드웨어 주도형 짖음 이벤트 수신 - 기기 [{}]", deviceId);

        // 중복 미션 생성을 막기 위해 현재 완료되지 않은 짖음 미션이 있는지 먼저 체크
        java.util.Optional<com.petready.backend.domain.mission.entity.Mission> activeBarkingOpt = 
                missionRepository.findFirstByDeviceDeviceIdAndTypeAndIsCompletedFalseOrderByIssuedAtDesc(
                        deviceId, NotificationType.BARKING.name());

        if (activeBarkingOpt.isEmpty()) {
            // 1. 미션 기록 생성 (발급 시각 기록)
            com.petready.backend.domain.mission.entity.Mission mission = com.petready.backend.domain.mission.entity.Mission.builder()
                    .device(device)
                    .type(NotificationType.BARKING.name())
                    .issuedAt(LocalDateTime.now())
                    .isCompleted(false)
                    .build();
            missionRepository.save(mission);

            // 2. 사용자 앱으로 푸시 즉시 토스 (BK-05)
            if (device.getUser() != null && device.getUser().getFcmToken() != null) {
                String petName = device.getPetName() != null ? device.getPetName() : "반려견";
                fcmNotificationService.sendNotification(
                        device.getUser().getFcmToken(),
                        "펫-레디 알림",
                        petName + "가 짖고 있어요! 얼른 달래줘야 해요",
                        NotificationType.BARKING
                );
            }
            log.info("기기 [{}] 하드웨어 짖음 수신 성공: 짖음 미션 생성 및 앱 FCM 알림 즉시 발송", deviceId);
        } else {
            log.info("기기 [{}] 이미 진행 중인 짖음 미션이 존재하므로 짖음 수집 수신을 스킵합니다.", deviceId);
        }
    }
}
