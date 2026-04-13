package com.petready.backend.domain.communication.service;

import com.petready.backend.domain.command.entity.Command;
import com.petready.backend.domain.command.repository.CommandRepository;
import com.petready.backend.domain.communication.dto.CommandResponse;
import com.petready.backend.domain.communication.dto.PetStatusRequest;
import com.petready.backend.domain.communication.dto.PetStatusResponse;
import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.log.entity.PetStatusLog;
import com.petready.backend.domain.log.repository.PetStatusLogRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 기기(ESP32)와의 통신 및 데이터 분석을 담당하는 서비스 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetCommunicationService {

    private final DeviceRepository deviceRepository;
    private final PetStatusLogRepository logRepository;
    private final CommandRepository commandRepository;

    /**
     * 기기로부 상태 로그를 수신하여 저장하고, 기기의 실시간 상태를 업데이트합니다.
     * 수신된 데이터를 바탕으로 반려견의 상태를 분석합니다.
     * 
     * @param request 상태 수신 데이터
     * @return 상태 분석 결과
     */
    @Transactional
    public PetStatusResponse receiveStatus(PetStatusRequest request) {
        // 1. 기기 존재 여부 확인
        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new EntityNotFoundException("등록되지 않은 기기입니다: " + request.getDeviceId()));

        // 2. 로그 저장
        PetStatusLog statusLog = PetStatusLog.builder()
                .device(device)
                .batteryLevel(request.getBatteryLevel())
                .isCharging(request.getIsCharging())
                .touchActive(request.getTouchActive())
                .pressureValue(request.getPressureValue())
                .recordedAt(LocalDateTime.now())
                .build();
        logRepository.save(statusLog);

        // 3. 기기 실시간 상태 업데이트 (Heartbeat)
        // Device의 필드를 업데이트하기 위해 수동으로 업데이트 (Entity가 Immutable하지 않은 경우)
        // 여기서는 간단히 리포지토리를 통해 업데이트 로직을 태우거나, 필드 접근용 메서드 필요
        // (참고: 현 엔티티 설계상 Setter가 없으므로 Reflection이나 별도 메서드 필요)
        updateDeviceHeartbeat(device);

        // 4. 상태 분석 로직
        return analyzePetStatus(request);
    }

    /**
     * 기기가 수행해야 할 아직 확인되지 않은 최신 명령을 조회합니다.
     * 
     * @param deviceId 기기 ID
     * @return 명령 정보 (명령이 없으면 hasCommand = false)
     */
    public CommandResponse getPendingCommand(String deviceId) {
        // 해당 기기의 acked_at이 null인 가장 최신 명령 1건 조회
        List<Command> pendingCommands = commandRepository.findAllByDeviceDeviceIdOrderByCreatedAtDesc(deviceId);
        
        return pendingCommands.stream()
                .filter(c -> c.getAckedAt() == null)
                .findFirst()
                .map(command -> CommandResponse.builder()
                        .hasCommand(true)
                        .commandId(command.getId())
                        .command(command.getCommand())
                        .durationSec(command.getDurationSec())
                        .build())
                .orElse(CommandResponse.builder().hasCommand(false).build());
    }

    /**
     * 기기에서 명령 수신을 확인(Ack)했을 때 처리합니다.
     * 
     * @param commandId 명령 ID
     */
    @Transactional
    public void acknowledgeCommand(Long commandId) {
        Command command = commandRepository.findById(commandId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 명령입니다: " + commandId));
        
        // acked_at 업데이트 (현재 엔티티 구조상 리플렉션이나 메서드 필요)
        // 실무에서는 @Setter나 update용 메서드를 엔티티에 추가함
        // 여기서는 개념적 구현을 위해 로직 기술
        updateCommandAck(command);
    }

    private void updateDeviceHeartbeat(Device device) {
        device.updateHeartbeat();
        log.info("기기 [{}] 하트비트 업데이트: Online", device.getDeviceId());
    }

    private void updateCommandAck(Command command) {
        command.acknowledge();
        log.info("명령 [{}] 수신 확인 처리", command.getId());
    }

    /**
     * 배터리, 터치, 압력 센서 데이터를 기반으로 반려견의 상태를 분석하는 기초 로직입니다.
     */
    private PetStatusResponse analyzePetStatus(PetStatusRequest request) {
        boolean isHungry = request.getBatteryLevel() != null && request.getBatteryLevel() <= 20;
        String mood = "NORMAL";
        String healthStatus = "GOOD";
        String message = "반려견이 안정적인 상태입니다.";

        if (Boolean.TRUE.equals(request.getTouchActive())) {
            mood = "HAPPY";
            message = "반려견이 주인의 터치를 느껴 기분이 좋습니다!";
        } else if (request.getPressureValue() != null && request.getPressureValue() > 50.0) {
            mood = "STRESSED";
            healthStatus = "WARNING";
            message = "강한 압력이 감지되었습니다. 반려견의 상태를 확인해주세요.";
        }

        if (isHungry) {
            message = "배터리가 부족하여 반려견의 배고픔 수치가 올라갔습니다.";
        }

        return PetStatusResponse.builder()
                .isHungry(isHungry)
                .mood(mood)
                .healthStatus(healthStatus)
                .analysisMessage(message)
                .build();
    }
}
