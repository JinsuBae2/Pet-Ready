package com.petready.backend.domain.mission;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.mission.dto.MissionResponse;
import com.petready.backend.domain.mission.entity.Mission;
import com.petready.backend.domain.mission.repository.MissionRepository;
import com.petready.backend.domain.mission.service.MissionService;
import com.petready.backend.domain.medical.entity.MedicalFeeCache;
import com.petready.backend.domain.medical.repository.MedicalFeeCacheRepository;
import com.petready.backend.domain.report.entity.PetReport;
import com.petready.backend.domain.report.repository.PetReportRepository;
import com.petready.backend.domain.user.entity.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MissionService의 일일 미션 자동 적재 및 MEDICAL 진료비 누적 가산 기능을 검증하는 단위 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
public class MissionServiceTest {

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private MedicalFeeCacheRepository medicalFeeCacheRepository;

    @Mock
    private PetReportRepository reportRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private MissionService missionService;

    @Test
    @DisplayName("성공: 오늘 날짜에 발급된 필수 일일 미션 3종이 없을 시, 최초 조회할 때 자동 초기화 적재(Insert)된다")
    void getTodayMissions_AutoInitialization_Success() {
        // given
        String email = "test@example.com";
        String deviceId = "DEVICE_A";

        Device mockDevice = Device.builder()
                .deviceId(deviceId)
                .petName("바둑이")
                .build();

        // Repository Mocking: 오늘 미션이 존재하지 않음 (exists = false)
        when(deviceRepository.findByUserEmail(email)).thenReturn(Optional.of(mockDevice));
        when(missionRepository.existsByDeviceDeviceIdAndTypeAndIssuedAtAfter(eq(deviceId), anyString(), any(LocalDateTime.class)))
                .thenReturn(false);

        // 최초 저장된 미션들이 없다가, 저장 후 최종 목록 조회 시 3개 미션이 반환된다고 가정
        List<Mission> mockSavedMissions = List.of(
                Mission.builder().id(1L).device(mockDevice).type("WALK").issuedAt(LocalDateTime.now()).isCompleted(false).build(),
                Mission.builder().id(2L).device(mockDevice).type("FEEDING").issuedAt(LocalDateTime.now()).isCompleted(false).build(),
                Mission.builder().id(3L).device(mockDevice).type("ROBOT_PLAY").issuedAt(LocalDateTime.now()).isCompleted(false).build()
        );
        when(missionRepository.findAllByDeviceDeviceIdAndIssuedAtAfter(eq(deviceId), any(LocalDateTime.class)))
                .thenReturn(mockSavedMissions);

        // when
        List<MissionResponse> responses = missionService.getTodayMissions(email);

        // then
        // 1. 필수 미션 3종이 존재하지 않아 각각 DB 저장(save)을 호출했는지 확인
        verify(missionRepository, times(3)).save(any(Mission.class));

        // 2. 최종 3개의 일일 미션 목록이 반환되었는지 확인
        assertNotNull(responses);
        assertEquals(3, responses.size());
        assertTrue(responses.stream().anyMatch(r -> "WALK".equals(r.getType())));
        assertTrue(responses.stream().anyMatch(r -> "FEEDING".equals(r.getType())));
        assertTrue(responses.stream().anyMatch(r -> "ROBOT_PLAY".equals(r.getType())));
    }

    @Test
    @DisplayName("성공: MEDICAL 미션을 완료 처리하면, 진료비 항목 캐시를 로드해 영수증 금액에 누적 합산 가산된다")
    void completeMission_MedicalFeeAggregation_Success() throws Exception {
        // given
        Long missionId = 10L;
        String email = "owner@example.com";

        User mockUser = User.builder()
                .email(email)
                .nickname("진수")
                .build();

        Device mockDevice = Device.builder()
                .deviceId("DOG_03")
                .user(mockUser)
                .petName("바둑이")
                .build();

        Mission mockMedicalMission = Mission.builder()
                .id(missionId)
                .device(mockDevice)
                .type("MEDICAL")
                .issuedAt(LocalDateTime.now().minusHours(1))
                .isCompleted(false)
                .build();

        // 기존 리포트 설정 (가상 유기견 입양비 100,000원이 이미 청구된 상태 가정)
        List<Map<String, Object>> existingItems = new ArrayList<>();
        existingItems.add(Map.of("item", "가상 유기견 입양비", "amount", 100000L, "reason", "최초 기기 등록 시 입양비"));
        String existingJson = objectMapper.writeValueAsString(existingItems);

        PetReport mockReport = PetReport.builder()
                .id(1L)
                .user(mockUser)
                .totalScore(BigDecimal.valueOf(100))
                .grade("A+")
                .totalReceiptAmount(100000L)
                .receiptDetailsJson(existingJson)
                .build();

        // 캐시된 진료 항목 리스트 설정
        List<MedicalFeeCache> mockFeeCaches = List.of(
                MedicalFeeCache.builder().itemName("엑스선촬영비/판독료").feeAmount(BigDecimal.valueOf(37266.00)).build(),
                MedicalFeeCache.builder().itemName("초진 진찰료(개)").feeAmount(BigDecimal.valueOf(10840.00)).build()
        );

        when(missionRepository.findById(missionId)).thenReturn(Optional.of(mockMedicalMission));
        when(reportRepository.findByUserEmail(email)).thenReturn(Optional.of(mockReport));
        when(medicalFeeCacheRepository.findAll()).thenReturn(mockFeeCaches);

        // 수정을 가로채서 검증하기 위한 ArgumentCaptor 설정
        ArgumentCaptor<PetReport> reportCaptor = ArgumentCaptor.forClass(PetReport.class);

        // when
        missionService.completeMission(missionId);

        // then
        // 1. 미션의 완료 여부가 true로 갱신되었는지 확인
        assertTrue(mockMedicalMission.getIsCompleted());

        // 2. PetReport의 누적 합산 및 상세 JSON 영수증 Append 검증
        verify(reportRepository, times(1)).save(reportCaptor.capture());
        PetReport updatedReport = reportCaptor.getValue();

        assertNotNull(updatedReport);
        // 금액 누적 가산 검증: 기존 100,000원 + 추가 진료비(최소 엑스레이 1건~최대 2건의 요금)
        assertTrue(updatedReport.getTotalReceiptAmount() > 100000L);
        
        // JSON 파싱 및 데이터 정합성 검증
        List<Map<String, Object>> parsedItems = objectMapper.readValue(
                updatedReport.getReceiptDetailsJson(), 
                new TypeReference<List<Map<String, Object>>>() {}
        );
        
        // 기존 1개 항목에 신규 랜덤 1~2개 항목이 안전하게 덧붙여졌는지 확인
        assertTrue(parsedItems.size() >= 2); 
        assertEquals("가상 유기견 입양비", parsedItems.get(0).get("item"));
        assertTrue(parsedItems.stream().anyMatch(item -> "엑스선촬영비/판독료".equals(item.get("item")) || "초진 진찰료(개)".equals(item.get("item"))));
    }
}
