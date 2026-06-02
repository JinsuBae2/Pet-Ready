package com.petready.backend.domain.mission.api;

import com.petready.backend.domain.mission.service.MissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 미션 관련 API 엔드포인트를 제공하는 컨트롤러입니다.
 */
@Tag(name = "Mission", description = "미션 알림 및 완료 관리 API")
@RestController
@RequestMapping("/api/v1/mission")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    @Operation(summary = "미션 완료", description = "사용자가 모바일 앱에서 미션을 수행했음을 서버에 알립니다.")
    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> completeMission(@PathVariable("id") Long missionId) {
        missionService.completeMission(missionId);
        return ResponseEntity.ok().build();
    }
}
