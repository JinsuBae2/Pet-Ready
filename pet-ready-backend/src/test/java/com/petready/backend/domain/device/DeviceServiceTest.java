package com.petready.backend.domain.device;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petready.backend.domain.device.dto.DeviceRegisterRequest;
import com.petready.backend.domain.device.dto.MyDeviceResponse;
import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.device.service.DeviceService;
import com.petready.backend.domain.report.entity.PetReport;
import com.petready.backend.domain.report.repository.PetReportRepository;
import com.petready.backend.domain.score.entity.RealTimeScore;
import com.petready.backend.domain.score.repository.RealTimeScoreRepository;
import com.petready.backend.domain.user.entity.User;
import com.petready.backend.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DeviceService의 초기비용 150,420원 정합성 검증 및 기기 정보 조회 단위 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
public class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RealTimeScoreRepository scoreRepository;

    @Mock
    private PetReportRepository reportRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private DeviceService deviceService;

    @Test
    @DisplayName("성공: 기기 최초 등록 시 초기 비용(150,420원)과 입양비/필수 백신비 영수증 내역이 정상적으로 생성 및 Persist 된다")
    void registerDevice_InitialFees_Success() {
        // given
        String email = "owner@example.com";
        String deviceId = "DOG_03";
        DeviceRegisterRequest request = new DeviceRegisterRequest(deviceId);

        User mockUser = User.builder()
                .email(email)
                .nickname("진수")
                .build();

        Device mockDevice = Device.builder()
                .deviceId(deviceId)
                .user(mockUser)
                .petName("반려견")
                .isOnline(false)
                .build();

        when(deviceRepository.existsById(deviceId)).thenReturn(false);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(deviceRepository.save(any(Device.class))).thenReturn(mockDevice);

        // PetReport 저장을 캡처하기 위한 Captor 설정
        ArgumentCaptor<PetReport> reportCaptor = ArgumentCaptor.forClass(PetReport.class);

        // when
        deviceService.registerDevice(request, email);

        // then
        // 1. 초기 점수 100점 저장 확인
        verify(scoreRepository, times(1)).save(any(RealTimeScore.class));

        // 2. 초기 리포트 저장 및 150,420원 정합성 검증
        verify(reportRepository, times(1)).save(reportCaptor.capture());
        PetReport savedReport = reportCaptor.getValue();
        
        assertNotNull(savedReport);
        assertEquals(mockUser, savedReport.getUser());
        assertEquals(150420L, savedReport.getTotalReceiptAmount()); // 초기 비용 150,420원 확인
        
        String detailsJson = savedReport.getReceiptDetailsJson();
        assertNotNull(detailsJson);
        assertTrue(detailsJson.contains("가상 유기견 입양비"));
        assertTrue(detailsJson.contains("개 종합백신"));
        assertTrue(detailsJson.contains("광견병 백신"));
    }

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
