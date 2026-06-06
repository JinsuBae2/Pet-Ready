package com.petready.backend.domain.communication.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 젯슨나노 비전 동기화 수신 요청을 위한 DTO입니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "젯슨나노 비전 동기화 요청")
public class JetsonVisionSyncRequest {

    @NotBlank(message = "기기 ID는 필수입니다.")
    @Schema(description = "기기 식별자", example = "DOG_03", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceId;

    @NotNull(message = "밥그릇 인식 감지 상태는 필수입니다.")
    @Schema(description = "밥그릇 인식 감지 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean bowlDetected;
}
