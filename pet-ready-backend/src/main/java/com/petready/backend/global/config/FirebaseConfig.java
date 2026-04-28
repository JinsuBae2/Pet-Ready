package com.petready.backend.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

/**
 * Firebase Admin SDK를 초기화하는 설정 클래스입니다.
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${fcm.key-path}")
    private String fcmKeyPath;

    @PostConstruct
    public void initialize() {
        try {
            ClassPathResource resource = new ClassPathResource(fcmKeyPath);
            // 키 파일이 없을 경우 예외가 발생하므로, 존재하는 경우에만 초기화하도록 예외 처리
            if (resource.exists()) {
                InputStream serviceAccount = resource.getInputStream();
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                    log.info("Firebase application has been initialized.");
                }
            } else {
                log.warn("Firebase key file does not exist at {}. FCM will not work.", fcmKeyPath);
            }
        } catch (Exception e) {
            log.error("Failed to initialize Firebase: ", e);
        }
    }
}
