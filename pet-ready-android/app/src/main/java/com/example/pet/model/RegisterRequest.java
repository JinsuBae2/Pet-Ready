package com.example.pet.model;

/*
 * 회원가입 요청 데이터
 * 백엔드 RegisterRequest(email, password, nickname)와 필드명을 동일하게 맞춘다.
 */
public class RegisterRequest {
    public String email;
    public String password;
    public String nickname;

    public RegisterRequest(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }
}
