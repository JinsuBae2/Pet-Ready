-- V2.2 훈련 도메인 전면 구축 및 반려견 양육 상태 에코시스템 컬럼 마이그레이션 (MariaDB/MySQL 호환)

-- 1. 가상 훈련 결과 로그 기록 테이블
CREATE TABLE IF NOT EXISTS training_logs (
    id BIGINT AUTO_INCREMENT NOT NULL COMMENT '로그 고유 번호 (PK)',
    device_id VARCHAR(50) NOT NULL COMMENT 'IoT 기기 고유 ID (FK)',
    gesture_type VARCHAR(50) COMMENT '감지된 제스쳐 종류 (SIT, STAY 등)',
    status VARCHAR(20) NOT NULL COMMENT '훈련 결과 상태 (SUCCESS, CONFUSED, SAD)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '로그 생성 시각',
    PRIMARY KEY (id),
    CONSTRAINT fk_training_log_device FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE
) COMMENT = '가상 훈련 결과를 기록하는 테이블';

-- 2. 반려견 양육 상태 관리를 위한 기기(devices) 테이블 컬럼 추가
ALTER TABLE devices ADD COLUMN IF NOT EXISTS routine_status VARCHAR(50) NOT NULL DEFAULT 'HAPPY' COMMENT '평상시 상태 유형';
ALTER TABLE devices ADD COLUMN IF NOT EXISTS last_feed_time TIMESTAMP NULL COMMENT '마지막 급여 완료 시각';
