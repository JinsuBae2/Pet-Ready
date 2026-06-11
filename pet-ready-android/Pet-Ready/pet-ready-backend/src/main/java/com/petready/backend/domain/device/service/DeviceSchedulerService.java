package com.petready.backend.domain.device.service;

import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.log.entity.PetStatusLog;
import com.petready.backend.domain.log.repository.PetStatusLogRepository;
import com.petready.backend.domain.score.service.ScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 기기의 상태 모니터링 및 방전 패널티 등을 관리하는 스케줄러 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceSchedulerService {

    private final DeviceRepository deviceRepository;
    private final PetStatusLogRepository logRepository;
    private final ScoreService scoreService;

    /**
     * 1분마다 실행되어 하트비트가 5분 이상 갱신되지 않은 기기를 오프라인 처리하고,
     * 방전된 상태라면 패널티를 부여합니다. (BK-03)
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkOfflineAndDischargePenalty() {
        LocalDateTime fiveMinsAgo = LocalDateTime.now().minusMinutes(5);
        List<Device> devices = deviceRepository.findAll();

        for (Device device : devices) {
            // 하트비트가 아예 없거나 5분 이상 경과한 경우
            if (device.getLastHeartbeat() == null || device.getLastHeartbeat().isBefore(fiveMinsAgo)) {
                
                // 오프라인 상태로 변경
                if (Boolean.TRUE.equals(device.getIsOnline())) {
                    device.setOnlineStatus(false);
                    deviceRepository.save(device);
                    log.info("기기 [{}] - 5분 이상 하트비트 부재로 오프라인 처리", device.getDeviceId());
                }

                // 최근 로그를 확인하여 방전(배터리 0%)인지 판별
                Optional<PetStatusLog> lastLogOpt = logRepository.findFirstByDeviceDeviceIdOrderByRecordedAtDesc(device.getDeviceId());
                if (lastLogOpt.isPresent()) {
                    PetStatusLog lastLog = lastLogOpt.get();
                    if (lastLog.getBatteryLevel() != null && lastLog.getBatteryLevel() <= 0) {
                        
                        // 이미 이 방전 이벤트에 대해 패널티를 부여했는지 확인 (12시간 내 중복 부여 방지 등)
                        // 여기서는 가장 최근 패널티 부여 시각이 5분 전보다 더 예전일 경우에만 부여 (즉 한 번 오프라인+0% 될 때 1회만)
                        if (device.getLastDischargePenaltyAt() == null || device.getLastDischargePenaltyAt().isBefore(fiveMinsAgo)) {
                            device.incrementSickCount();
                            deviceRepository.save(device);
                            
                            scoreService.processScoreEvent(device.getDeviceId(), "DISCHARGE_PENALTY", -15, "5분 이상 오프라인 및 배터리 방전 패널티");
                            log.warn("기기 [{}] - 방전 패널티 부여 (-15점). 현재 굶김 횟수: {}", device.getDeviceId(), device.getSickCount());
                        }
                    }
                }
            }
        }
    }
}
