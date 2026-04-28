package com.petready.backend.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.petready.backend.global.enums.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * FCM(Firebase Cloud Messaging) 푸시 알림 발송을 담당하는 서비스입니다.
 */
@Slf4j
@Service
public class FcmNotificationService {

    /**
     * 특정 기기 토큰으로 푸시 알림을 발송합니다.
     *
     * @param targetToken      수신 대상의 FCM 토큰
     * @param title            알림 제목
     * @param body             알림 본문
     * @param notificationType 알림 종류(BARKING, FEEDING, MEDICAL 등)
     */
    public void sendNotification(String targetToken, String title, String body, NotificationType notificationType) {
        if (targetToken == null || targetToken.isEmpty()) {
            log.warn("FCM 토큰이 없어 알림({})을 보낼 수 없습니다.", notificationType);
            return;
        }

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setToken(targetToken)
                    .setNotification(notification)
                    .putData("type", notificationType.name())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM 알림 발송 성공 [{}] response: {}", notificationType, response);
            
        } catch (Exception e) {
            log.error("FCM 알림 발송 중 오류 발생 [{}]: {}", notificationType, e.getMessage());
            // 시스템 동작 자체를 막지 않기 위해 예외를 밖으로 던지지 않습니다.
        }
    }
}
