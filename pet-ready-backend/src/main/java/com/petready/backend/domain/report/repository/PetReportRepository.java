package com.petready.backend.domain.report.repository;

import com.petready.backend.domain.report.entity.PetReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PetReportRepository extends JpaRepository<PetReport, Long> {
    /**
     * 사용자 이메일을 통해 해당 사용자의 리포트를 조회합니다.
     */
    Optional<PetReport> findByUserEmail(String email);
}
