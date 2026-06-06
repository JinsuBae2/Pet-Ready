package com.petready.backend.domain.communication;

import com.petready.backend.domain.communication.service.PetCommunicationService;
import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.mission.entity.Mission;
import com.petready.backend.domain.mission.repository.MissionRepository;
import com.petready.backend.domain.score.service.ScoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PetCommunicationServiceTest {

    @Mock private DeviceRepository deviceRepository;
    @Mock private MissionRepository missionRepository;
    @Mock private ScoreService scoreService;
    @Mock private com.petready.backend.domain.command.repository.CommandRepository commandRepository;
    @Mock private com.petready.backend.domain.log.repository.PetStatusLogRepository logRepository;

    @InjectMocks private PetCommunicationService communicationService;

    @Test
    void testCrossCheckFeedingLock() {
        Device mockDevice = Device.builder()
                .deviceId("DOG_03")
                .appFeedClicked(false)
                .bowlDetected(false)
                .build();

        when(deviceRepository.findById("DOG_03")).thenReturn(Optional.of(mockDevice));

        // 1. App Feed Clicked 만 전송했을 때 (락이 풀리지 않음)
        communicationService.feedPetByApp("DOG_03");
        assertTrue(mockDevice.getAppFeedClicked());
        assertFalse(mockDevice.getBowlDetected());
        verify(scoreService, never()).processScoreEvent(any(), any(), anyInt(), any());

        // 2. 비전 밥그릇 인식까지 들어왔을 때 (두 조건 충족되어 락 해제 및 완료 처리)
        when(missionRepository.findFirstByDeviceDeviceIdAndTypeAndIsCompletedFalseOrderByIssuedAtDesc(
                eq("DOG_03"), eq("FEEDING"))).thenReturn(Optional.of(Mission.builder().issuedAt(LocalDateTime.now().minusMinutes(5)).build()));

        communicationService.syncVisionByJetson("DOG_03", true);
        
        // 최종적으로 두 플래그는 리셋되어야 하고, 점수 가점 및 완료 명령들이 나가야 함
        assertFalse(mockDevice.getAppFeedClicked());
        assertFalse(mockDevice.getBowlDetected());
        verify(scoreService).processScoreEvent(eq("DOG_03"), eq("FEEDING_COMPLETE"), eq(3), anyString());
        verify(commandRepository).save(any());
        verify(logRepository).save(any());
        
        System.out.println("✅ 앱 터치 - 실물 밥그릇 크로스 체크 미션 락 연동 테스트 성공!");
    }
}
