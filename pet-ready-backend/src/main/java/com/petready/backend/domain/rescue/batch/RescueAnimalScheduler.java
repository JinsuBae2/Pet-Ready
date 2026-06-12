package com.petready.backend.domain.rescue.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petready.backend.domain.rescue.entity.RescueAnimalCache;
import com.petready.backend.domain.rescue.repository.RescueAnimalCacheRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 매일 오전 3시에 data.go.kr 공공데이터 API를 호출하여
 * 구조동물 현황 정보를 수집하고 캐시 테이블을 업데이트하는 배치 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RescueAnimalScheduler {

    private final RescueAnimalCacheRepository rescueAnimalCacheRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${public-data.api-url}")
    private String apiUrl;

    @Value("${public-data.api-key}")
    private String apiKey;

    /**
     * 서버 구동 즉시 동작하는 안전망 메소드입니다.
     * DB가 비어 있는 경우 20마리의 Mock 데이터를 즉시 채웁니다.
     * 이후 백그라운드 스레드를 통해 실시간 API 수집 테스트를 수행합니다.
     */
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void init() {
        log.info("========== [구조동물 스케줄러 초기화 시작] ==========");
        
        long count = rescueAnimalCacheRepository.count();
        if (count == 0) {
            log.info("[구조동물 스케줄러] 현재 DB가 비어 있으므로 Fallback Mock 데이터 20마리를 즉시 적재합니다.");
            executeRobustFallback();
        } else {
            log.info("[구조동물 스케줄러] 이미 DB에 {}건의 캐시 데이터가 존재합니다. 초기 적재를 건너뜁니다.", count);
        }

        // 실시간 API 동작성 검증을 위해 백그라운드 스레드에서 비동기로 수집 시도
        new Thread(() -> {
            try {
                log.info("[구조동물 스케줄러] 실시간 공공데이터 API 연동 및 데이터 동기화 시도 중...");
                fetchAndCacheRescueAnimals();
            } catch (Exception e) {
                log.error("[구조동물 스케줄러] 실시간 API 수집 실패 (정상 동작 중이며 기존 데이터를 유지합니다) - 사유: {}", e.getMessage());
            }
        }).start();

        log.info("========== [구조동물 스케줄러 초기화 종료] ==========");
    }

    /**
     * 매일 오전 3시에 자동으로 구동되어 공공데이터 API로부터 구조동물 정보를 수집합니다.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void scheduledFetch() {
        log.info("[구조동물 스케줄러] 오전 3시 배치 기동: 실시간 구조동물 정보 갱신을 진행합니다.");
        try {
            fetchAndCacheRescueAnimals();
        } catch (Exception e) {
            log.error("[구조동물 스케줄러] 오전 3시 배치 작업 실패 - 폴백 모드를 실행합니다. 사유: {}", e.getMessage());
            executeRobustFallback();
        }
    }

    /**
     * 공공데이터 API에서 JSON 데이터를 가져와 파싱 후 DB에 적재합니다.
     */
    public void fetchAndCacheRescueAnimals() throws Exception {
        // serviceKey 파라미터가 자동으로 이중 인코딩되는 문제를 방지하기 위해 java.net.URI 객체를 직접 조립하여 호출합니다.
        // upkind=417000 파라미터를 명시하여 100% 개(강아지) 유기동물 데이터만 필터링 수집합니다.
        String urlString = apiUrl + "?serviceKey=" + apiKey + "&_type=json&numOfRows=100&upkind=417000";
        URI uri = new URI(urlString);

        log.info("[구조동물 스케줄러] API 요청 URL: {}", apiUrl);
        String response = restTemplate.getForObject(uri, String.class);

        if (response == null || response.trim().isEmpty()) {
            throw new RuntimeException("API 서버 응답이 비어 있습니다.");
        }

        JsonNode root = objectMapper.readTree(response);
        JsonNode itemNode = root.path("response").path("body").path("items").path("item");

        if (itemNode.isMissingNode() || itemNode.isNull()) {
            JsonNode header = root.path("response").path("header");
            String resultCode = header.path("resultCode").asText();
            String resultMsg = header.path("resultMsg").asText();
            throw new RuntimeException("API 결과 정상 처리 실패 (resultCode: " + resultCode + ", msg: " + resultMsg + ")");
        }

        List<JsonNode> items = new ArrayList<>();
        if (itemNode.isArray()) {
            for (JsonNode item : itemNode) {
                items.add(item);
            }
        } else if (itemNode.isObject()) {
            items.add(itemNode);
        }

        if (items.isEmpty()) {
            log.warn("[구조동물 스케줄러] API로부터 수집된 유기동물 목록이 존재하지 않습니다.");
            return;
        }

        int successCount = 0;
        int updateCount = 0;

        for (JsonNode item : items) {
            try {
                String animalId = item.path("desertionNo").asText();
                if (animalId == null || animalId.trim().isEmpty()) {
                    continue;
                }

                String popfile = item.path("popfile").asText();
                String happenDt = item.path("happenDt").asText();
                
                // 공식 품종 명칭인 kindNm 파싱 (없을 경우 kindCd를 Fallback으로 사용)
                String kindNm = item.path("kindNm").asText();
                if (kindNm == null || kindNm.trim().isEmpty()) {
                    kindNm = item.path("kindCd").asText("믹스견");
                }
                
                String age = item.path("age").asText("알 수 없음");
                String careNm = item.path("careNm").asText("알 수 없음");
                String orgNm = item.path("orgNm").asText("");

                // 1. 접수일 날짜 변환 (yyyyMMdd -> LocalDate)
                LocalDate rescueDate;
                try {
                    rescueDate = LocalDate.parse(happenDt, DateTimeFormatter.ofPattern("yyyyMMdd"));
                } catch (Exception e) {
                    rescueDate = LocalDate.now();
                }

                // 2. 품종 및 축종 문자열 가공 (개 전용)
                String species = "개";
                String breed = kindNm;
                if (kindNm.startsWith("[개]")) {
                    breed = kindNm.substring(3).trim();
                } else if (kindNm.startsWith("[")) {
                    int closeBracket = kindNm.indexOf("]");
                    if (closeBracket > 0) {
                        breed = kindNm.substring(closeBracket + 1).trim();
                    }
                }

                // 공공데이터 고유 숫자 분류 코드가 반환되는 예외 상황에 대한 안전장치 매핑
                if ("000114".equals(breed) || breed.contains("000114")) {
                    breed = "믹스견";
                } else if ("000072".equals(breed) || breed.contains("000072")) {
                    breed = "진도개";
                } else if ("000054".equals(breed) || breed.contains("000054")) {
                    breed = "말티즈";
                } else if ("000212".equals(breed) || breed.contains("000212")) {
                    breed = "시추";
                } else if (breed != null && breed.matches("\\d+")) {
                    breed = "믹스견";
                }

                // 3. 지역 명칭 파싱
                String region = "전국";
                if (!orgNm.trim().isEmpty()) {
                    region = orgNm.split(" ")[0];
                }

                // 4. 이미지 경로(popfile) 유효성 검증 및 Fallback 바인딩
                String imageUrl = popfile;
                boolean isFallback = false;
                if (imageUrl == null || imageUrl.trim().isEmpty() || !imageUrl.startsWith("http")) {
                    imageUrl = getBreedFallbackImage(breed);
                    isFallback = true;
                }

                // 5. DB에 Upsert
                final String finalSpecies = species;
                final String finalBreed = breed;
                final String finalRegion = region;
                final LocalDate finalRescueDate = rescueDate;
                final String finalAge = age;
                final String finalCareNm = careNm;
                final String finalImageUrl = imageUrl;
                final boolean finalIsFallback = isFallback;
                
                boolean isNew = rescueAnimalCacheRepository.findByAnimalId(animalId).map(existing -> {
                    RescueAnimalCache updated = RescueAnimalCache.builder()
                            .id(existing.getId())
                            .animalId(animalId)
                            .species(finalSpecies)
                            .breed(finalBreed)
                            .age(finalAge)
                            .shelterName(finalCareNm)
                            .region(finalRegion)
                            .imageUrl(finalImageUrl)
                            .isFallback(finalIsFallback)
                            .rescueDate(finalRescueDate)
                            .cachedAt(LocalDateTime.now())
                            .build();
                    rescueAnimalCacheRepository.save(updated);
                    return false;
                }).orElseGet(() -> {
                    RescueAnimalCache inserted = RescueAnimalCache.builder()
                            .animalId(animalId)
                            .species(finalSpecies)
                            .breed(finalBreed)
                            .age(finalAge)
                            .shelterName(finalCareNm)
                            .region(finalRegion)
                            .imageUrl(finalImageUrl)
                            .isFallback(finalIsFallback)
                            .rescueDate(finalRescueDate)
                            .cachedAt(LocalDateTime.now())
                            .build();
                    rescueAnimalCacheRepository.save(inserted);
                    return true;
                });

                if (isNew) {
                    successCount++;
                } else {
                    updateCount++;
                }

            } catch (Exception e) {
                log.error("[구조동물 스케줄러] 개별 항목 적재 중 오류 발생 - 건너뜁니다: {}", e.getMessage());
            }
        }

        log.info("[구조동물 스케줄러] 동기화 결과 - 신규 등록: {}건, 기존 업데이트: {}건", successCount, updateCount);
    }

    /**
     * 통신 장애 혹은 예외 발생 시 안전하게 20마리의 Mock 데이터를 DB에 바인딩하는 복구 메서드입니다.
     */
    public void executeRobustFallback() {
        log.info("[구조동물 스케줄러] Robust Fallback 모드를 실행합니다. 가상 유기동물 데이터 20건 적재를 시작합니다.");
        
        List<RescueAnimalCache> mockAnimals = new ArrayList<>();
        
        mockAnimals.add(createMockAnimal("F01", "개", "골든리트리버", "2022(년생)", "서울유기동물보호센터", "서울특별시", getBreedFallbackImage("골든리트리버"), LocalDate.now().minusDays(1)));
        mockAnimals.add(createMockAnimal("F02", "개", "라브라도리트리버", "2021(년생)", "경기반려동물입양센터", "경기도", getBreedFallbackImage("라브라도리트리버"), LocalDate.now().minusDays(2)));
        mockAnimals.add(createMockAnimal("F03", "개", "비글", "2023(년생)", "인천동물보호소", "인천광역시", getBreedFallbackImage("비글"), LocalDate.now().minusDays(3)));
        mockAnimals.add(createMockAnimal("F04", "개", "푸들", "2020(년생)", "서울유기동물보호센터", "서울특별시", getBreedFallbackImage("푸들"), LocalDate.now().minusDays(1)));
        mockAnimals.add(createMockAnimal("F05", "개", "말티즈", "2022(년생)", "부산동물사랑보호센터", "부산광역시", getBreedFallbackImage("말티즈"), LocalDate.now().minusDays(4)));
        mockAnimals.add(createMockAnimal("F06", "개", "비숑프리제", "2023(년생)", "경기반려동물입양센터", "경기도", getBreedFallbackImage("비숑프리제"), LocalDate.now().minusDays(5)));
        mockAnimals.add(createMockAnimal("F07", "개", "시추", "2019(년생)", "강원유기동물쉼터", "강원특별자치도", getBreedFallbackImage("시추"), LocalDate.now().minusDays(2)));
        mockAnimals.add(createMockAnimal("F08", "개", "포메라니안", "2024(년생)", "인천동물보호소", "인천광역시", getBreedFallbackImage("포메라니안"), LocalDate.now().minusDays(1)));
        mockAnimals.add(createMockAnimal("F09", "개", "시바이누", "2021(년생)", "충남유기견보호협회", "충청남도", getBreedFallbackImage("시바이누"), LocalDate.now().minusDays(6)));
        mockAnimals.add(createMockAnimal("F10", "개", "진도개", "2020(년생)", "전남동물구호센터", "전라남도", getBreedFallbackImage("진도개"), LocalDate.now().minusDays(7)));
        
        mockAnimals.add(createMockAnimal("F11", "개", "치와와", "2022(년생)", "서울유기동물보호센터", "서울특별시", getBreedFallbackImage("치와와"), LocalDate.now().minusDays(1)));
        mockAnimals.add(createMockAnimal("F12", "개", "웰시코기", "2021(년생)", "경기반려동물입양센터", "경기도", getBreedFallbackImage("웰시코기"), LocalDate.now().minusDays(2)));
        mockAnimals.add(createMockAnimal("F13", "개", "닥스훈트", "2023(년생)", "인천동물보호소", "인천광역시", getBreedFallbackImage("닥스훈트"), LocalDate.now().minusDays(3)));
        mockAnimals.add(createMockAnimal("F14", "개", "퍼그", "2022(년생)", "경북동물구조협회", "경상북도", getBreedFallbackImage("퍼그"), LocalDate.now().minusDays(4)));
        mockAnimals.add(createMockAnimal("F15", "개", "웰시코기", "2021(년생)", "충남유기견보호협회", "충청남도", getBreedFallbackImage("웰시코기"), LocalDate.now().minusDays(5)));
        mockAnimals.add(createMockAnimal("F16", "개", "믹스견", "2023(년생)", "전남동물구호센터", "전라남도", getBreedFallbackImage("믹스견"), LocalDate.now().minusDays(6)));
        mockAnimals.add(createMockAnimal("F17", "개", "요크셔테리어", "2020(년생)", "부산동물사랑보호센터", "부산광역시", getBreedFallbackImage("요크셔테리어"), LocalDate.now().minusDays(3)));
        mockAnimals.add(createMockAnimal("F18", "개", "치와와", "2022(년생)", "서울유기동물보호센터", "서울특별시", getBreedFallbackImage("치와와"), LocalDate.now().minusDays(1)));
        mockAnimals.add(createMockAnimal("F19", "개", "닥스훈트", "2021(년생)", "경기반려동물입양센터", "경기도", getBreedFallbackImage("닥스훈트"), LocalDate.now().minusDays(8)));
        mockAnimals.add(createMockAnimal("F20", "개", "진도개", "2022(년생)", "부산동물사랑보호센터", "부산광역시", getBreedFallbackImage("진도개"), LocalDate.now().minusDays(4)));

        for (RescueAnimalCache mock : mockAnimals) {
            if (!rescueAnimalCacheRepository.existsByAnimalId(mock.getAnimalId())) {
                rescueAnimalCacheRepository.save(mock);
            }
        }
        log.info("[구조동물 스케줄러] Robust Fallback Mock 데이터 20건 적재 완료.");
    }

    private RescueAnimalCache createMockAnimal(String suffix, String species, String breed, String age, String shelterName, String region, String imageUrl, LocalDate rescueDate) {
        String animalId = "MOCK_2026_" + suffix;
        return RescueAnimalCache.builder()
                .animalId(animalId)
                .species(species)
                .breed(breed)
                .age(age)
                .shelterName(shelterName)
                .region(region)
                .imageUrl(imageUrl)
                .isFallback(true) // Mock 데이터는 무조건 fallback 이미지
                .rescueDate(rescueDate)
                .cachedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 품종명을 분석하여 미리 백엔드 정적 리소스에 저장한 오프라인 Fallback 이미지 URL을 반환합니다.
     * 공공 API 문자열에서 다양한 조건(contains)을 유연하게 수용합니다.
     */
    private String getBreedFallbackImage(String breed) {
        String baseUrl = "http://220.67.0.11:8080";
        if (breed == null || breed.trim().isEmpty()) {
            return baseUrl + "/images/fallback/mix.png";
        }
        
        String cleanBreed = breed.replaceAll("\\s+", "").toLowerCase();
        
        if (cleanBreed.contains("푸들")) {
            return baseUrl + "/images/fallback/poodle.png";
        } else if (cleanBreed.contains("진도")) {
            return baseUrl + "/images/fallback/jindo.png";
        } else if (cleanBreed.contains("리트리버") || cleanBreed.contains("라브라도")) {
            return baseUrl + "/images/fallback/retriever.png";
        } else if (cleanBreed.contains("말티즈")) {
            return baseUrl + "/images/fallback/maltese.png";
        }
        
        // 그 외 매칭되지 않거나 믹스견은 기본 믹스견 이미지 반환
        return baseUrl + "/images/fallback/mix.png";
    }
}
