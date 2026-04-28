package com.petready.backend.domain.mission.service;

import com.petready.backend.domain.command.entity.Command;
import com.petready.backend.domain.command.repository.CommandRepository;
import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.mission.entity.Mission;
import com.petready.backend.domain.mission.repository.MissionRepository;
import com.petready.backend.domain.notification.service.FcmNotificationService;
import com.petready.backend.global.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * 랜덤 미션 발동을 관리하는 스케줄러 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MissionSchedulerService {

    private final DeviceRepository deviceRepository;
    private final MissionRepository missionRepository;
    private final CommandRepository commandRepository;
    private final FcmNotificationService fcmNotificationService;
    private final TaskScheduler taskScheduler;

    /**
     * 매일 자정(00:00)에 실행되어 당일의 '새벽 짖음' 미션 시각을 계산하고 스케줄링합니다.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void scheduleDailyMissions() {
        log.info("일일 새벽 짖음 미션 스케줄링이 시작되었습니다.");
        
        // 실제로는 활성화된(사용 중인) 기기 목록만 조회하는 등 최적화 필요
        List<Device> devices = deviceRepository.findAll();
        Random random = new Random();
        
        // 요일별 가중치 (주말인 경우 발동 빈도나 횟수를 줄이는 로직이 들어갈 수 있음)
        int maxMissionsPerDay = 2; // 매일 1~2회 발동 (주말은 1회 고정 등 로직 추가 가능)

        for (Device device : devices) {
            int numberOfMissions = random.nextInt(maxMissionsPerDay) + 1; // 1~2회
            
            for (int i = 0; i < numberOfMissions; i++) {
                // 00:00(0) ~ 05:00(18000초) 사이의 랜덤 초를 구함
                int randomSeconds = random.nextInt(5 * 60 * 60);
                Instant missionInstant = Instant.now().plusSeconds(randomSeconds);
                
                // 해당 시각에 단발성 작업 작동하도록 스케줄링 등록
                taskScheduler.schedule(() -> triggerBarkingMission(device.getDeviceId()), missionInstant);
                log.info("기기 [{}] - 미션 예정 시간: {}초 후 발동", device.getDeviceId(), randomSeconds);
            }
        }
    }

    /**
     * 스케줄된 시각에 실제 미션을 발동하는 로직입니다.
     */
    @Transactional
    public void triggerBarkingMission(String deviceId) {
        // 별도 스레드에서 실행되므로 영속성 컨텍스트를 위해 새로 조회
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) return;

        log.info("새벽 짖음 미션 발동! - 기기 [{}]", deviceId);

        // 1. 미션 기록 생성 (발급 시각 기록)
        Mission mission = Mission.builder()
                .device(device)
                .type(NotificationType.BARKING.name())
                .issuedAt(LocalDateTime.now())
                .isCompleted(false)
                .build();
        missionRepository.save(mission);

        // 2. Command 큐에 BARK_START 30초 명령 등록
        Command command = Command.builder()
                .device(device)
                .command("BARK_START")
                .durationSec(30)
                .build();
        commandRepository.save(command);

        // 3. 사용자 앱으로 푸시 알림 발송
        if (device.getUser() != null && device.getUser().getFcmToken() != null) {
            String petName = device.getPetName() != null ? device.getPetName() : "반려견";
            fcmNotificationService.sendNotification(
                    device.getUser().getFcmToken(),
                    "펫-레디 알림",
                    petName + "가 짖고 있어요! 얼른 달래줘야 해요",
                    NotificationType.BARKING
            );
        }
    }
}
