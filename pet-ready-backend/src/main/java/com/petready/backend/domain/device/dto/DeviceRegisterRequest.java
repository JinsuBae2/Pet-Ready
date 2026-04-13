package com.petready.backend.domain.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기기 등록 요청을 위한 DTO입니다.
 */
@Getter
@NoArgsConstructor
@Schema(description = "기기 등록 요청")
public class DeviceRegisterRequest {
    
    @NotBlank(message = "기기 ID는 필수입니다.")
    @Schema(description = "등록할 기기 식별자", example = "DOG_01")
    private String deviceId;

    @NotBlank(message = "반려견 이름은 필수입니다.")
    @Schema(description = "반려견 이름", example = "초코")
    private String petName;

    @Schema(description = "하루 산책 목표 거리 (km 단위, 최소 0.1km)", example = "3.0")
    private Double walkGoalKm;
}
