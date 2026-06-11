package com.example.pet.model;

/*
 * 로그인 성공 시 서버에서 받아오는 응답 데이터
 * 현재 백엔드 기준으로 accessToken, refreshToken만 먼저 사용한다.
 */
public class LoginResponse {

    // 인증이 필요한 API 호출 때 사용할 토큰
    public String accessToken;

    // accessToken 만료 시 재발급에 사용할 토큰
    public String refreshToken;
}