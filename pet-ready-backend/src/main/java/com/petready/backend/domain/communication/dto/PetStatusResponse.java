package com.petready.backend.domain.communication.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 수신된 상태를 분석하여 응답하는 DTO입니다.
 * 배고픔, 기분, 건강 상태 및 LCD/LED 기기 제어 피드백이 포함됩니다.
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

    @Schema(description = "상태 확인 LED 색상 (GREEN, RED)", example = "GREEN")
    private String ledColor;

    @Schema(description = "하드웨어 16x2 LCD 출력용 식별 명령", example = "LCD_HAPPY")
    private String lcdCommand;

    @Schema(description = "하드웨어 16x2 LCD 첫 번째 행 아스키 텍스트", example = "[   SO HAPPY   ]")
    private String lcdTextLine1;

    @Schema(description = "하드웨어 16x2 LCD 두 번째 행 아스키 텍스트", example = "DOG:  ( ≧ ▽ ≦ )")
    private String lcdTextLine2;

    @Schema(description = "가상 배터리 잔량 (포만감 연동용)", example = "85")
    private int batteryLevel;
}

