package com.petready.backend.domain.mission.api;

import com.petready.backend.domain.mission.service.MissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 모바일 클라이언트 앱에서 발생하는 개별 일반 미션(산책, 밥주기 외 기타 돌봄활동)의 
 * 완료 처리 트랜잭션을 담당하는 컨트롤러 클래스입니다.
 */
@Tag(name = "Mission", description = "미션 상태 및 완료 관리 API")
@RestController
@RequestMapping("/api/v1/mission")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    /**
     * 사용자가 스마트폰 앱 화면에서 개별 미션의 '완료' 버튼을 눌렀을 때 호출되어 상태를 갱신합니다.
     */
    @Operation(
        summary = "일반 미션 수동 완료 처리 API", 
        description = "사용자가 안드로이드 앱에서 특정 돌봄(병원, 접종 등) 미션의 완료 버튼을 수동으로 눌렀을 때 호출됩니다. 해당 미션의 완료 여부(is_completed = true)를 갱신하고 점수 정산 로직을 실행시킵니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "미션 완료 상태 저장 성공"),
        @ApiResponse(responseCode = "404", description = "요청한 미션 일련번호(id)가 데이터베이스에 존재하지 않음"),
        @ApiResponse(responseCode = "401", description = "유효한 JWT 인증 정보가 누락된 경우")
    })
    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> completeMission(@PathVariable("id") Long missionId) {
        
        // 해당 미션의 상태를 강제로 완료 처리하고 이력을 기록합니다.
        missionService.completeMission(missionId);
        return ResponseEntity.ok().build();
    }
}
