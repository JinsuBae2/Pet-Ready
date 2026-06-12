package com.petready.backend.domain.walk.repository;

import com.petready.backend.domain.walk.entity.Walk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.petready.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface WalkRepository extends JpaRepository<Walk, Long> {
    /**
     * 특정 사용자의 모든 산책 기록을 일괄 삭제합니다.
     */
    @Modifying
    @Query("delete from Walk w where w.user = :user")
    void deleteAllByUser(@Param("user") User user);

    /**
     * 특정 사용자의 모든 산책 기록을 조회합니다.
     */
    List<Walk> findAllByUserEmail(String email);

    /**
     * 특정 기기의 특정 기간 내 산책 기록 개수를 조회합니다.
     */
    long countByDeviceDeviceIdAndStartedAtBetween(String deviceId, java.time.LocalDateTime start, java.time.LocalDateTime end);

    /**
     * 특정 기기의 특정 시각 이후 산책 기록 목록을 조회합니다.
     */
    List<Walk> findAllByDeviceDeviceIdAndStartedAtAfter(String deviceId, java.time.LocalDateTime time);
}
