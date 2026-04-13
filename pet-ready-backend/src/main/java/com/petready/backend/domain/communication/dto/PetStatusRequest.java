package com.petready.backend.domain.communication.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ESP32 기기로부터 수신하는 반려견 상태 로그 요청 DTO입니다.
 */
@Getter
@NoArgsConstructor
@Schema(description = "반려견 상태 수신 요청")
public class PetStatusRequest {

    @NotBlank(message = "기기 ID는 필수입니다.")
    @Schema(description = "기기 식별자", example = "DOG_01")
    private String deviceId;

    @Schema(description = "배터리 잔량 (%)", example = "85")
    private Integer batteryLevel;

    @Schema(description = "충전 중 여부", example = "false")
    private Boolean isCharging;

    @Schema(description = "터치 센서 활성화 여부", example = "true")
    private Boolean touchActive;

    @Schema(description = "압력 센서 측정값", example = "12.5")
    private Double pressureValue;
}
