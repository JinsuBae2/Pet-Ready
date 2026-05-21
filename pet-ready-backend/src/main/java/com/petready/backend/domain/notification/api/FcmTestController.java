package com.petready.backend.domain.notification.api;

import com.petready.backend.domain.notification.service.FcmNotificationService;
import com.petready.backend.global.enums.NotificationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "FCM Test", description = "안드로이드 푸시 알림 테스트 전용 API")
@RestController
@RequestMapping("/api/v1/fcm")
@RequiredArgsConstructor
public class FcmTestController {

    private final FcmNotificationService fcmNotificationService;

    @Operation(summary = "푸시 알림 수동 발송 테스트", description = "입력한 FCM 토큰으로 즉시 테스트 알림을 발송합니다.")
    @PostMapping("/test")
    public ResponseEntity<String> sendTestPush(@RequestBody FcmTestRequest request) {
        fcmNotificationService.sendNotification(
                request.getToken(),
                request.getTitle(),
                request.getBody(),
                NotificationType.FEEDING // 임의로 지정
        );
        return ResponseEntity.ok("푸시 알림 발송 명령을 실행했습니다. 백엔드 콘솔과 안드로이드 기기를 확인해주세요.");
    }

    @Data
    public static class FcmTestRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "안드로이드 기기의 실제 FCM 토큰")
        private String token;
        @io.swagger.v3.oas.annotations.media.Schema(description = "알림 제목", example = "테스트 제목")
        private String title;
        @io.swagger.v3.oas.annotations.media.Schema(description = "알림 내용", example = "테스트 내용입니다!")
        private String body;
    }
}
