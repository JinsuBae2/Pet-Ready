package com.petready.backend.domain.device.dto;

import com.petready.backend.domain.device.entity.Device;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인한 사용자의 등록된 기기 정보를 전달하는 응답 DTO 클래스입니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "로그인 사용자 소유 기기 정보 응답 데이터 모델")
public class MyDeviceResponse {

    @Schema(description = "기기의 고유 식별자 (ID)", example = "DOG_03")
    private String deviceId;

    @Schema(description = "사용자가 등록한 반려견의 이름", example = "바둑이")
    private String petName;

    @Schema(description = "하루 산책 목표 거리 (km 단위)", example = "2.0")
    private Double walkGoalKm;

    @Schema(description = "기기의 현재 온라인 연결 여부 (true: 온라인, false: 오프라인)", example = "true")
    private Boolean isOnline;

    /**
     * Device 엔티티를 MyDeviceResponse DTO로 변환하는 정적 팩토리 메서드입니다.
     *
     * @param device 변환할 기기 엔티티
     * @return 매핑된 MyDeviceResponse DTO
     */
    public static MyDeviceResponse from(Device device) {
        if (device == null) {
            return null;
        }
        return MyDeviceResponse.builder()
                .deviceId(device.getDeviceId())
                .petName(device.getPetName())
                .walkGoalKm(device.getWalkGoalKm())
                .isOnline(device.getIsOnline())
                .build();
    }
}
