package com.petready.backend.domain.mission.service;

import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.mission.dto.TrainingGestureRequest;
import com.petready.backend.domain.mission.dto.TrainingRewardResponse;
import com.petready.backend.domain.mission.entity.TrainingLog;
import com.petready.backend.domain.mission.repository.TrainingLogRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 가상 반려견 훈련 판정(SUCCESS, CONFUSED, SAD) 및 아두이노 LCD/LED 동기화 락아웃을 처리하는 핵심 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingService {

    private final TrainingLogRepository trainingLogRepository;
    private final DeviceRepository deviceRepository;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // 기기별 최근 감지된 제스쳐 캐시 (60초 타임아웃 적용)
    private final Map<String, LastGestureInfo> gestureCache = new ConcurrentHashMap<>();

    // 아두이노 폴링 연출용 3초 우선순위 락아웃 캐시
    private final Map<String, TrainingResultInfo> resultLockCache = new ConcurrentHashMap<>();

    @Getter
    @AllArgsConstructor
    public static class LastGestureInfo {
        private final String gestureType;
        private final double confidence;
        private final long timestamp;
        private final ScheduledFuture<?> timeoutTask;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class TrainingResultInfo {
        private final String status;
        private final String lcdCommand;
        private final String lcdLine1;
        private final String lcdLine2;
        private final String ledColor;
        private final long occurredAt;
    }

    /**
     * 젯슨나노 비전 YOLOv8로부터 제스쳐 감지 신호를 수신해 캐싱하고 60초 타이머를 구동합니다.
     */
    public void handleGesture(TrainingGestureRequest request) {
        String deviceId = request.getDeviceId();
        String gesture = request.getGestureType();
        double conf = request.getConfidence() != null ? request.getConfidence() : 0.0;

        log.info("[훈련 제스쳐 감지] Device: {}, Gesture: {}, Confidence: {}", deviceId, gesture, conf);

        // 1. 기존 타이머가 존재한다면 취소하여 중복 SAD 적재 방지
        LastGestureInfo oldGesture = gestureCache.remove(deviceId);
        if (oldGesture != null && oldGesture.getTimeoutTask() != null) {
            oldGesture.getTimeoutTask().cancel(false);
        }

        // 2. 60초(1분) 뒤 보상이 없으면 SAD(방치/실패) 처리하는 일회성 비동기 태스크 스케줄링
        ScheduledFuture<?> timeoutTask = scheduler.schedule(() -> {
            try {
                handleTrainingTimeout(deviceId);
            } catch (Exception e) {
                log.error("[훈련 타임아웃 처리 중 에러] Device: {}, Error: {}", deviceId, e.getMessage());
            }
        }, 60, TimeUnit.SECONDS);

        // 3. 캐시에 최신 제스쳐 버퍼 저장
        gestureCache.put(deviceId, new LastGestureInfo(gesture, conf, System.currentTimeMillis(), timeoutTask));
    }

    /**
     * 아두이노 보상 입력 수신 시 최근 제스쳐 캐시를 확인해 SUCCESS / CONFUSED 판정을 내리고 DB에 저장합니다.
     */
    @Transactional
    public TrainingRewardResponse handleReward(String deviceId) {
        log.info("[훈련 보상 입력 수신] Device: {}", deviceId);

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("등록되지 않은 기기입니다. ID: " + deviceId));

        LastGestureInfo cached = gestureCache.remove(deviceId);
        
        String status;
        String lcdCommand;
        String line1;
        String line2;
        String ledColor;

        // 60초 이내에 유효한 제스쳐 감지 기록이 존재하는 경우 -> SUCCESS
        if (cached != null) {
            // 5초 이내에 타이머 취소
            if (cached.getTimeoutTask() != null) {
                cached.getTimeoutTask().cancel(false);
            }

            status = "SUCCESS";
            lcdCommand = "LCD_HAPPY";
            line1 = "[ TRAINING OK! ]";
            line2 = "DOG:  ( ^ _ ^ )/";
            ledColor = "GREEN";

            log.info("[훈련 판정: SUCCESS] Device: {}, Gesture: {}", deviceId, cached.getGestureType());

            // 훈련 로그 적재
            TrainingLog logEntity = TrainingLog.builder()
                    .device(device)
                    .gestureType(cached.getGestureType())
                    .status(status)
                    .build();
            trainingLogRepository.save(logEntity);

        } else {
            // 제스쳐 감지 이력이 없는데 보상을 준 경우 -> CONFUSED (뇌정지/혼란)
            status = "CONFUSED";
            lcdCommand = "LCD_CONFUSED";
            line1 = "WHAT?  ??? ";
            line2 = "DOG:  ( ? . ? )";
            ledColor = "RED";

            log.info("[훈련 판정: CONFUSED] Device: {} (제스쳐 없음)", deviceId);

            // 훈련 로그 적재
            TrainingLog logEntity = TrainingLog.builder()
                    .device(device)
                    .gestureType("UNKNOWN")
                    .status(status)
                    .build();
            trainingLogRepository.save(logEntity);
        }

        // 3초간 아두이노 폴링 시 최우선 노출되도록 3초 연출 락아웃 캐시에 적재
        TrainingResultInfo resultInfo = TrainingResultInfo.builder()
                .status(status)
                .lcdCommand(lcdCommand)
                .lcdLine1(line1)
                .lcdLine2(line2)
                .ledColor(ledColor)
                .occurredAt(System.currentTimeMillis())
                .build();
        resultLockCache.put(deviceId, resultInfo);

        return TrainingRewardResponse.builder()
                .status(status)
                .lcdCommand(lcdCommand)
                .lcdTextLine1(line1)
                .lcdTextLine2(line2)
                .ledColor(ledColor)
                .build();
    }

    /**
     * 제스쳐 감지 후 60초간 아무 보상이 없어 방치되었을 때 호출되어 SAD 로그를 적재하는 타임아웃 메서드입니다.
     */
    @Transactional
    public void handleTrainingTimeout(String deviceId) {
        LastGestureInfo cached = gestureCache.remove(deviceId);
        if (cached == null) {
            return; // 이미 보상 입력으로 소거된 경우 스킵
        }

        log.info("[훈련 판정: SAD] Device: {}, 60초간 보상 없음으로 인한 방임 상태 기록", deviceId);

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("등록되지 않은 기기입니다. ID: " + deviceId));

        String status = "SAD";
        String lcdCommand = "LCD_SAD";
        String line1 = "[ TRAINING FAIL]";
        String line2 = "DOG:  ( T _ T ) ";
        String ledColor = "RED";

        // 훈련 실패(SAD) 로그 저장
        TrainingLog logEntity = TrainingLog.builder()
                .device(device)
                .gestureType(cached.getGestureType())
                .status(status)
                .build();
        trainingLogRepository.save(logEntity);

        // 실패 연출도 아두이노가 가져가도록 3초 연출 락아웃 캐시에 적재
        TrainingResultInfo resultInfo = TrainingResultInfo.builder()
                .status(status)
                .lcdCommand(lcdCommand)
                .lcdLine1(line1)
                .lcdLine2(line2)
                .ledColor(ledColor)
                .occurredAt(System.currentTimeMillis())
                .build();
        resultLockCache.put(deviceId, resultInfo);
    }

    /**
     * 아두이노 폴링 시 3초 이내의 최근 훈련 결과 연출 패킷이 있는지 판별합니다. (3초 락아웃 룰)
     */
    public TrainingResultInfo getPendingTrainingResult(String deviceId) {
        TrainingResultInfo info = resultLockCache.get(deviceId);
        if (info == null) {
            return null;
        }

        // 3초(3000ms) 경과 여부 판단
        if (System.currentTimeMillis() - info.getOccurredAt() > 3000) {
            resultLockCache.remove(deviceId); // 만료 시 캐시 소거
            return null;
        }

        return info;
    }
}
