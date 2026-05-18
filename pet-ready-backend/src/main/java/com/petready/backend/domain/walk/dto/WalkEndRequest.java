package com.petready.backend.domain.walk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "산책 종료 및 기록 저장 요청")
public class WalkEndRequest {

    @Schema(description = "기기 고유 ID", example = "DOG_01")
    @NotBlank(message = "기기 ID는 필수입니다.")
    private String deviceId;

    @Schema(description = "실제 산책 거리 (km)", example = "2.5")
    @NotNull(message = "산책 거리는 필수입니다.")
    private BigDecimal distanceKm;

    @Schema(description = "산책 지속 시간 (초)", example = "1800")
    @NotNull(message = "산책 시간은 필수입니다.")
    private Long durationSec;

    @Schema(description = "산책 경로 데이터 (JSON 형태)", example = "[{\"lat\": 37.5, \"lng\": 127.0}, ...]")
    private String routeJson;

    @Schema(description = "산책 목표 거리 (km)", example = "2.0")
    @NotNull(message = "산책 목표는 필수입니다.")
    private BigDecimal walkGoalKm;
}
