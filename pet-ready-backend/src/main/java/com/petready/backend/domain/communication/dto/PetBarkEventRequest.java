package com.petready.backend.domain.communication.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 하드웨어 주도형 짖음 이벤트 감지 요청을 위한 DTO입니다 (ESP32 연동).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "하드웨어 주도형 짖음 이벤트 요청")
public class PetBarkEventRequest {

    @NotBlank(message = "기기 ID는 필수입니다.")
    @Schema(description = "기기 식별자", example = "DOG_03", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceId;

    @Schema(description = "짖음 발생 시각 타임스탬프 (Optional, epoch milliseconds)", example = "1717672800000")
    private Long timestamp;
}
