package com.petready.backend.domain.mission.api;

import com.petready.backend.domain.mission.dto.MissionResponse;
import com.petready.backend.domain.mission.service.MissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 모바일 클라이언트 앱에서 발생하는 개별 일반 미션(산책, 밥주기 외 기타 돌봄활동)의 
 * 완료 처리 트랜잭션 및 조회를 담당하는 컨트롤러 클래스입니다.
 */
@Tag(name = "Mission", description = "미션 상태 및 완료 관리 API")
@RestController
@RequestMapping("/api/v1/mission")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    /**
     * 사용자가 스마트폰 앱 화면에 진입했을 때 오늘자 미션 목록을 조회하여 반환합니다.
     */
    @Operation(
        summary = "오늘의 미션 목록 조회 API", 
        description = "현재 로그인된 사용자의 기기에 할당된 오늘(자정 이후)의 미션 목록을 조회하여 각각의 미션 식별자, 종류, 발급 시각, 완료 여부를 반환합니다. 오늘 발급된 필수 일일 미션 3종(산책, 밥주기, 놀아주기)이 없을 시 최초 호출 시점에 백엔드에서 자동 생성 후 리스트를 리턴합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "오늘의 미션 목록 조회 성공"),
        @ApiResponse(responseCode = "404", description = "현재 로그인된 유저가 등록한 기기가 존재하지 않음"),
        @ApiResponse(responseCode = "401", description = "유효한 JWT 인증 정보가 누락된 경우")
    })
    @GetMapping("/today")
    public ResponseEntity<List<MissionResponse>> getTodayMissions(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // 인증된 사용자 이메일로 매핑된 기기의 오늘의 미션 목록을 가져옵니다.
        List<MissionResponse> response = missionService.getTodayMissions(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자가 스마트폰 앱 화면에서 개별 미션의 '완료' 버튼을 눌렀을 때 호출되어 상태를 갱신합니다.
     */
    @Operation(
        summary = "일반 미션 수동 완료 처리 API", 
        description = "사용자가 안드로이드 앱에서 특정 돌봄(병원, 접종 등) 미션의 완료 버튼을 수동으로 눌렀을 때 호출됩니다. 해당 미션의 상태를 COMPLETED로 갱신하고, FCM 알림을 전송하며, 중복 완료 요청 시 안전하게 스킵합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "미션 완료 상태 저장 성공"),
        @ApiResponse(responseCode = "404", description = "요청한 미션 일련번호(id)가 데이터베이스에 존재하지 않음"),
        @ApiResponse(responseCode = "403", description = "타인의 미션 ID로 접근하여 권한이 없는 경우"),
        @ApiResponse(responseCode = "401", description = "유효한 JWT 인증 정보가 누락된 경우")
    })
    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> completeMission(
            @PathVariable("id") Long missionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // 해당 미션의 상태를 강제로 완료 처리하고 이력을 기록하며 FCM을 보냅니다.
        missionService.completeMission(missionId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * 사용자가 안드로이드 앱에서 특정 미션의 '시작' 버튼을 눌렀을 때 호출되어 상태를 진행 중으로 변경합니다.
     */
    @Operation(
        summary = "미션 진행 시작 API", 
        description = "사용자가 안드로이드 앱에서 특정 미션의 진행을 시작할 때 호출됩니다. 해당 미션의 상태를 IN_PROGRESS로 변경하고, startedAt 시각을 기록하며, 중복 요청 시 기존 진행중 데이터를 안전하게 반환합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "미션 시작 상태 저장 성공 및 현재 미션 정보 반환"),
        @ApiResponse(responseCode = "404", description = "요청한 미션 일련번호(id)가 데이터베이스에 존재하지 않음"),
        @ApiResponse(responseCode = "403", description = "타인의 미션 ID로 접근하여 권한이 없는 경우"),
        @ApiResponse(responseCode = "401", description = "유효한 JWT 인증 정보가 누락된 경우")
    })
    @PostMapping("/{id}/start")
    public ResponseEntity<MissionResponse> startMission(
            @PathVariable("id") Long missionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        MissionResponse response = missionService.startMission(missionId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * 안드로이드 앱에서 폴링하며 미션 상태를 실시간 동기화하기 위해 개별 미션을 조회합니다.
     */
    @Operation(
        summary = "단일 미션 상태 조회 API", 
        description = "안드로이드 앱에서 미션의 실시간 진행 상태를 폴링하기 위해 특정 미션의 정보를 단건 조회합니다. 기기 소유주의 정보와 대조하여 보안을 확보합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "단일 미션 상태 조회 성공"),
        @ApiResponse(responseCode = "404", description = "요청한 미션 일련번호(id)가 데이터베이스에 존재하지 않음"),
        @ApiResponse(responseCode = "403", description = "타인의 미션 ID로 접근하여 권한이 없는 경우"),
        @ApiResponse(responseCode = "401", description = "유효한 JWT 인증 정보가 누락된 경우")
    })
    @GetMapping("/{id}")
    public ResponseEntity<MissionResponse> getMission(
            @PathVariable("id") Long missionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        MissionResponse response = missionService.getMission(missionId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}
