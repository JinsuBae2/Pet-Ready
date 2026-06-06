package com.petready.backend.domain.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 반려견 이름(닉네임) 수정을 위한 DTO입니다.
 */
@Getter
@NoArgsConstructor
@Schema(description = "반려견 이름 수정 요청")
public class UpdatePetNameRequest {

    @NotBlank(message = "반려견 이름은 필수입니다.")
    @Schema(description = "새로운 반려견 이름(닉네임)", example = "초코")
    private String petName;
}
