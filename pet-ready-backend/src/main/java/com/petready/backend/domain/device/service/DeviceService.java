package com.petready.backend.domain.device.service;

import com.petready.backend.domain.device.dto.DeviceRegisterRequest;
import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.score.entity.RealTimeScore;
import com.petready.backend.domain.score.repository.RealTimeScoreRepository;
import com.petready.backend.domain.user.entity.User;
import com.petready.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}
