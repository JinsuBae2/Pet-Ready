package com.petready.backend.domain.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "메인 대시보드 화면 응답 DTO")
public class DashboardResponse {

    @Schema(description = "기기 고유 ID", example = "DOG_01")
    private String deviceId;

    @Schema(description = "반려견 이름", example = "뭉치")
    private String petName;

    // --- 신규 추가된 실시간 점수 관련 필드 ---

    @Schema(description = "현재 실시간 점수 (0~100)", example = "95")
    private Integer currentScore;

    @Schema(description = "가장 최근 점수 변동 폭", example = "-5")
    private Integer lastScoreDelta;

    @Schema(description = "가장 최근 점수 변동 이벤트 타입", example = "WALK_NONE")
    private String lastScoreEvent;
}
