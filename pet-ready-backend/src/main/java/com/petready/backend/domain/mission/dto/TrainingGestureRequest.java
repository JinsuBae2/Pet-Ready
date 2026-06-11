package com.petready.backend.domain.mission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 젯슨나노 YOLO 비전 AI가 감지한 유저의 제스쳐(구호) 정보를 전달받는 Request DTO 클래스입니다.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "젯슨나노 제스쳐 감지 수신 요청")
public class TrainingGestureRequest {

    @NotBlank(message = "기기 고유 ID는 필수입니다.")
    @Schema(description = "IoT 기기 고유 ID", example = "DOG_01")
    private String deviceId;

    @NotBlank(message = "감지된 제스쳐 종류는 필수입니다.")
    @Schema(description = "제스쳐 종류 (SIT, STAY 등)", example = "SIT")
    private String gestureType;

    @Schema(description = "제스쳐 인식 신뢰도 (0.0 ~ 1.0)", example = "0.85")
    private Double confidence;
}
