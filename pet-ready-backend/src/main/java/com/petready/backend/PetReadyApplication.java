package com.petready.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 펫-레디(Pet-Ready) 백엔드 애플리케이션의 메인 진입 클래스입니다.
 * Spring Boot 애플리케이션을 초기화하고 실행하는 역할을 합니다.
 */
@EnableJpaAuditing
@SpringBootApplication
public class PetReadyApplication {

    /**
     * 애플리케이션의 시작점인 main 메서드입니다.
     * @param args 실행 시 전달되는 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(PetReadyApplication.class, args);
    }

}
