package com.petready.backend.domain.communication.api;

import com.petready.backend.domain.communication.dto.CommandResponse;
import com.petready.backend.domain.communication.dto.PetFeedRequest;
import com.petready.backend.domain.communication.dto.PetStatusRequest;
import com.petready.backend.domain.communication.dto.PetStatusResponse;
import com.petready.backend.domain.communication.dto.PetBarkEventRequest;
import com.petready.backend.domain.communication.dto.JetsonVisionSyncRequest;
import com.petready.backend.domain.communication.service.PetCommunicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 기기(ESP32), 젯슨나노 및 클라이언트와의 실시간 통신을 위한 통합 컨트롤러입니다.
 */
@Tag(name = "Pet Communication", description = "기기 통신, 젯슨나노 및 상태 관리 API")
@RestController
@RequiredArgsConstructor
public class PetCommunicationController {

    private final PetCommunicationService communicationService;

    /**
     * 기기(ESP32)로부터 센서 데이터를 수신받아 서버에 기록하고 분석 결과를 반환합니다.
     */
    @Operation(summary = "상태 수신 API", description = "기기의 센서 데이터를 전송하고 분석 결과를 받습니다.")
    @PostMapping("/api/v1/pet/status")
    public ResponseEntity<PetStatusResponse> receiveStatus(@Valid @RequestBody PetStatusRequest request) {
        return ResponseEntity.ok(communicationService.receiveStatus(request));
    }

    /**
     * 사용자가 앱 터치로 밥 주기를 수행했을 때 호출하는 API입니다 (크로스 체크 미션 락 연동).
     */
    @Operation(summary = "앱 터치 밥 주기 API", description = "사용자가 앱에서 밥 주기 버튼을 눌렀을 때 크로스 체크용 앱 피딩 상태를 활성화합니다.")
    @PostMapping("/api/v1/pet/feed")
    public ResponseEntity<Void> feedPetByApp(@Valid @RequestBody PetFeedRequest request) {
        communicationService.feedPetByApp(request.getDeviceId());
        return ResponseEntity.ok().build();
    }

    /**
     * 젯슨 나노 AI가 실물 밥그릇 인식 성공/실패 여부를 전송하는 API입니다 (크로스 체크 미션 락 연동).
     */
    @Operation(summary = "젯슨나노 비전 동기화 수신 API", description = "젯슨나노 AI가 밥그릇 인식을 성공/실패했을 때 상태를 동기화합니다.")
    @PostMapping("/api/v1/jetson/vision-sync")
    public ResponseEntity<Void> syncVisionByJetson(@Valid @RequestBody JetsonVisionSyncRequest request) {
        communicationService.syncVisionByJetson(request.getDeviceId(), request.getBowlDetected());
        return ResponseEntity.ok().build();
    }

    /**
     * 아두이노(ESP32)가 자체 스케줄에 따라 짖기 시작할 때 짖음 이벤트를 수신하는 API입니다.
     */
    @Operation(summary = "하드웨어 주도형 짖음 이벤트 수신 API", description = "아두이노가 짖자마자 서버로 타임스탬프를 송신하여 짖음 미션을 개시합니다.")
    @PostMapping("/api/v1/device/bark-event")
    public ResponseEntity<Void> receiveBarkEvent(@Valid @RequestBody PetBarkEventRequest request) {
        communicationService.receiveBarkEvent(request.getDeviceId());
        return ResponseEntity.ok().build();
    }

    /**
     * 기기가 주기적으로 호출하여 수행할 명령이 있는지 확인합니다.
     */
    @Operation(summary = "명령 폴링 API", description = "기기가 아직 수행하지 않은 최신 명령을 조회합니다.")
    @GetMapping("/api/v1/pet/command/{deviceId}")
    public ResponseEntity<CommandResponse> getPendingCommand(@PathVariable String deviceId) {
        return ResponseEntity.ok(communicationService.getPendingCommand(deviceId));
    }

    /**
     * 기기에서 명령 수신에 성공했음을 서버에 알립니다.
     */
    @Operation(summary = "명령 확인 API", description = "기기가 명령을 성공적으로 수신했음을 확인 처리합니다.")
    @PostMapping("/api/v1/pet/command/ack/{commandId}")
    public ResponseEntity<Void> acknowledgeCommand(@PathVariable Long commandId) {
        communicationService.acknowledgeCommand(commandId);
        return ResponseEntity.ok().build();
    }
}
