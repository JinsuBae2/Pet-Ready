package com.petready.backend.domain.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기기 등록 요청을 위한 DTO입니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "기기 등록 요청")
public class DeviceRegisterRequest {
    
    @NotBlank(message = "기기 ID는 필수입니다.")
    @Schema(description = "등록할 기기 식별자 (QR 코드 스캔 값)", example = "DOG_01", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceId;
}
