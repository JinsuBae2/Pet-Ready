package com.petready.backend.domain.mission.api;

import com.petready.backend.domain.mission.dto.TrainingGestureRequest;
import com.petready.backend.domain.mission.dto.TrainingRewardRequest;
import com.petready.backend.domain.mission.dto.TrainingRewardResponse;
import com.petready.backend.domain.mission.service.TrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 젯슨나노 비전(제스쳐) 및 아두이노 보상(버튼) 훈련 시퀀스를 연동하고 판정하는 API 컨트롤러입니다.
 */
@Tag(name = "Virtual Dog Training", description = "가상 반려견 훈련(제스쳐 감지 및 보상 판정) API")
@RestController
@RequestMapping("/api/v1/training")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingService trainingService;

    /**
     * 젯슨나노 YOLOv8 비전 모듈이 유저의 훈련 지시 제스쳐를 감지했을 때 호출하는 API입니다.
     */
    @Operation(
        summary = "젯슨나노 제스쳐(훈련 신호) 감지 수신 API [젯슨나노 -> 서버]",
        description = "◆ 젯슨나노(비전 카메라) 연동 가이드:\n" +
                      "* 카메라 화면에서 'SIT'(앉아), 'STAY'(대기) 등의 훈련 구호 제스쳐가 성공적으로 탐지되면 본 API를 즉시 호출해 주세요.\n" +
                      "* 밥그릇 감지 조건과 카메라가 겹쳐도 백엔드 단에서 병렬 처리하므로 안심하고 호출하시면 됩니다.\n" +
                      "* 호출 성공 시 백엔드 메모리 버퍼에 해당 제스쳐가 **60초(1분) 동안 임시 캐싱**되며, 60초 이내에 유저가 보상(버튼)을 입력하지 않으면 방치(SAD/훈련 실패) 로그가 자동 기록됩니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "제스쳐 감지 수신 및 60초 타이머 작동 성공")
    })
    @PostMapping("/gesture")
    public ResponseEntity<Void> receiveGesture(@Valid @RequestBody TrainingGestureRequest request) {
        trainingService.handleGesture(request);
        return ResponseEntity.ok().build();
    }

    /**
     * 아두이노 하드웨어 보상 버튼이 눌렸거나 모바일 앱에서 보상(간식) 버튼을 터치했을 때 호출하는 API입니다.
     */
    @Operation(
        summary = "훈련 보상(버튼 입력) 처리 API [아두이노/안드 -> 서버]",
        description = "◆ 아두이노(ESP32) 및 안드로이드 연동 가이드:\n" +
                      "1. 유저가 로봇의 보상(간식 주기) 버튼을 누르면 본 API를 즉각 호출합니다.\n" +
                      "2. **필수 조치 사항**: API 응답 바디(`TrainingRewardResponse`)로 내려오는 제어 필드들을 파싱하여 하드웨어를 직접 제어해야 합니다.\n" +
                      "   * `lcdCommand`: `LCD_HAPPY`(훈련 성공) 또는 `LCD_CONFUSED`(훈련 실패/뇌정지)가 반환됩니다.\n" +
                      "   * `lcdTextLine1` / `lcdTextLine2`: 16x2 LCD에 출력할 아스키 이모티콘 텍스트입니다. 즉시 하드웨어 LCD API로 화면에 그리십시오.\n" +
                      "   * `ledColor`: `GREEN` 또는 `RED` 색상 명령입니다. 아두이노의 초록/빨강 LED 핀 신호를 이에 맞추어 즉시 출력해 주십시오.\n" +
                      "3. 이 훈련 결과 연출은 **3초 동안 최우선 순위로 고정**되며, 3초가 지난 후에는 평상시 양육 상태 화면으로 알아서 복귀 처리됩니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "보상 시퀀스 무결성 판정 및 LCD/LED 피드백 패킷 반환 완료"),
        @ApiResponse(responseCode = "404", description = "등록되지 않은 기기 ID가 전달됨")
    })
    @PostMapping("/reward")
    public ResponseEntity<TrainingRewardResponse> receiveReward(@Valid @RequestBody TrainingRewardRequest request) {
        return ResponseEntity.ok(trainingService.handleReward(request.getDeviceId()));
    }
}
