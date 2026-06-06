package com.petready.backend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(OpenAPI) 문서화를 위한 설정 클래스입니다.
 * SpringDoc 라이브러리를 사용하여 API 명세서를 자동으로 생성합니다.
 */
@Configuration
public class SwaggerConfig {

    /**
     * OpenAPI 설정을 정의하는 Bean입니다.
     * 프로젝트 제목, 설명, 버전 등의 정보를 API 문서 상단에 노출합니다.
     * JWT 인증 방식을 연동하여 Swagger UI에서 인증된 요청을 전송할 수 있도록 설정합니다.
     * 
     * @return 설정된 OpenAPI 객체
     */
    @Bean
    public OpenAPI openAPI() {
        String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .addServersItem(new io.swagger.v3.oas.models.servers.Server().url("http://220.67.0.147:8080").description("안드로이드 연동 테스트 서버"))
                .addServersItem(new io.swagger.v3.oas.models.servers.Server().url("http://localhost:8080").description("로컬 개발 서버"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .info(new Info()
                        .title("Pet-Ready Backend API")
                        .description("반려견 양육 시뮬레이터 '펫-레디' 백엔드 API 명세서입니다.")
                        .version("v0.0.1"));
    }
}
