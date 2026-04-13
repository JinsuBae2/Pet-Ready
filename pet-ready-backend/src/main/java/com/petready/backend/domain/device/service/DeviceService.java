package com.petready.backend.domain.device.service;

import com.petready.backend.domain.device.dto.DeviceRegisterRequest;
import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.user.entity.User;
import com.petready.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기기 등록 및 관리를 담당하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    /**
     * 사용자와 기기를 연결(등록)합니다.
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

        // 목표 거리 보정 (최소 0.1km)
        Double walkGoal = request.getWalkGoalKm();
        if (walkGoal == null || walkGoal < 0.1) {
            walkGoal = 0.1;
        }

        Device device = Device.builder()
                .deviceId(request.getDeviceId())
                .user(user)
                .petName(request.getPetName())
                .walkGoalKm(walkGoal)
                .isOnline(false)
                .build();

        deviceRepository.save(device);
    }
}
