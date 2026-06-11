package com.petready.backend.domain.mission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 아두이노 보상 버튼 입력 수신을 위한 Request DTO 클래스입니다.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "아두이노 보상 버튼 입력 수신 요청")
public class TrainingRewardRequest {

    @NotBlank(message = "기기 고유 ID는 필수입니다.")
    @Schema(description = "IoT 기기 고유 ID", example = "DOG_01")
    private String deviceId;
}
