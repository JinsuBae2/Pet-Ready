package com.petready.backend.domain.device.batch;

import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.entity.RoutineStatus;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.log.entity.PetStatusLog;
import com.petready.backend.domain.log.repository.PetStatusLogRepository;
import com.petready.backend.domain.mission.entity.TrainingLog;
import com.petready.backend.domain.mission.repository.MissionRepository;
import com.petready.backend.domain.mission.repository.TrainingLogRepository;
import com.petready.backend.domain.walk.entity.Walk;
import com.petready.backend.domain.walk.repository.WalkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * 24시간 반려견 상태(RoutineStatus)를 실시간 모니터링하고 판정하는 라이프 사이클 스케줄러입니다.
 * 매 1분마다 가상 반려견의 지표(마지막 급여 시간, 산책 이행률, 배터리 잔량, 미션 상태)를 스캔하여 갱신합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PetLifeCycleScheduler {

    private final DeviceRepository deviceRepository;
    private final PetStatusLogRepository petStatusLogRepository;
    private final MissionRepository missionRepository;
    private final TrainingLogRepository trainingLogRepository;
    private final WalkRepository walkRepository;

    /**
     * 매 1분마다 모든 기기의 상태를 평가하여 RoutineStatus를 업데이트합니다.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void evaluateAllPetsStatus() {
        log.info("[양육 라이프 사이클 스케줄러] 24시간 평상시 반려견 상태 스캔 시작");
        List<Device> devices = deviceRepository.findAll();

        for (Device device : devices) {
            try {
                RoutineStatus oldStatus = device.getRoutineStatus();
                RoutineStatus newStatus = determineRoutineStatus(device);
                
                if (oldStatus != newStatus) {
                    device.updateRoutineStatus(newStatus);
                    deviceRepository.save(device);
                    log.info("[상태 변화 감지] 기기: {}, 기존 상태: {} -> 변경 상태: {}", 
                            device.getDeviceId(), oldStatus, newStatus);
                }
            } catch (Exception e) {
                log.error("[반려견 상태 스캔 중 오류] 기기 ID: {}, 에러: {}", device.getDeviceId(), e.getMessage(), e);
            }
        }
        log.info("[양육 라이프 사이클 스케줄러] 상태 스캔 완료");
    }

    /**
     * 특정 기기의 현재 상태를 규칙에 따라 판정합니다.
     */
    private RoutineStatus determineRoutineStatus(Device device) {
        String deviceId = device.getDeviceId();
        LocalDateTime now = LocalDateTime.now();

        // 1. SLEEPING: 밤 23시 ~ 아침 7시 사이
        LocalTime currentTime = now.toLocalTime();
        if (currentTime.isAfter(LocalTime.of(23, 0)) || currentTime.isBefore(LocalTime.of(7, 0))) {
            return RoutineStatus.SLEEPING;
        }

        // 2. SICK: 가상 배터리가 20% 이하이거나 sickCount가 존재하는 경우
        int batteryLevel = 100;
        Optional<PetStatusLog> latestLogOpt = petStatusLogRepository.findFirstByDeviceDeviceIdOrderByRecordedAtDesc(deviceId);
        if (latestLogOpt.isPresent()) {
            batteryLevel = latestLogOpt.get().getBatteryLevel() != null ? latestLogOpt.get().getBatteryLevel() : 100;
        }
        if (batteryLevel <= 20 || (device.getSickCount() != null && device.getSickCount() > 0)) {
            return RoutineStatus.SICK;
        }

        // 3. BARKING: 완료되지 않은 짖음 미션이 존재하는 경우
        boolean hasActiveBarking = missionRepository.findFirstByDeviceDeviceIdAndTypeAndIsCompletedFalseOrderByIssuedAtDesc(
                deviceId, "BARKING").isPresent();
        if (hasActiveBarking) {
            return RoutineStatus.BARKING;
        }

        // 4. HUNGRY: 마지막 급여 시각이 없거나 4시간을 초과하여 경과한 경우
        LocalDateTime lastFeedTime = device.getLastFeedTime();
        if (lastFeedTime == null || now.isAfter(lastFeedTime.plusHours(4))) {
            return RoutineStatus.HUNGRY;
        }

        // 5. HAPPY (훈련 성공 30분 이내이거나 오늘 산책 목표 100% 완료 시)
        // (A) 훈련 성공 30분 이내 여부
        boolean isRecentlyTrained = false;
        Optional<TrainingLog> latestSuccessTrainOpt = trainingLogRepository
                .findFirstByDeviceDeviceIdAndStatusOrderByCreatedAtDesc(deviceId, "SUCCESS");
        if (latestSuccessTrainOpt.isPresent()) {
            LocalDateTime trainTime = latestSuccessTrainOpt.get().getCreatedAt();
            if (now.isBefore(trainTime.plusMinutes(30))) {
                isRecentlyTrained = true;
            }
        }

        // (B) 오늘 산책 100% 완료 여부
        boolean isWalkGoalAchieved = false;
        List<Walk> todayWalks = walkRepository.findAllByDeviceDeviceIdAndStartedAtAfter(deviceId, LocalDate.now().atStartOfDay());
        BigDecimal totalWalkDistance = todayWalks.stream()
                .map(Walk::getDistanceKm)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double goalKm = device.getWalkGoalKm() != null ? device.getWalkGoalKm() : 2.0;
        if (totalWalkDistance.doubleValue() >= goalKm) {
            isWalkGoalAchieved = true;
        }

        if (isRecentlyTrained || isWalkGoalAchieved) {
            return RoutineStatus.HAPPY;
        }

        // 6. BORED: 최근 2시간 이내에 어떠한 상호작용(급여, 터치교감, 산책, 훈련성공)도 없을 때
        boolean hasRecentInteraction = false;
        
        // 6-1. 최근 2시간 이내 급여
        if (lastFeedTime != null && now.isBefore(lastFeedTime.plusHours(2))) {
            hasRecentInteraction = true;
        }
        // 6-2. 최근 2시간 이내 터치교감
        if (!hasRecentInteraction) {
            Optional<PetStatusLog> lastTouchLogOpt = petStatusLogRepository.findLastTouchLog(deviceId);
            if (lastTouchLogOpt.isPresent()) {
                LocalDateTime touchTime = lastTouchLogOpt.get().getRecordedAt();
                if (now.isBefore(touchTime.plusHours(2))) {
                    hasRecentInteraction = true;
                }
            }
        }
        // 6-3. 최근 2시간 이내 산책 시작
        if (!hasRecentInteraction) {
            List<Walk> recentWalks = walkRepository.findAllByDeviceDeviceIdAndStartedAtAfter(deviceId, now.minusHours(2));
            if (!recentWalks.isEmpty()) {
                hasRecentInteraction = true;
            }
        }
        // 6-4. 최근 2시간 이내 훈련 성공
        if (!hasRecentInteraction && latestSuccessTrainOpt.isPresent()) {
            LocalDateTime trainTime = latestSuccessTrainOpt.get().getCreatedAt();
            if (now.isBefore(trainTime.plusHours(2))) {
                hasRecentInteraction = true;
            }
        }

        if (!hasRecentInteraction) {
            return RoutineStatus.BORED;
        }

        // 7. HAPPY: 기본 상태
        return RoutineStatus.HAPPY;
    }
}
