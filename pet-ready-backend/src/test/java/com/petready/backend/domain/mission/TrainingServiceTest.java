package com.petready.backend.domain.mission;

import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.mission.dto.TrainingGestureRequest;
import com.petready.backend.domain.mission.dto.TrainingRewardResponse;
import com.petready.backend.domain.mission.entity.TrainingLog;
import com.petready.backend.domain.mission.repository.TrainingLogRepository;
import com.petready.backend.domain.mission.service.TrainingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TrainingService의 가상 훈련 SUCCESS, CONFUSED, SAD 판단 시퀀스를 검증하는 단위 테스트입니다.
 */
@ExtendWith(MockitoExtension.class)
public class TrainingServiceTest {

    @Mock
    private TrainingLogRepository trainingLogRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private TrainingService trainingService;

    @Test
    @DisplayName("성공: 제스쳐 감지 후 60초 이내에 보상 버튼 입력 시 SUCCESS 판정 및 훈련 로그가 저장된다")
    void handleReward_Success() {
        // given
        String deviceId = "DOG_01";
        Device device = Device.builder().deviceId(deviceId).petName("바둑이").build();
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        // 1. 제스쳐 감지 입력
        TrainingGestureRequest gestureRequest = TrainingGestureRequest.builder()
                .deviceId(deviceId)
                .gestureType("SIT")
                .confidence(0.95)
                .build();
        trainingService.handleGesture(gestureRequest);

        // when: 60초 이내에 보상 처리
        TrainingRewardResponse response = trainingService.handleReward(deviceId);

        // then
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("LCD_HAPPY", response.getLcdCommand());
        assertEquals("GREEN", response.getLedColor());

        // TrainingLog save 호출 및 데이터 정합성 검증
        ArgumentCaptor<TrainingLog> logCaptor = ArgumentCaptor.forClass(TrainingLog.class);
        verify(trainingLogRepository, times(1)).save(logCaptor.capture());
        TrainingLog savedLog = logCaptor.getValue();
        assertEquals("SIT", savedLog.getGestureType());
        assertEquals("SUCCESS", savedLog.getStatus());
    }

    @Test
    @DisplayName("뇌정지: 제스쳐 감지 이력 없이 보상 버튼 클릭 시 CONFUSED 판정 및 UNKNOWN 제스쳐 로그가 저장된다")
    void handleReward_Confused() {
        // given
        String deviceId = "DOG_01";
        Device device = Device.builder().deviceId(deviceId).petName("바둑이").build();
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        // when: 제스쳐 감지 없이 보상 처리
        TrainingRewardResponse response = trainingService.handleReward(deviceId);

        // then
        assertNotNull(response);
        assertEquals("CONFUSED", response.getStatus());
        assertEquals("LCD_CONFUSED", response.getLcdCommand());
        assertEquals("RED", response.getLedColor());

        // TrainingLog save 호출 검증
        ArgumentCaptor<TrainingLog> logCaptor = ArgumentCaptor.forClass(TrainingLog.class);
        verify(trainingLogRepository, times(1)).save(logCaptor.capture());
        TrainingLog savedLog = logCaptor.getValue();
        assertEquals("UNKNOWN", savedLog.getGestureType());
        assertEquals("CONFUSED", savedLog.getStatus());
    }

    @Test
    @DisplayName("방치: 제스쳐 감지 후 보상 없이 타임아웃 발생 시 SAD 판정 및 훈련 실패 로그가 저장된다")
    void handleTrainingTimeout_Sad() {
        // given
        String deviceId = "DOG_01";
        Device device = Device.builder().deviceId(deviceId).petName("바둑이").build();
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        // 제스쳐 감지 입력
        TrainingGestureRequest gestureRequest = TrainingGestureRequest.builder()
                .deviceId(deviceId)
                .gestureType("STAY")
                .confidence(0.9)
                .build();
        trainingService.handleGesture(gestureRequest);

        // when: 강제 타임아웃 호출
        trainingService.handleTrainingTimeout(deviceId);

        // then
        ArgumentCaptor<TrainingLog> logCaptor = ArgumentCaptor.forClass(TrainingLog.class);
        verify(trainingLogRepository, times(1)).save(logCaptor.capture());
        TrainingLog savedLog = logCaptor.getValue();
        assertEquals("STAY", savedLog.getGestureType());
        assertEquals("SAD", savedLog.getStatus());

        // 3초 락아웃 캐시에 적재되었는지 확인
        assertNotNull(trainingService.getPendingTrainingResult(deviceId));
    }
}
