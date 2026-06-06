package com.petready.backend.domain.mission.dto;

import com.petready.backend.domain.mission.entity.Mission;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 모바일 클라이언트 앱에 전달될 오늘의 미션 정보를 담는 응답 DTO 클래스입니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "오늘의 미션 정보 응답 데이터 모델")
public class MissionResponse {

    @Schema(description = "미션의 고유 식별자 (ID)", example = "1")
    private Long id;

    @Schema(description = "미션의 종류 (예: BARKING_ALERT, FEEDING_TIME, WALK, CARE 등)", example = "FEEDING_TIME")
    private String type;

    @Schema(description = "미션이 최초로 발급(생성)된 시각", example = "2026-06-06T12:00:00")
    private LocalDateTime issuedAt;

    @Schema(description = "사용자가 미션을 해결(응답)하여 완료 처리한 시각", example = "2026-06-06T12:05:00")
    private LocalDateTime respondedAt;

    @Schema(description = "미션 발급 후 사용자가 응답하기까지 걸린 시간 (초 단위)", example = "300")
    private Long responseTimeSec;

    @Schema(description = "미션 완료 여부 (true: 완료, false: 미완료)", example = "true")
    private Boolean isCompleted;

    /**
     * Mission 엔티티를 MissionResponse DTO 객체로 변환해주는 빌더 기반 정적 팩토리 메서드입니다.
     *
     * @param mission 변환할 미션 엔티티
     * @return 매핑된 MissionResponse DTO
     */
    public static MissionResponse from(Mission mission) {
        return MissionResponse.builder()
                .id(mission.getId())
                .type(mission.getType())
                .issuedAt(mission.getIssuedAt())
                .respondedAt(mission.getRespondedAt())
                .responseTimeSec(mission.getResponseTimeSec())
                .isCompleted(mission.getIsCompleted())
                .build();
    }
}
