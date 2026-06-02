package com.petready.backend.domain.user.repository;

import com.petready.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User 엔티티에 대한 데이터베이스 접근을 담당하는 리포지토리입니다.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 이메일을 통해 사용자 정보를 조회합니다.
     * 
     * @param email 조회할 이메일
     * @return 조회된 사용자 정보 (Optional)
     */
    Optional<User> findByEmail(String email);
}
