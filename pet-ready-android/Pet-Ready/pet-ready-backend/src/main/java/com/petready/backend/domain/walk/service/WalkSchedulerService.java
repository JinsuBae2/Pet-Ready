package com.petready.backend.domain.walk.service;

import com.petready.backend.domain.device.entity.Device;
import com.petready.backend.domain.device.repository.DeviceRepository;
import com.petready.backend.domain.score.service.ScoreService;
import com.petready.backend.domain.walk.repository.WalkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 매일 자정에 산책 기록을 결산하여 패널티를 부여하는 스케줄러입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalkSchedulerService {

    private final DeviceRepository deviceRepository;
    private final WalkRepository walkRepository;
    private final ScoreService scoreService;

    /**
     * BK-07: 매일 자정(00:00:00)에 자동으로 도는 배치 스케줄러.
     * 당일 날짜로 산책 기록이 전혀 없는 기기들을 찾아 실시간 점수 -5점 감점 패널티를 부여합니다.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processMidnightWalkPenalty() {
        log.info("자정 산책 결산 배치를 시작합니다.");

        // 어제 날짜 기준으로 검사 (자정에 도는 스케줄러이므로, 전날 00:00:00 ~ 23:59:59를 확인해야 함)
        // 만약 밤 11시 59분에 돈다면 당일이 맞지만, "자정(00:00)"이라면 전날 기록을 검사하는 것이 논리적으로 맞습니다.
        // 스펙상 "당일 날짜로 기록이 없는" 인데 00:00:00 기준이면 이미 날짜가 넘어간 직후이므로 어제(yesterday)를 기준으로 합니다.
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime startOfDay = yesterday.atStartOfDay();
        LocalDateTime endOfDay = yesterday.atTime(23, 59, 59, 999999999);

        List<Device> devices = deviceRepository.findAll();

        int penaltyCount = 0;
        for (Device device : devices) {
            long walkCount = walkRepository.countByDeviceDeviceIdAndStartedAtBetween(device.getDeviceId(), startOfDay, endOfDay);
            
            if (walkCount == 0) {
                // 하루 종일 산책을 안 한 경우 -5점 패널티 부여
                scoreService.processScoreEvent(device.getDeviceId(), "WALK_NONE", -5, yesterday.toString() + " 산책 미실시 패널티");
                penaltyCount++;
                log.info("기기 [{}] 어제 산책 0회 - 패널티 부과", device.getDeviceId());
            }
        }

        log.info("자정 산책 결산 배치 종료. 총 {}대의 기기에 패널티가 부여되었습니다.", penaltyCount);
    }
}
