package com.petready.backend.domain.communication.api;

import com.petready.backend.domain.communication.dto.CommandResponse;
import com.petready.backend.domain.communication.dto.PetStatusRequest;
import com.petready.backend.domain.communication.dto.PetStatusResponse;
import com.petready.backend.domain.communication.service.PetCommunicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 기기(ESP32) 및 클라이언트와의 실시간 통신을 위한 컨트롤러입니다.
 */
@Tag(name = "Pet Communication", description = "기기 통신 및 상태 관리 API")
@RestController
@RequestMapping("/api/v1/pet")
@RequiredArgsConstructor
public class PetCommunicationController {

    private final PetCommunicationService communicationService;

    /**
     * 기기로부 센서 데이터를 수신받아 서버에 기록하고 분석 결과를 반환합니다.
     */
    @Operation(summary = "상태 수신 API", description = "기기의 센서 데이터를 전송하고 분석 결과를 받습니다.")
    @PostMapping("/status")
    public ResponseEntity<PetStatusResponse> receiveStatus(@Valid @RequestBody PetStatusRequest request) {
        return ResponseEntity.ok(communicationService.receiveStatus(request));
    }

    /**
     * 기기가 주기적으로 호출하여 수행할 명령이 있는지 확인합니다.
     */
    @Operation(summary = "명령 폴링 API", description = "기기가 아직 수행하지 않은 최신 명령을 조회합니다.")
    @GetMapping("/command/{deviceId}")
    public ResponseEntity<CommandResponse> getPendingCommand(@PathVariable String deviceId) {
        return ResponseEntity.ok(communicationService.getPendingCommand(deviceId));
    }

    /**
     * 기기에서 명령 수신에 성공했음을 서버에 알립니다.
     */
    @Operation(summary = "명령 확인 API", description = "기기가 명령을 성공적으로 수신했음을 확인 처리합니다.")
    @PostMapping("/command/ack/{commandId}")
    public ResponseEntity<Void> acknowledgeCommand(@PathVariable Long commandId) {
        communicationService.acknowledgeCommand(commandId);
        return ResponseEntity.ok().build();
    }
}
