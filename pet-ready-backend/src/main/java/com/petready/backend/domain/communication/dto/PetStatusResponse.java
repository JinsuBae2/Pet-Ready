package com.petready.backend.domain.communication.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 수신된 상태를 분석하여 응답하는 DTO입니다.
 * 배고픔, 기분, 건강 상태 등의 분석 결과가 포함됩니다.
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "반려견 상태 분석 결과 응답")
public class PetStatusResponse {

    @Schema(description = "배고픔 경고 여부", example = "true")
    private boolean isHungry;

    @Schema(description = "현재 기분 상태", example = "GOOD")
    private String mood;

    @Schema(description = "종합 건강 지수", example = "NORMAL")
    private String healthStatus;

    @Schema(description = "분석 메시지", example = "배터리가 부족하여 반려견이 배고플 수 있습니다.")
    private String analysisMessage;
}
