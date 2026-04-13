package com.petready.backend.domain.user.entity;

import com.petready.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 시스템을 이용하는 사용자 정보를 담는 엔티티입니다.
 * 이메일, 비밀번호, 닉네임 및 알림 발송을 위한 FCM 토큰 정보를 관리합니다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    /**
     * 사용자의 고유 식별자 (정수형 PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 사용자의 로그인 이메일 주소
     */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * 해싱 처리된 사용자의 비밀번호
     */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * 서비스 내에서 사용될 사용자의 별명
     */
    @Column(nullable = false, length = 50)
    private String nickname;

    /**
     * Firebase Cloud Messaging 알림 전송을 위한 디바이스 토큰
     */
    @Column(name = "fcm_token")
    private String fcmToken;
}
