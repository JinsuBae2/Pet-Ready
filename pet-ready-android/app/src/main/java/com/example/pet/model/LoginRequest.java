package com.example.pet.model;

/*
 * 로그인 요청용 클래스
 * 사용자가 입력한 이메일과 비밀번호를 서버로 보낼 때 사용한다.
 */
public class LoginRequest {

    // 사용자가 입력한 이메일
    public String email;

    // 사용자가 입력한 비밀번호
    public String password;

    // 로그인 시 서버에 등록할 FCM 토큰
    public String fcmToken;

    /*
     * 생성자
     * 로그인 버튼을 눌렀을 때 입력값을 이 객체에 담아서 서버로 보낸다.
     */
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
        this.fcmToken = null;
    }

    public LoginRequest(String email, String password, String fcmToken) {
        this.email = email;
        this.password = password;
        this.fcmToken = fcmToken;
    }
}
