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
 * 아두이노 기기(ESP32) 및 젯슨나노 비전 모듈과의 모든 실시간 상태 처리,
 * 센서 이벤트 분석, 명령어 생성 및 미션 크로스 체크 처리를 담당하는 핵심 서비스 클래스입니다.
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
     * 로봇 기기로부터 실시간 센서 상태 로그를 수신하고 시뮬레이터 가상 배터리 감소를 계산하여 적재합니다.
     * 
     * @param request 아두이노 기기 센서 데이터
     * @return 펫의 분석된 기분 및 LED 상태
     */
    @Transactional
    public PetStatusResponse receiveStatus(PetStatusRequest request) {
        // 1. 등록된 기기인지 DB 조회 수행 (없으면 예외 발생)
        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new EntityNotFoundException("등록되지 않은 기기입니다: " + request.getDeviceId()));

        // 2. 가상 배터리 감소 시뮬레이션 로직 작동 (이전 상태 로그와의 시간차 기준 분당 0.1%씩 차감)
        int battery = 100;
        java.util.Optional<PetStatusLog> lastLogOpt = logRepository.findFirstByDeviceDeviceIdOrderByRecordedAtDesc(device.getDeviceId());
        if (lastLogOpt.isPresent()) {
            PetStatusLog lastLog = lastLogOpt.get();
            int lastBattery = lastLog.getBatteryLevel() != null ? lastLog.getBatteryLevel() : 100;
            long minutesDiff = java.time.Duration.between(lastLog.getRecordedAt(), LocalDateTime.now()).toMinutes();
            // 10분에 1% 비율로 차감 처리
            double depletion = minutesDiff * 0.1;
            battery = Math.max(0, (int) (lastBattery - depletion));
        }

        // 3. 수신한 상태 및 배터리 수치를 로그 엔티티로 변환해 영속화
        PetStatusLog statusLog = PetStatusLog.builder()
                .device(device)
                .batteryLevel(battery)
                .headTouch(request.getHeadTouch())
                .backTouch1(request.getBackTouch1())
                .backTouch2(request.getBackTouch2())
                .recordedAt(LocalDateTime.now())
                .build();
        logRepository.save(statusLog);

        // 4. 기기의 하트비트 타임스탬프 갱신으로 온라인 상태 관리
        updateDeviceHeartbeat(device);

        // 5. 터치 및 미션 상태에 따른 분석 응답 조립
        return analyzePetStatus(request, device, statusLog);
    }

    /**
     * 기기에서 처리 대기 중인(아직 Ack되지 않은) 명령어 목록 중 가장 오래된 1건을 조회합니다.
     */
    public CommandResponse getPendingCommand(String deviceId) {
        // 해당 기기의 명령어 중 생성 일자 오름차순(FIFO)으로 대기 커맨드 로드
        List<Command> pendingCommands = commandRepository.findAllByDeviceDeviceIdOrderByCreatedAtAsc(deviceId);
        
        return pendingCommands.stream()
                .filter(c -> c.getAckedAt() == null) // 아직 수신 완료되지 않은 건만 필터링
                .findFirst()
                .map(command -> CommandResponse.builder()
                        .hasCommand(true)
                        .commandId(command.getId())
                        .command(command.getCommand())
                        .durationSec(command.getDurationSec())
                        .nextPollIntervalSec(5) // 명령어 수신 인지 중에는 폴링 간격을 5초로 빠르게 단축
                        .build())
                .orElse(CommandResponse.builder()
                        .hasCommand(false)
                        .nextPollIntervalSec(30) // 대기 명령어 없을 시 기본 30초 주기로 폴링
                        .build());
    }

    /**
     * 기기가 명령 실행을 성공하고 Ack를 보냈을 때 처리합니다.
     */
    @Transactional
    public void acknowledgeCommand(Long commandId) {
        Command command = commandRepository.findById(commandId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 명령입니다: " + commandId));
        
        updateCommandAck(command);
    }

    /**
     * 기기 하트비트 갱신 내부 헬퍼 메소드
     */
    private void updateDeviceHeartbeat(Device device) {
        device.updateHeartbeat();
        log.info("기기 [{}] 하트비트 업데이트 완료: Online", device.getDeviceId());
    }

    /**
     * 커맨드 수신 확인 처리 내부 헬퍼 메소드
     */
    private void updateCommandAck(Command command) {
        command.acknowledge();
        log.info("명령어 ID [{}] 수신 완료 확인 처리", command.getId());
    }

    /**
     * 로봇의 센서 값들을 교차 대조하여 감정, 건강 상태 및 미션 성공 여부를 판별합니다.
     */
    private PetStatusResponse analyzePetStatus(PetStatusRequest request, Device device, PetStatusLog statusLog) {
        int virtualBattery = statusLog.getBatteryLevel() != null ? statusLog.getBatteryLevel() : 100;
        boolean isHungry = virtualBattery <= 20; // 가상 배터리가 20% 이하면 배고픔 미션 자동 발동 조건 충족
        String mood = "NORMAL";
        String healthStatus = "GOOD";
        String message = "반려견이 안정적인 상태입니다.";

        // ----------------------------------------------------
        // [로직 1] 머리 터치 센서 -> 하드웨어 주도형 짖음 미션 해결
        // ----------------------------------------------------
        if (Boolean.TRUE.equals(request.getHeadTouch())) {
            mood = "HAPPY";
            message = "반려견이 머리를 쓰다듬자 기분이 좋아졌습니다!";
            
            // 현재 완료되지 않은 진행 중인 BARKING 미션을 로드합니다.
            java.util.Optional<com.petready.backend.domain.mission.entity.Mission> activeBarkingOpt = 
                    missionRepository.findFirstByDeviceDeviceIdAndTypeAndIsCompletedFalseOrderByIssuedAtDesc(
                            device.getDeviceId(), NotificationType.BARKING.name());
            
            if (activeBarkingOpt.isPresent()) {
                com.petready.backend.domain.mission.entity.Mission mission = activeBarkingOpt.get();
                long responseTimeSec = java.time.Duration.between(mission.getIssuedAt(), LocalDateTime.now()).getSeconds();
                
                // 대응 반응속도 정밀 측정 구간별 점수 가감 처리 (BK-08)
                int delta = 0;
                if (responseTimeSec <= 300) {
                    delta = 5;       // 5분 이내 초고속 완료
                } else if (responseTimeSec <= 900) {
                    delta = 2;       // 15분 이내 보통 완료
                } else if (responseTimeSec <= 1800) {
                    delta = -3;      // 30분 이내 지연 대응 감점
                } else {
                    delta = -10;     // 30분 초과 장기 방치 감점
                }
                
                // 미션 완료 상태 영속화
                mission.complete(LocalDateTime.now(), responseTimeSec);
                missionRepository.save(mission);
                
                // 점수 이벤트 로깅 및 실시간 점수 반영
                scoreService.processScoreEvent(device.getDeviceId(), "BARK_RESPOND", delta, "짖음 미션 응답 완료 (" + responseTimeSec + "초)");
                
                // 하드웨어 오디오 재생을 끄기 위한 SOUND_STOP 명령어 큐 추가 (BK-04)
                Command stopCommand = Command.builder()
                        .device(device)
                        .command("SOUND_STOP")
                        .durationSec(0)
                        .build();
                commandRepository.save(stopCommand);
                
                log.info("기기 [{}] 짖음 미션 해제: SOUND_STOP 명령 추가 완료", device.getDeviceId());
            }
        }
        
        // ----------------------------------------------------
        // [로직 2] 등 터치 센서 -> 쓰다듬기 교감 보너스 적재
        // ----------------------------------------------------
        if (Boolean.TRUE.equals(request.getBackTouch1()) || Boolean.TRUE.equals(request.getBackTouch2())) {
            mood = "HAPPY";
            message = "반려견이 등을 쓰다듬어 주자 아주 기뻐합니다!";
            
            // 어뷰징 방지를 위해 오늘 획득한 교감 보너스가 5회 미만인 경우에만 획득 허용
            java.time.LocalDateTime startOfToday = java.time.LocalDate.now().atStartOfDay();
            long count = scoreEventRepository.countByDeviceDeviceIdAndEventTypeAndOccurredAtAfter(
                    device.getDeviceId(), "PET_BONDING", startOfToday);
            if (count < 5) {
                scoreService.processScoreEvent(device.getDeviceId(), "PET_BONDING", 1, "반려견 쓰다듬기 및 교감 보너스 (+1점)");
                log.info("기기 [{}] 교감 보너스 점수 적재 (+1점). 오늘 횟수: {}/5", device.getDeviceId(), count + 1);
            }
        }

        // ----------------------------------------------------
        // [로직 3] 가상 배터리 방전 임계치 진입 시 배고픔 미션 자동 생성
        // ----------------------------------------------------
        if (isHungry) {
            message = "배터리가 부족하여 반려견의 배고픔 수치가 올라갔습니다.";
            
            java.util.Optional<com.petready.backend.domain.mission.entity.Mission> activeFeedingOpt = 
                    missionRepository.findFirstByDeviceDeviceIdAndTypeAndIsCompletedFalseOrderByIssuedAtDesc(
                            device.getDeviceId(), NotificationType.FEEDING.name());
            
            if (activeFeedingOpt.isEmpty()) {
                // 신규 FEEDING 미션 발행
                com.petready.backend.domain.mission.entity.Mission feedingMission = com.petready.backend.domain.mission.entity.Mission.builder()
                        .device(device)
                        .type(NotificationType.FEEDING.name())
                        .issuedAt(LocalDateTime.now())
                        .isCompleted(false)
                        .build();
                missionRepository.save(feedingMission);

                // 스피커 앓는 소리 WHINE_START 커맨드 추가를 통한 하드웨어 청각 피드백 발동
                Command whineCommand = Command.builder()
                        .device(device)
                        .command("WHINE_START")
                        .durationSec(30)
                        .build();
                commandRepository.save(whineCommand);

                // 모바일 앱 푸시 토스 전송
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

        // 4. 활성화된 미션 존재 여부에 따라 LED 색상 결정 (RED: 미션 진행중 / GREEN: 대기상태)
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
     * 사용자가 앱 터치로 밥 주기를 수행했을 때 호출하여 플래그를 세팅하고 크로스 체크를 수행합니다. (BK-03)
     */
    @Transactional
    public void feedPetByApp(String deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("등록되지 않은 기기입니다: " + deviceId));

        // 앱 터치 상태 활성화
        device.updateAppFeedClicked(true);
        deviceRepository.save(device);
        log.info("기기 [{}] - 모바일 앱 피딩 터치 상태 기록 성공 (appFeedClicked=true)", deviceId);

        // '앱 터치 - 실물 밥그릇' 크로스 체크 락 해제 검증을 호출합니다.
        checkAndProcessFeedingLock(device);
    }

    /**
     * 젯슨나노 AI가 실물 밥그릇 감지 비전 신호를 송신했을 때 호출하여 플래그를 동기화하고 크로스 체크를 수행합니다. (BK-03)
     */
    @Transactional
    public void syncVisionByJetson(String deviceId, boolean bowlDetected) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("등록되지 않은 기기입니다: " + deviceId));

        // 실물 밥그릇 비전 감지 상태 동기화
        device.updateBowlDetected(bowlDetected);
        deviceRepository.save(device);
        log.info("기기 [{}] - 젯슨나노 비전 동기화 수신 성공 (bowlDetected={})", deviceId, bowlDetected);

        // '앱 터치 - 실물 밥그릇' 크로스 체크 락 해제 검증을 호출합니다.
        checkAndProcessFeedingLock(device);
    }

    /**
     * 앱 터치와 비전 밥그릇 인식 두 조건이 모두 완료(Lock 해제)되었을 때에만 피딩 미션을 완수 및 배터리를 100% 충전시킵니다. (BK-03)
     */
    @Transactional
    public void checkAndProcessFeedingLock(Device device) {
        log.info("기기 [{}] 피딩 미션 락 해제 검증 - 앱 터치 여부: {}, 비전 감지 여부: {}",
                device.getDeviceId(), device.getAppFeedClicked(), device.getBowlDetected());

        // 가상 피딩 편법 원천 차단을 위해 두 조건이 모두 참(true)인지 검증합니다.
        if (Boolean.TRUE.equals(device.getAppFeedClicked()) && Boolean.TRUE.equals(device.getBowlDetected())) {
            String deviceId = device.getDeviceId();
            
            // 현재 완료되지 않은 FEEDING 미션을 불러옵니다.
            java.util.Optional<com.petready.backend.domain.mission.entity.Mission> activeFeedingOpt = 
                    missionRepository.findFirstByDeviceDeviceIdAndTypeAndIsCompletedFalseOrderByIssuedAtDesc(
                            deviceId, NotificationType.FEEDING.name());
            
            if (activeFeedingOpt.isPresent()) {
                com.petready.backend.domain.mission.entity.Mission mission = activeFeedingOpt.get();
                long responseTimeSec = java.time.Duration.between(mission.getIssuedAt(), LocalDateTime.now()).getSeconds();
                
                // 30분(1800초) 이내에 급여 성공 시 가점 반영
                if (responseTimeSec <= 1800) {
                    scoreService.processScoreEvent(deviceId, "FEEDING_COMPLETE", 3, "밥그릇 비전 크로스 체크 성공 피딩 미션 성공 (+3점)");
                    log.info("기기 [{}] 크로스 체크 밥 주기 성공 가점 적용 (+3점)", deviceId);
                } else {
                    log.info("기기 [{}] 크로스 체크 밥 주기 완료 (30분 초과로 가점 없음)", deviceId);
                }
                
                // 미션 성공 처리 영속화
                mission.complete(LocalDateTime.now(), responseTimeSec);
                missionRepository.save(mission);
            }

            // 기기 앓는 소리 출력을 즉시 소거하기 위해 SOUND_STOP 명령어 주입
            Command stopCommand = Command.builder()
                    .device(device)
                    .command("SOUND_STOP")
                    .durationSec(0)
                    .build();
            commandRepository.save(stopCommand);

            // 가상 배터리를 100% 완충 상태 로그로 초기화
            PetStatusLog statusLog = PetStatusLog.builder()
                    .device(device)
                    .batteryLevel(100)
                    .headTouch(false)
                    .backTouch1(false)
                    .backTouch2(false)
                    .recordedAt(LocalDateTime.now())
                    .build();
            logRepository.save(statusLog);

            // 크로스 체크 미션 락 해제 상태 초기화
            device.resetFeedingLock();
            deviceRepository.save(device);
            
            log.info("기기 [{}] 최종 크로스 체크 미션 락 해제 및 배터리 충전 시뮬레이션 리셋 성공!", deviceId);
        }
    }

    /**
     * 아두이노(ESP32)가 자체 오디오 출력으로 짖자마자 짖음 이벤트를 수신하여 미션 타이머를 개시합니다. (하드웨어 주도형 짖음 - BK-04/BK-05)
     */
    @Transactional
    public void receiveBarkEvent(String deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("등록되지 않은 기기입니다: " + deviceId));

        log.info("하드웨어 주도형 짖음 이벤트 감지 수신 - 기기 [{}]", deviceId);

        // 중복 미션 생성을 막기 위해 현재 완료되지 않은 진행 중인 BARKING 미션 존재 여부 조회
        java.util.Optional<com.petready.backend.domain.mission.entity.Mission> activeBarkingOpt = 
                missionRepository.findFirstByDeviceDeviceIdAndTypeAndIsCompletedFalseOrderByIssuedAtDesc(
                        deviceId, NotificationType.BARKING.name());

        if (activeBarkingOpt.isEmpty()) {
            // 1. 짖기 시작한 현재 시각 기준으로 BARKING 미션 신규 영속화 (타이머 개시)
            com.petready.backend.domain.mission.entity.Mission mission = com.petready.backend.domain.mission.entity.Mission.builder()
                    .device(device)
                    .type(NotificationType.BARKING.name())
                    .issuedAt(LocalDateTime.now())
                    .isCompleted(false)
                    .build();
            missionRepository.save(mission);

            // 2. 사용자 앱으로 돌발 짖음 경고 FCM 푸시 즉시 전송
            if (device.getUser() != null && device.getUser().getFcmToken() != null) {
                String petName = device.getPetName() != null ? device.getPetName() : "반려견";
                fcmNotificationService.sendNotification(
                        device.getUser().getFcmToken(),
                        "펫-레디 알림",
                        petName + "가 짖고 있어요! 얼른 달래줘야 해요",
                        NotificationType.BARKING
                );
            }
            log.info("기기 [{}] 하드웨어 주도형 짖음 미션 발동 및 사용자 FCM 푸시 통스 완료", deviceId);
        } else {
            log.info("기기 [{}] 이미 활성화된 짖음 미션이 진행 중이므로 수신 처리를 건너뜁니다.", deviceId);
        }
    }
}
