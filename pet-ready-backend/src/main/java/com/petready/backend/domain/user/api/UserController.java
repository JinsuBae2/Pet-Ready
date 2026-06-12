package com.petready.backend.domain.user.api;

import com.petready.backend.domain.user.service.ResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 정보 관리 및 회원 탈퇴(완전 삭제)를 처리하는 컨트롤러입니다.
 */
@Tag(name = "User", description = "사용자 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final ResetService resetService;

    /**
     * 현재 로그인된 사용자의 모든 정보(회원 정보, 연동 기기, 시뮬레이션 지표 데이터 등)를 데이터베이스에서 일괄 완전 삭제합니다.
     */
    @Operation(
        summary = "회원 탈퇴 및 데이터 완전 청소 API",
        description = "사용자의 회원 정보와 연동된 IoT 기기 정보, 산책/미션/훈련 및 실시간 점수를 포함한 모든 데이터베이스 찌꺼기를 영구히 삭제합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "회원 탈퇴 및 데이터 삭제 처리 완료"),
        @ApiResponse(responseCode = "401", description = "인증 토큰 누락 또는 유효 만료 상태"),
        @ApiResponse(responseCode = "500", description = "서버 내부 처리 오류")
    })
    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdrawUser(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        log.info("[회원 탈퇴 API 호출] 사용자 이메일: {}", email);
        
        resetService.withdrawUser(email);
        
        return ResponseEntity.ok().build();
    }
}
