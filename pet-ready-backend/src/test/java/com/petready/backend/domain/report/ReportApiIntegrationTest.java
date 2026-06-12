package com.petready.backend.domain.report;

import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.mission.entity.Mission;
import com.petready.backend.domain.mission.repository.MissionRepository;
import com.petready.backend.domain.rescue.entity.RescueAnimalCache;
import com.petready.backend.domain.rescue.repository.RescueAnimalCacheRepository;
import com.petready.backend.domain.score.entity.RealTimeScore;
import com.petready.backend.domain.score.repository.RealTimeScoreRepository;
import com.petready.backend.domain.user.entity.User;
import com.petready.backend.domain.user.repository.UserRepository;
import com.petready.backend.domain.walk.entity.Walk;
import com.petready.backend.domain.walk.repository.WalkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 최종 양육 리포트 API (GET /api/v1/report/final) 연동 및 AI 분석 엔진 동작을 검증하는 통합 테스트 클래스입니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // H2 인메모리 테스트 환경 실행
@Transactional
public class ReportApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private WalkRepository walkRepository;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private RealTimeScoreRepository realTimeScoreRepository;

    @Autowired
    private RescueAnimalCacheRepository rescueAnimalCacheRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private User testUser;
    private Device testDevice;

    @BeforeEach
    void setUp() {
        // 0. 캐시 리포지토리 초기화
        rescueAnimalCacheRepository.deleteAll();

        // 1. 테스트 유저 생성 및 저장
        testUser = User.builder()
                .email("test@petready.com")
                .passwordHash("encrypted_password")
                .nickname("테스터")
                .fcmToken("test_fcm_token")
                .build();
        testUser = userRepository.save(testUser);

        // 2. 테스트 기기 생성 및 저장
        testDevice = Device.builder()
                .deviceId("DEVICE_TEST_01")
                .user(testUser)
                .petName("댕댕이")
                .walkGoalKm(2.0)
                .isOnline(true)
                .sickCount(0)
                .build();
        testDevice = deviceRepository.save(testDevice);

        // 3. 기기의 실시간 점수 초기화
        RealTimeScore score = RealTimeScore.builder()
                .device(testDevice)
                .currentScore(95) // A+ 등급 조건
                .lastUpdatedAt(LocalDateTime.now())
                .build();
        realTimeScoreRepository.save(score);

        // 4. 누적 산책 데이터 추가 (달성률 100% 조건: 목표 2.0km, 실제 2.0km)
        Walk walk = Walk.builder()
                .device(testDevice)
                .user(testUser)
                .distanceKm(new BigDecimal("2.0"))
                .walkGoalKm(new BigDecimal("2.0"))
                .durationSec(3600L)
                .routeJson("[]")
                .startedAt(LocalDateTime.now().minusHours(1))
                .endedAt(LocalDateTime.now())
                .build();
        walkRepository.save(walk);

        // 5. 누적 미션 데이터 추가 (성공률 100%, 평균응답속도 10초)
        Mission mission = Mission.builder()
                .device(testDevice)
                .type("BARKING")
                .issuedAt(LocalDateTime.now().minusMinutes(10))
                .isCompleted(true)
                .respondedAt(LocalDateTime.now().minusMinutes(10).plusSeconds(10))
                .responseTimeSec(10L)
                .build();
        missionRepository.save(mission);

        // 6. 공공 구조동물 캐시 데이터 적재 (추천 매칭 테스트용)
        RescueAnimalCache retriever = RescueAnimalCache.builder()
                .animalId("DESERTION_001")
                .species("개")
                .breed("골든리트리버")
                .age("2023(년생)")
                .shelterName("서울보호센터")
                .region("서울특별시")
                .imageUrl("https://images.unsplash.com/photo-1552053831-71594a27632d")
                .rescueDate(LocalDate.now().minusDays(5))
                .cachedAt(LocalDateTime.now())
                .build();
        rescueAnimalCacheRepository.save(retriever);

        RescueAnimalCache poodle = RescueAnimalCache.builder()
                .animalId("DESERTION_002")
                .species("개")
                .breed("푸들")
                .age("2022(년생)")
                .shelterName("경기보호소")
                .region("경기도")
                .imageUrl("https://images.unsplash.com/photo-1598133185553-c2f74d175b97")
                .rescueDate(LocalDate.now().minusDays(10))
                .cachedAt(LocalDateTime.now())
                .build();
        rescueAnimalCacheRepository.save(poodle);
    }

    /**
     * 최종 리포트 API가 정상적인 점수, 등급(A+), AI 분석 유형(READY_ACTIVE) 및 매칭되는 골든리트리버 구조견 1건을
     * JSON 필드로 바르게 제공하는지 검증합니다.
     */
    @Test
    @WithMockUser(username = "test@petready.com")
    void testGetFinalReportSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/report/final")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                // 기본 성적 통계 검증
                .andExpect(jsonPath("$.finalScore", is(100)))
                .andExpect(jsonPath("$.grade", is("A+")))

                .andExpect(jsonPath("$.walkScore", is(100)))
                .andExpect(jsonPath("$.responseScore", is(100)))
                .andExpect(jsonPath("$.healthPenalty", is(0)))
                .andExpect(jsonPath("$.totalWalkKm", is(2.0)))
                .andExpect(jsonPath("$.avgResponseSec", is(10)))
                // AI 분석 결과 검증
                .andExpect(jsonPath("$.userType", is("READY_ACTIVE")))
                .andExpect(jsonPath("$.userTypeLabel", is("준비된 활동가형")))
                .andExpect(jsonPath("$.breedRecommendation.type", is("대형견 / 활동견")))
                .andExpect(jsonPath("$.breedRecommendation.examples", containsString("골든리트리버")))
                .andExpect(jsonPath("$.contextMessage", containsString("칭호")))
                // 실시간 구조견 추천 검증 (골든리트리버 1건이 매칭되어 노출)
                .andExpect(jsonPath("$.recommendedAnimals", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.recommendedAnimals[0].breed", is("골든리트리버")))
                .andExpect(jsonPath("$.recommendedAnimals[0].animalId", is("DESERTION_001")));
    }

    @Test
    @WithMockUser(username = "test@petready.com")
    void testResetSimulationSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/report/reset")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 테스트 스레드의 1차 캐시를 비워 DB의 물리적 삭제 반영 상태를 확인
        entityManager.clear();

        org.junit.jupiter.api.Assertions.assertTrue(walkRepository.findAllByUserEmail("test@petready.com").isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(missionRepository.findAllByDeviceUserEmail("test@petready.com").isEmpty());
        
        RealTimeScore score = realTimeScoreRepository.findById(testDevice.getDeviceId()).orElse(null);
        org.junit.jupiter.api.Assertions.assertNotNull(score);
        org.junit.jupiter.api.Assertions.assertEquals(100, score.getCurrentScore());
    }

    @Test
    @WithMockUser(username = "test@petready.com")
    void testWithdrawUserSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/user/withdraw")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 테스트 스레드의 1차 캐시를 비워 DB의 물리적 삭제 반영 상태를 확인
        entityManager.clear();

        org.junit.jupiter.api.Assertions.assertFalse(userRepository.findByEmail("test@petready.com").isPresent());
        org.junit.jupiter.api.Assertions.assertFalse(deviceRepository.findByUserEmail("test@petready.com").isPresent());
        org.junit.jupiter.api.Assertions.assertFalse(realTimeScoreRepository.findById(testDevice.getDeviceId()).isPresent());
    }
}
