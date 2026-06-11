package com.petready.backend.domain.walk;

import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.score.service.ScoreService;
import com.petready.backend.domain.user.entity.User;
import com.petready.backend.domain.walk.dto.WalkEndRequest;
import com.petready.backend.domain.walk.repository.WalkRepository;
import com.petready.backend.domain.walk.service.WalkSchedulerService;
import com.petready.backend.domain.walk.service.WalkService;
import com.petready.backend.domain.report.service.ScoringService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WalkIntegrationTest {

    @Mock private WalkRepository walkRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private ScoringService scoringService;
    @Mock private ScoreService scoreService;

    @InjectMocks private WalkService walkService;
    @InjectMocks private WalkSchedulerService walkSchedulerService;

    @Test
    void testWalkSettlementAndMidnightPenalty() {
        // 1. User & Device Mock
        User mockUser = User.builder().build();
        ReflectionTestUtils.setField(mockUser, "id", 1L);
        ReflectionTestUtils.setField(mockUser, "email", "test@petready.com");

        Device mockDeviceA = Device.builder().build();
        ReflectionTestUtils.setField(mockDeviceA, "deviceId", "DEVICE_A");
        ReflectionTestUtils.setField(mockDeviceA, "walkGoalKm", 2.0);
        ReflectionTestUtils.setField(mockDeviceA, "user", mockUser);

        Device mockDeviceB = Device.builder().build();
        ReflectionTestUtils.setField(mockDeviceB, "deviceId", "DEVICE_B");
        ReflectionTestUtils.setField(mockDeviceB, "walkGoalKm", 1.0);
        ReflectionTestUtils.setField(mockDeviceB, "user", mockUser);

        when(deviceRepository.findById("DEVICE_A")).thenReturn(Optional.of(mockDeviceA));

        // 2. Device A 산책 종료 찌르기 (2.0km 걸음 -> 달성률 100%)
        WalkEndRequest walkReq = new WalkEndRequest();
        ReflectionTestUtils.setField(walkReq, "deviceId", "DEVICE_A");
        ReflectionTestUtils.setField(walkReq, "distanceKm", new java.math.BigDecimal("2.0"));
        ReflectionTestUtils.setField(walkReq, "durationSec", 3600L);
        
        walkService.endWalk(walkReq, "test@petready.com");

        // 검증 1: 100% 달성이므로 WALK_FULL, +5점 호출되었는지 검증
        verify(scoreService).processScoreEvent(eq("DEVICE_A"), eq("WALK_FULL"), eq(5), any(String.class));
        System.out.println("✅ [DEVICE_A] 산책 달성률 100% 반영 검증 성공!");

        // 3. 자정 배치 강제 호출 (Device B 패널티 확인)
        when(deviceRepository.findAll()).thenReturn(List.of(mockDeviceA, mockDeviceB));
        
        // Device A는 1회 산책, Device B는 0회 산책이라 가정
        when(walkRepository.countByDeviceDeviceIdAndStartedAtBetween(eq("DEVICE_A"), any(), any())).thenReturn(1L);
        when(walkRepository.countByDeviceDeviceIdAndStartedAtBetween(eq("DEVICE_B"), any(), any())).thenReturn(0L);

        walkSchedulerService.processMidnightWalkPenalty();

        // 검증 2: Device B만 산책 0회로 -5점 패널티를 받아야 함
        verify(scoreService).processScoreEvent(eq("DEVICE_B"), eq("WALK_NONE"), eq(-5), any(String.class));
        System.out.println("✅ [DEVICE_B] 산책 0회 자정 패널티 로직 작동 검증 성공!");
    }
}
