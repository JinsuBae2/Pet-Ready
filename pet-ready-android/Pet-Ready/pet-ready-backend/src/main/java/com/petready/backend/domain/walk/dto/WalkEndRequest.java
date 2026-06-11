package com.petready.backend.domain.walk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 산책 종료 시 클라이언트로부터 전달받는 요청 DTO입니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "산책 종료 및 기록 저장 요청")
public class WalkEndRequest {

    @Schema(description = "산책을 수행한 사용자의 고유 식별자", example = "1")
    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    @Schema(description = "산책에 사용된 기기 고유 ID", example = "DOG_01")
    @NotBlank(message = "기기 ID는 필수입니다.")
    private String deviceId;

    @Schema(description = "실제 산책 거리 (km)", example = "2.5")
    @NotNull(message = "산책 거리는 필수입니다.")
    private BigDecimal distanceKm;

    @Schema(description = "산책 지속 시간 (초)", example = "1800")
    @NotNull(message = "산책 시간은 필수입니다.")
    private Long durationSec;

    @Schema(description = "산책 시작 시각 (타임스탬프)", example = "2026-05-19T14:00:00")
    @NotNull(message = "산책 시작 시간은 필수입니다.")
    private LocalDateTime startedAt;

    @Schema(description = "산책 종료 시각 (타임스탬프)", example = "2026-05-19T14:30:00")
    @NotNull(message = "산책 종료 시간은 필수입니다.")
    private LocalDateTime endedAt;

    @Schema(description = "산책 경로 좌표 배열 (순수 JSON 배열)")
    private List<CoordinateDto> route;

    /**
     * 경로 좌표를 담기 위한 내부 정적 클래스입니다.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "산책 경로 좌표 데이터")
    public static class CoordinateDto {
        @Schema(description = "위도", example = "37.5665")
        private double lat;

        @Schema(description = "경도", example = "126.9780")
        private double lng;
    }
}
