package com.petready.backend.domain.mission;

import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.mission.dto.MissionResponse;
import com.petready.backend.domain.mission.entity.Mission;
import com.petready.backend.domain.mission.repository.MissionRepository;
import com.petready.backend.domain.mission.service.MissionService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * MissionService의 오늘의 미션 조회 관련 비즈니스 로직을 검증하는 단위 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
public class MissionServiceTest {

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private MissionService missionService;

    @Test
    @DisplayName("성공: 등록된 기기가 있고 오늘 날짜의 미션들이 존재할 때 목록이 정상 반환된다")
    void getTodayMissions_Success() {
        // given
        String email = "test@example.com";
        String deviceId = "DEVICE_A";

        Device mockDevice = Device.builder()
                .deviceId(deviceId)
                .petName("바둑이")
                .build();

        Mission mockMission = Mission.builder()
                .id(1L)
                .device(mockDevice)
                .type("FEEDING_TIME")
                .issuedAt(LocalDateTime.now())
                .isCompleted(false)
                .build();

        // Mock 객체 동작 정의
        when(deviceRepository.findByUserEmail(email)).thenReturn(Optional.of(mockDevice));
        when(missionRepository.findAllByDeviceDeviceIdAndIssuedAtAfter(eq(deviceId), any(LocalDateTime.class)))
                .thenReturn(List.of(mockMission));

        // when
        List<MissionResponse> responses = missionService.getTodayMissions(email);

        // then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getId());
        assertEquals("FEEDING_TIME", responses.get(0).getType());
        assertFalse(responses.get(0).getIsCompleted());
    }

    @Test
    @DisplayName("실패: 유저 메일로 등록된 기기가 존재하지 않을 때 EntityNotFoundException이 발생한다")
    void getTodayMissions_NoDevice_ThrowsException() {
        // given
        String email = "no_device@example.com";
        when(deviceRepository.findByUserEmail(email)).thenReturn(Optional.empty());

        // when & then
        assertThrows(EntityNotFoundException.class, () -> {
            missionService.getTodayMissions(email);
        });
    }
}
