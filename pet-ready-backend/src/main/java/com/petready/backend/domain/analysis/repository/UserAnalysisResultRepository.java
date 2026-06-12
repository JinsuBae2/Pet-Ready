package com.petready.backend.domain.analysis.repository;

import com.petready.backend.domain.analysis.entity.UserAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.petready.backend.domain.user.entity.User;
import java.util.Optional;

/**
2.1 양육 준비도 분석 결과 조회를 담당하는 JPA 리포지토리입니다.
 */
@Repository
public interface UserAnalysisResultRepository extends JpaRepository<UserAnalysisResult, Long> {

    /**
     * 특정 사용자의 분석 결과를 삭제합니다.
     */
    void deleteByUser(User user);

    /**
     * 사용자의 이메일을 기준으로 가장 최근에 수행된 분석 결과를 조회합니다.
     *
     * @param email 사용자 이메일
     * @return 최신 분석 결과 (Optional)
     */
    Optional<UserAnalysisResult> findFirstByUserEmailOrderByAnalyzedAtDesc(String email);
}
