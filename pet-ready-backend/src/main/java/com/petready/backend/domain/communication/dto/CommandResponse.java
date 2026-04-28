package com.petready.backend.domain.communication.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 기기가 서버에 수행할 명령이 있는지 확인(Polling)할 때 반환되는 응답 DTO입니다.
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "기기 명령 폴링 응답")
public class CommandResponse {

    @Schema(description = "수행할 명령 존재 여부", example = "true")
    private boolean hasCommand;

    @Schema(description = "명령 식별 ID (수신 확인용)", example = "1")
    private Long commandId;

    @Schema(description = "수행할 명령 내용", example = "BARK_START")
    private String command;

    @Schema(description = "명령 지속 시간 (초)", example = "10")
    private Integer durationSec;

    @Schema(description = "기기가 다음 명령을 확인하기까지 대기할 시간(초). 이벤트 발생 시 폴링 주기를 단축하기 위함.", example = "5")
    private Integer nextPollIntervalSec;
}
