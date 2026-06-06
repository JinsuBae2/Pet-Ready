package com.petready.backend.domain.communication.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ESP32 기기로부터 수신하는 반려견 상태 로그 요청 DTO입니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "반려견 상태 수신 요청")
public class PetStatusRequest {

    @NotBlank(message = "기기 ID는 필수입니다.")
    @Schema(description = "기기 식별자", example = "DOG_01", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceId;

    @Schema(description = "머리 터치 센서 활성화 여부", example = "false")
    private Boolean headTouch;

    @Schema(description = "등 터치 센서 1 활성화 여부", example = "false")
    private Boolean backTouch1;

    @Schema(description = "등 터치 센서 2 활성화 여부", example = "false")
    private Boolean backTouch2;
}
