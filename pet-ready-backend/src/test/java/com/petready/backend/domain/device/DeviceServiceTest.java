package com.petready.backend.domain.device;

import com.petready.backend.domain.device.dto.MyDeviceResponse;
import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.device.service.DeviceService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * DeviceService의 내 기기 정보 조회 기능의 비즈니스 로직을 검증하는 단위 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
public class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private DeviceService deviceService;

    @Test
    @DisplayName("성공: 현재 사용자 이메일로 이미 등록된 기기가 존재하면 기기 정보를 정상적으로 조회하여 반환한다")
    void getMyDevice_Success() {
        // given
        String email = "owner@example.com";
        String deviceId = "DOG_03";
        String petName = "초코";

        Device mockDevice = Device.builder()
                .deviceId(deviceId)
                .petName(petName)
                .walkGoalKm(2.5)
                .isOnline(true)
                .build();

        // Repository Mocking
        when(deviceRepository.findByUserEmail(email)).thenReturn(Optional.of(mockDevice));

        // when
        MyDeviceResponse response = deviceService.getMyDevice(email);

        // then
        assertNotNull(response);
        assertEquals(deviceId, response.getDeviceId());
        assertEquals(petName, response.getPetName());
        assertEquals(2.5, response.getWalkGoalKm());
        assertTrue(response.getIsOnline());
    }

    @Test
    @DisplayName("실패: 현재 사용자 이메일로 등록된 기기가 존재하지 않을 시 EntityNotFoundException 예외를 던진다")
    void getMyDevice_NoDevice_ThrowsException() {
        // given
        String email = "nodevice@example.com";
        when(deviceRepository.findByUserEmail(email)).thenReturn(Optional.empty());

        // when & then
        assertThrows(EntityNotFoundException.class, () -> {
            deviceService.getMyDevice(email);
        });
    }
}
