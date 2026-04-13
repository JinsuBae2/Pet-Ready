package com.petready.backend.domain.health.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 서버의 상태를 확인하기 위한 상태 체크 컨트롤러입니다.
 * 로드 밸런서나 모니터링 시스템에서 서버의 가동 여부를 판단할 때 사용됩니다.
 */
@Tag(name = "Health Check", description = "서버 상태 확인 API")
@RestController
public class HealthController {

    /**
     * 서버가 정상적으로 구동 중인지 확인하는 GET API입니다.
     * 
     * @return 서버 상태 정보를 담은 Map 객체와 HTTP 200 OK 응답
     */
    @Operation(summary = "서버 상태 확인", description = "서버가 현재 동작 중인지 확인합니다.")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "message", "Pet-Ready Backend Server is running."
        ));
    }
}
