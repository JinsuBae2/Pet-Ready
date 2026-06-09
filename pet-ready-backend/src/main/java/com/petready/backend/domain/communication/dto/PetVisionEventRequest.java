package com.petready.backend.domain.communication.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로컬 환경 통합 테스트용 비전 이벤트 데이터를 담는 DTO 클래스입니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "로컬 비전 이벤트 요청 데이터 모델")
public class PetVisionEventRequest {

    @NotBlank(message = "기기 식별자는 필수입니다.")
    @Schema(description = "기기 고유 식별자", example = "DOG_01")
    private String deviceId;

    @NotBlank(message = "이벤트 타입은 필수입니다.")
    @Schema(description = "이벤트 타입 (FOOD_BOWL 등)", example = "FOOD_BOWL")
    private String eventType;
}
