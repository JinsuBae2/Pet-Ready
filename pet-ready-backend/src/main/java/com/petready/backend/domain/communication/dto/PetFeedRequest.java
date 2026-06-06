package com.petready.backend.domain.communication.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 밥 주기 완료 요청을 위한 DTO입니다 (젯슨 나노 비전 AI 및 테스트 용도).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "밥 주기 완료 요청")
public class PetFeedRequest {

    @NotBlank(message = "기기 ID는 필수입니다.")
    @Schema(description = "기기 식별자", example = "DOG_01", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceId;
}
