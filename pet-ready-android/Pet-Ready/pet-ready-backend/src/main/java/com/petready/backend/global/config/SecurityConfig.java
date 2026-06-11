package com.petready.backend.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.petready.backend.global.security.JwtAuthenticationFilter;
import com.petready.backend.global.security.JwtTokenProvider;

/**
 * Spring Security 및 보안 관련 설정 클래스입니다.
 * JWT 기반 인증을 사용하므로 세션 관리를 비활성화하고, API 접근 권한을 설정합니다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 비밀번호를 안전하게 암호화하기 위한 BCryptPasswordEncoder Bean입니다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * HTTP 보안 필터 체인을 구성하는 Bean입니다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. CSRF 보안 비활성화 (JWT 사용으로 인한 세션 미사용)
            .csrf(AbstractHttpConfigurer::disable)
            // 2. CORS 기본 설정 적용 (안드로이드 기기 등 외부 접속 허용)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 3. 폼 로그인 및 HTTP Basic 인증 비활성화
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            // 4. 세션 정책을 STATELESS로 설정
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 5. URL별 접근 권한 설정
            .authorizeHttpRequests(auth -> auth
                // 헬스 체크, Swagger, 인증 관련 경로는 모두 허용
                .requestMatchers("/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                // 테스트용 산책 API 접근 허용 (요청사항 반영)
                .requestMatchers("/api/v1/walk/end").permitAll()
                // 아두이노(기기) 통신용 API 인증 생략 (403 에러 방지)
                .requestMatchers("/api/v1/pet/**").permitAll()
                // 푸시 알림 수동 테스트 API 허용
                .requestMatchers("/api/v1/fcm/**").permitAll()
                // 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 기본적인 CORS 허용 정책을 설정합니다.
     */
    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.addAllowedOriginPattern("*"); // 모든 도메인 허용 (개발 단계)
        configuration.addAllowedMethod("*"); // 모든 HTTP 메서드 허용
        configuration.addAllowedHeader("*"); // 모든 헤더 허용
        configuration.setAllowCredentials(true);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
