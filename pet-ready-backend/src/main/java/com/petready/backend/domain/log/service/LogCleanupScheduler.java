package com.petready.backend.domain.log.service;

import com.petready.backend.domain.log.repository.PetStatusLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 오래된 상태 로그 데이터 정리 스케줄러 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogCleanupScheduler {

    private final PetStatusLogRepository logRepository;

    /**
     * 매일 새벽 3시에 구동되어 3일이 경과한 상태 로그 데이터를 데이터베이스에서 삭제합니다.
     * 이를 통해 로그 테이블의 과도한 비대화를 방지하고 성능을 쾌적하게 유지합니다.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldStatusLogs() {
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        
        try {
            long deletedCount = logRepository.deleteByRecordedAtBefore(threeDaysAgo);
            if (deletedCount > 0) {
                log.info("[LogCleanupScheduler] 3일 이전의 오래된 상태 로그 {}건을 데이터베이스에서 자동 정리하였습니다.", deletedCount);
            }
        } catch (Exception e) {
            log.error("[LogCleanupScheduler] 오래된 로그 정리 중 에러 발생: ", e);
        }
    }
}
