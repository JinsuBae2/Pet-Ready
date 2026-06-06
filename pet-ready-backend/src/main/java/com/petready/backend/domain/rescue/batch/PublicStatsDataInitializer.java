package com.petready.backend.domain.rescue.batch;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 프로젝트 내 정적 JSON 파일 4종(보호소, 등록통계, 설문, 가구통계)을
 * 초기화 단계에서 메모리나 캐시 테이블에 로드하기 위한 뼈대 클래스입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PublicStatsDataInitializer {

    // 로드해야 할 정적 파일 목록 정의
    private static final List<String> STATIC_JSON_FILES = List.of(
            "data/shelter_centers.json",
            "data/animal_registration_stats.json",
            "data/welfare_survey.json",
            "data/household_stats.json"
    );

    /**
     * 서버 구동 시 정적 공공데이터 파일 존재 여부를 체크하고
     * 로딩 프로세스를 시작하는 초기화 메서드입니다.
     */
    @PostConstruct
    public void initializePublicStats() {
        log.info("========== [공공데이터 정적 초기화] v2.2 고도화로 인한 ANR-08 지도 화면 삭제로 정적 파일 로딩 생략 ==========");
    }
}
