package com.petready.backend.domain.medical.batch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petready.backend.domain.medical.entity.MedicalFeeCache;
import com.petready.backend.domain.medical.repository.MedicalFeeCacheRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 서버 시작 시 동물병원 진료비 정적 데이터를 초기화하는 배치 컴포넌트입니다.
 * 공공데이터 연동 또는 JSON 로드 실패 시 전국 평균값을 하드코딩 Fallback으로 삽입합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalFeeDataInitializer {

    private final MedicalFeeCacheRepository medicalFeeCacheRepository;
    private final ObjectMapper objectMapper;

    /**
     * 의존성 주입이 완료된 후 자동으로 실행되는 초기화 메서드입니다.
     */
    @PostConstruct
    public void initialize() {
        LocalDate today = LocalDate.now();

        // 1. 당일 데이터가 이미 존재한다면 중복 적재를 Skip합니다.
        if (medicalFeeCacheRepository.existsByTargetDate(today)) {
            log.info("[의료비 초기화] 오늘 날짜({})의 진료비 데이터가 이미 존재하여 Skip합니다.", today);
            return;
        }

        List<MedicalFeeCache> cacheList = new ArrayList<>();

        // 2. medical_fee.json 파일 로드 및 파싱 시도
        try {
            ClassPathResource resource = new ClassPathResource("data/medical_fee.json");
            InputStream inputStream = resource.getInputStream();

            // JSON 구조를 List<Map<String, Object>> 형태로 가정하고 파싱합니다.
            List<Map<String, Object>> rawDataList = objectMapper.readValue(inputStream, new TypeReference<>() {});

            for (Map<String, Object> data : rawDataList) {
                String itemName = (String) data.get("item_name");
                BigDecimal feeAmount = new BigDecimal(data.get("fee_amount").toString());

                MedicalFeeCache cache = MedicalFeeCache.builder()
                        .itemName(itemName)
                        .feeAmount(feeAmount)
                        .targetDate(today)
                        .build();
                cacheList.add(cache);
            }

            log.info("[의료비 초기화] medical_fee.json 파일에서 {}건의 데이터를 성공적으로 로드했습니다.", cacheList.size());

        } catch (Exception e) {
            // [방어 로직] 파일 누락, 권한 오류, 파싱 에러 발생 시 Fallback 데이터 주입
            log.warn("[의료비 초기화 경고] medical_fee.json 파일을 읽을 수 없습니다. 원인: {}. 전국 평균값 Fallback 모드를 작동합니다.", e.getMessage());
            
            cacheList.add(MedicalFeeCache.builder()
                    .itemName("초진료")
                    .feeAmount(new BigDecimal("10840.00"))
                    .targetDate(today)
                    .build());

            cacheList.add(MedicalFeeCache.builder()
                    .itemName("재진료")
                    .feeAmount(new BigDecimal("8550.00"))
                    .targetDate(today)
                    .build());
            
            log.info("[의료비 초기화] Fallback 모드로 초진료(10,840원), 재진료(8,550원)를 기본 등록했습니다.");
        }

        // 3. 파싱된 데이터(또는 Fallback 데이터)를 DB에 일괄 저장합니다.
        if (!cacheList.isEmpty()) {
            medicalFeeCacheRepository.saveAll(cacheList);
            log.info("[의료비 초기화 완료] 총 {}건의 진료비 데이터가 DB에 적재되었습니다.", cacheList.size());
        }
    }
}
