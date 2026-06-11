package com.petready.backend.domain.mission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * 아두이노 및 모바일 앱의 보상(버튼) 입력 결과에 따른 LCD 및 LED 제어 피드백 DTO 클래스입니다.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "훈련 보상 처리 결과 피드백")
public class TrainingRewardResponse {

    @Schema(description = "훈련 판정 상태 (SUCCESS, CONFUSED)", example = "SUCCESS")
    private String status;

    @Schema(description = "16x2 LCD 출력 명령어 코드 (LCD_HAPPY, LCD_CONFUSED)", example = "LCD_HAPPY")
    private String lcdCommand;

    @Schema(description = "LCD 1번째 라인 출력 문자열", example = "[ TRAINING OK! ]")
    private String lcdTextLine1;

    @Schema(description = "LCD 2번째 라인 출력 문자열", example = "DOG:  ( ^ _ ^ )/")

    private String lcdTextLine2;

    @Schema(description = "LED 출력 색상 (GREEN, RED)", example = "GREEN")
    private String ledColor;
}
