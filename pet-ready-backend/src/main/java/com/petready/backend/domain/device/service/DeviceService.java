package com.petready.backend.domain.device.service;

import com.petready.backend.domain.device.dto.DeviceRegisterRequest;
import com.petready.backend.domain.device.dto.MyDeviceResponse;
import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.report.entity.PetReport;
import com.petready.backend.domain.report.repository.PetReportRepository;
import com.petready.backend.domain.score.entity.RealTimeScore;
import com.petready.backend.domain.score.repository.RealTimeScoreRepository;
import com.petready.backend.domain.user.entity.User;
import com.petready.backend.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

/**
 * 기기 등록 및 관리를 담당하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final RealTimeScoreRepository scoreRepository;
    private final PetReportRepository reportRepository;
    private final ObjectMapper objectMapper;

    /**
     * 사용자와 기기를 연결(등록)하고 초기 점수를 세팅합니다.
     * 
     * @param request 등록 요청 데이터
     * @param email 유저 이메일 (인증 정보)
     */
    @Transactional
    public void registerDevice(DeviceRegisterRequest request, String email) {
        // 이미 등록된 기기인지 확인
        if (deviceRepository.existsById(request.getDeviceId())) {
            throw new IllegalStateException("이미 등록된 기기 ID입니다.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 기존에 이 유저가 등록했던 기기가 있다면 삭제하여 1:1 관계 유지
        List<Device> oldDevices = deviceRepository.findAllByUserId(user.getId());
        for (Device oldDevice : oldDevices) {
            String oldDeviceId = oldDevice.getDeviceId();
            deviceRepository.deleteScoreEventsByDeviceId(oldDeviceId);
            deviceRepository.deleteCommandsByDeviceId(oldDeviceId);
            deviceRepository.deleteMissionsByDeviceId(oldDeviceId);
            deviceRepository.deleteWalksByDeviceId(oldDeviceId);
            deviceRepository.deletePetStatusLogsByDeviceId(oldDeviceId);
            deviceRepository.deleteRealTimeScoreByDeviceId(oldDeviceId);
            deviceRepository.delete(oldDevice);
        }
        deviceRepository.flush();

        // 반려견 이름은 최초 등록 시 "반려견"으로 자동 설정
        String petName = "반려견";

        // 하루 산책 목표 거리는 시뮬레이터 평균 기준인 2.0km로 자동 설정
        Double walkGoal = 2.0;

        Device device = Device.builder()
                .deviceId(request.getDeviceId())
                .user(user)
                .petName(petName)
                .walkGoalKm(walkGoal)
                .isOnline(false)
                .build();

        Device savedDevice = deviceRepository.save(device);

        // BK-02 기기 등록 완료 시 해당 기기의 실시간 현재 점수를 100점으로 최초 초기화
        RealTimeScore initialScore = RealTimeScore.builder()
                .device(savedDevice)
                .currentScore(100)
                .lastUpdatedAt(java.time.LocalDateTime.now())
                .build();
        scoreRepository.save(initialScore);

        // [고도화] 기기 등록(입양) 시 초기 비용(입양비 100,000원 + 종합백신 25,992원 + 광견병 24,428원 = 총 150,420원) 산정 적재
        List<Map<String, Object>> initialReceiptItems = new ArrayList<>();
        
        Map<String, Object> adoptionFee = new HashMap<>();
        adoptionFee.put("item", "가상 유기견 입양비");
        adoptionFee.put("amount", 100000L);
        adoptionFee.put("reason", "최초 기기 등록 시 입양비");
        initialReceiptItems.add(adoptionFee);

        Map<String, Object> vaccine1 = new HashMap<>();
        vaccine1.put("item", "개 종합백신");
        vaccine1.put("amount", 25992L);
        vaccine1.put("reason", "기초 필수 접종비");
        initialReceiptItems.add(vaccine1);

        Map<String, Object> vaccine2 = new HashMap<>();
        vaccine2.put("item", "광견병 백신");
        vaccine2.put("amount", 24428L);
        vaccine2.put("reason", "기초 필수 접종비");
        initialReceiptItems.add(vaccine2);

        String initialReceiptJson = "[]";
        try {
            initialReceiptJson = objectMapper.writeValueAsString(initialReceiptItems);
        } catch (Exception e) {
            // JSON 변환 예외 발생 시 에러 로깅
        }

        PetReport initialReport = PetReport.builder()
                .user(user)
                .totalScore(BigDecimal.valueOf(100))
                .grade("A+")
                .totalReceiptAmount(150420L)
                .receiptDetailsJson(initialReceiptJson)
                .totalWalkCount(0)
                .totalMissionCount(0)
                .build();
        reportRepository.save(initialReport);
    }

    /**
     * 사용자의 기기에 등록된 반려견의 닉네임을 변경합니다.
     */
    @Transactional
    public void updatePetName(String petName, String email) {
        Device device = deviceRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저에게 등록된 기기가 없습니다."));
        device.updatePetName(petName);
        deviceRepository.save(device);
     }

    /**
     * 특정 사용자 이메일로 등록된 기기 정보를 조회하여 반환합니다.
     *
     * @param email 사용자 이메일
     * @return 등록된 기기 정보 응답 DTO
     */
    public MyDeviceResponse getMyDevice(String email) {
        // 유저 이메일로 매핑된 기기를 조회합니다. 기기가 없을 시 EntityNotFoundException(404)을 던집니다.
        Device device = deviceRepository.findByUserEmail(email)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("등록된 기기가 존재하지 않습니다."));
        
        // 기기 엔티티를 MyDeviceResponse DTO로 변환하여 반환합니다.
        return MyDeviceResponse.from(device);
    }
}
