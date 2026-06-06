package com.petready.backend.domain.communication.api;

import com.petready.backend.domain.communication.dto.CommandResponse;
import com.petready.backend.domain.communication.dto.PetFeedRequest;
import com.petready.backend.domain.communication.dto.PetStatusRequest;
import com.petready.backend.domain.communication.dto.PetStatusResponse;
import com.petready.backend.domain.communication.dto.PetBarkEventRequest;
import com.petready.backend.domain.communication.dto.JetsonVisionSyncRequest;
import com.petready.backend.domain.communication.service.PetCommunicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 기기(ESP32), 젯슨나노 및 클라이언트 앱과의 실시간 통신 및 제어를 처리하는 컨트롤러 클래스입니다.
 * 모든 통신 규격 및 이벤트 교환은 본 컨트롤러를 통해 이루어집니다.
 */
@Tag(name = "Pet Communication", description = "기기 통신, 젯슨나노 비전 동기화 및 실시간 상태 관리 API")
@RestController
@RequiredArgsConstructor
public class PetCommunicationController {

    private final PetCommunicationService communicationService;

    /**
     * 기기(ESP32)로부터 터치 센서 및 배터리 수치를 실시간으로 수신받아 분석 결과를 반환합니다.
     */
    @Operation(
        summary = "하드웨어 상태 수신 API (ESP32 -> 서버)", 
        description = "로봇 기기가 30초 주기로 호출합니다. 머리/등 터치 센서 상태와 가상 배터리 감쇄 시뮬레이션 수치를 전달받아 펫의 감정(NORMAL, HAPPY), 건강 상태(GOOD, WARNING) 및 LED 출력 색상(GREEN, RED)을 결정하여 반환합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "상태 분석 결과 정상 반환 완료"),
        @ApiResponse(responseCode = "404", description = "등록되지 않은 기기 ID가 전달됨")
    })
    @PostMapping("/api/v1/pet/status")
    public ResponseEntity<PetStatusResponse> receiveStatus(@Valid @RequestBody PetStatusRequest request) {
        // 서비스 단으로 상태 데이터를 넘겨 분석 결과를 수집하고 반환합니다.
        return ResponseEntity.ok(communicationService.receiveStatus(request));
    }

    /**
     * 사용자가 앱 터치로 밥 주기를 수행했을 때 호출하는 API입니다. 
     * 젯슨나노 비전 카메라 인식 조건과 함께 미션 락 해제를 구성합니다.
     */
    @Operation(
        summary = "모바일 앱 사용자 밥 주기 버튼 터치 API (앱 -> 서버)", 
        description = "사용자가 안드로이드 앱 화면에서 밥 주기 버튼을 누를 때 호출됩니다. 가상 피딩 우회 차단을 위한 '앱 터치 - 실물 밥그릇 비전' 크로스 체크 미션 락(Lock)에서 'appFeedClicked = true' 상태를 활성화합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "앱 피딩 상태 기록 성공"),
        @ApiResponse(responseCode = "404", description = "등록되지 않은 기기 ID가 전달됨")
    })
    @PostMapping("/api/v1/pet/feed")
    public ResponseEntity<Void> feedPetByApp(@Valid @RequestBody PetFeedRequest request) {
        // 앱 터치 완료 신호를 수집하여 락 해제 검증 단계를 수행합니다.
        communicationService.feedPetByApp(request.getDeviceId());
        return ResponseEntity.ok().build();
    }

    /**
     * 젯슨 나노 AI가 실물 밥그릇을 카메라로 인식했을 때 감지 로그를 전송하는 API입니다.
     * 모바일 앱 터치 조건과 크로스 체크하여 피딩 미션을 완수합니다.
     */
    @Operation(
        summary = "젯슨나노 비전 동기화 수신 API (젯슨나노 -> 서버)", 
        description = "젯슨나노의 비전 AI 카메라가 실물 밥그릇을 감지하면 호출됩니다. 'bowlDetected = true' 상태를 동기화하며, 앱 피딩 상태가 이미 켜져 있다면 피딩 미션 완료 및 점수 정산(+3점), 배터리 100% 충전 상태 리셋을 동시에 처리합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "비전 동기화 수신 및 크로스 체크 처리 성공"),
        @ApiResponse(responseCode = "404", description = "등록되지 않은 기기 ID가 전달됨")
    })
    @PostMapping("/api/v1/jetson/vision-sync")
    public ResponseEntity<Void> syncVisionByJetson(@Valid @RequestBody JetsonVisionSyncRequest request) {
        // 젯슨 나노로부터 감지 여부를 동기화받아 락 해제 검증을 실행합니다.
        communicationService.syncVisionByJetson(request.getDeviceId(), request.getBowlDetected());
        return ResponseEntity.ok().build();
    }

    /**
     * 아두이노(ESP32)가 자체 스케줄러 및 오디오 조건에 따라 스피커로 짖기 시작할 때 호출하는 이벤트 API입니다.
     */
    @Operation(
        summary = "하드웨어 주도형 짖음 이벤트 수신 API (ESP32 -> 서버)", 
        description = "아두이노 로봇견이 짖는 오디오 출력을 개시하자마자 서버에 타임스탬프를 쏘는 API입니다. 호출 즉시 서버는 짖음 미션을 개시하고 안드로이드 앱으로 FCM 돌발 알림 푸시를 전송하며, 대응 속도를 측정하기 위한 정밀 타이머를 가동합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "짖음 미션 발급 및 FCM 알림 즉시 발송 완료"),
        @ApiResponse(responseCode = "404", description = "등록되지 않은 기기 ID가 전달됨")
    })
    @PostMapping("/api/v1/device/bark-event")
    public ResponseEntity<Void> receiveBarkEvent(@Valid @RequestBody PetBarkEventRequest request) {
        // 하드웨어 주도형 짖음 이벤트를 수신하여 미션을 생성하고 대응 타이머를 개시합니다.
        communicationService.receiveBarkEvent(request.getDeviceId());
        return ResponseEntity.ok().build();
    }

    /**
     * 기기가 주기적으로 호출하여 수행할 스피커/LED 명령어가 있는지 큐에서 확인하는 폴링 API입니다.
     */
    @Operation(
        summary = "명령어 폴링 API (ESP32 -> 서버)", 
        description = "ESP32 기기가 수행해야 할 명령(짖음 정지 SOUND_STOP, 배터리 부족 앓는 소리 WHINE_START 등)을 FIFO 큐 형태로 대기 목록에서 가져갑니다. 명령어 수행 도중에는 폴링 간격이 5초로 단축됩니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "대기 명령 유무(hasCommand) 및 명령어 종류 정상 응답 완료")
    })
    @GetMapping("/api/v1/pet/command/{deviceId}")
    public ResponseEntity<CommandResponse> getPendingCommand(@PathVariable String deviceId) {
        // 아직 기기가 수신하지 않은 대기 명령어를 가져와 반환합니다.
        return ResponseEntity.ok(communicationService.getPendingCommand(deviceId));
    }

    /**
     * 기기에서 명령을 최종 수신하여 실행을 마쳤음을 알리고 수신 확인(Ack) 처리를 수행하는 API입니다.
     */
    @Operation(
        summary = "명령 수신 확인 API (ESP32 -> 서버)", 
        description = "기기가 명령을 최종적으로 인지하고 실행 완료했음을 알립니다. 서버는 해당 명령어 객체의 acked_at 필드를 갱신하여 큐에서 제외시킵니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "명령어 수신 확인 처리 완료"),
        @ApiResponse(responseCode = "404", description = "존재하지 않거나 유효하지 않은 명령 ID인 경우")
    })
    @PostMapping("/api/v1/pet/command/ack/{commandId}")
    public ResponseEntity<Void> acknowledgeCommand(@PathVariable Long commandId) {
        // 특정 명령 ID에 대해 수신 확인을 처리하여 중복 수신을 방지합니다.
        communicationService.acknowledgeCommand(commandId);
        return ResponseEntity.ok().build();
    }
}
