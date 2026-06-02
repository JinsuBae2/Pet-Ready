package com.petready.backend.domain.rescue;

import com.petready.backend.domain.rescue.batch.RescueAnimalScheduler;
import com.petready.backend.domain.rescue.entity.RescueAnimalCache;
import com.petready.backend.domain.rescue.repository.RescueAnimalCacheRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * RescueAnimalScheduler의 데이터 적재 및 예외 복구(Fallback) 동작을 검증하는 단위 테스트 클래스입니다.
 */
@ExtendWith(MockitoExtension.class)
public class RescueAnimalSchedulerTest {

    @Mock
    private RescueAnimalCacheRepository rescueAnimalCacheRepository;

    @InjectMocks
    private RescueAnimalScheduler rescueAnimalScheduler;

    /**
     * executeRobustFallback() 호출 시 20마리의 가상 데이터가 
     * 중복 검사를 거쳐 정상적으로 저장되는지 검증합니다.
     */
    @Test
    void testRobustFallback() {
        // 모든 유기번호에 대해 기존 데이터가 존재하지 않는 상황 모의
        when(rescueAnimalCacheRepository.existsByAnimalId(anyString())).thenReturn(false);

        // Fallback 실행
        rescueAnimalScheduler.executeRobustFallback();

        // 20마리 저장 확인
        verify(rescueAnimalCacheRepository, times(20)).save(any(RescueAnimalCache.class));
        System.out.println("✅ [RescueAnimalSchedulerTest] Fallback 가상 유기동물 20건 적재 완료 검증 성공!");
    }
}
