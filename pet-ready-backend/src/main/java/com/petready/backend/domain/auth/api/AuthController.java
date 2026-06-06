package com.petready.backend.domain.auth.api;

import com.petready.backend.domain.auth.dto.LoginRequest;
import com.petready.backend.domain.auth.dto.RegisterRequest;
import com.petready.backend.domain.auth.dto.TokenResponse;
import com.petready.backend.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 신규 유저 등록(회원가입), 로그인(토큰 발급) 및 만료된 토큰의 갱신을 처리하는 인증 도메인 컨트롤러입니다.
 */
@Tag(name = "Authentication", description = "인증 관리 API (회원가입, 로그인, 토큰 갱신)")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 신규 사용자가 이메일, 패스워드, 닉네임 정보를 입력하여 새로운 계정을 생성합니다.
     */
    @Operation(
        summary = "신규 사용자 회원가입 API", 
        description = "전달받은 이메일, 패스워드, 닉네임을 유효성 검증한 후 새로운 유저 엔티티를 영속화합니다. 패스워드는 해시화되어 안전하게 암호화 저장됩니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "회원가입 완료 및 계정 생성 성공"),
        @ApiResponse(responseCode = "400", description = "입력한 이메일 형식이 유효하지 않거나 이미 사용 중인 이메일인 경우")
    })
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        
        // 유저 등록 비즈니스 로직을 호출합니다.
        authService.register(request);
        return ResponseEntity.ok().build();
    }

    /**
     * 사용자가 이메일과 비밀번호를 전송하여 실시간 인증을 수행하고 토큰을 발급받습니다.
     */
    @Operation(
        summary = "사용자 로그인 및 JWT 토큰 발급 API", 
        description = "이메일과 비밀번호를 검증하여 일치 시 30분 유효기간의 Access Token과 30일 유효기간의 Refresh Token을 신규 발급합니다. 모바일 푸시 알림 수신을 위한 FCM 기기 토큰도 함께 수집하여 계정에 갱신 등록합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "인증 완료 및 JWT 발급 성공"),
        @ApiResponse(responseCode = "400", description = "비밀번호 불일치 혹은 가입되지 않은 이메일인 경우")
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        
        // 사용자 인증 및 토큰 발급 결과를 반환합니다.
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * 모바일 앱 세션 만료 시 Refresh Token을 통해 새로운 Access Token을 갱신 발급받습니다.
     */
    @Operation(
        summary = "사용자 인증 토큰(Access Token) 재발급 API", 
        description = "사용자의 세션 만료 시 저장된 Refresh Token을 전달받아 유효성을 검증하고, 새로운 Access Token과 Refresh Token 세트를 재발급하여 세션을 연장합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 재발행 성공"),
        @ApiResponse(responseCode = "400", description = "전달된 Refresh Token이 만료되었거나 서명이 유효하지 않은 경우")
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestParam String refreshToken) {
        
        // 토큰 갱신 작업을 실행하고 새 토큰 결과를 반환합니다.
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }
}
