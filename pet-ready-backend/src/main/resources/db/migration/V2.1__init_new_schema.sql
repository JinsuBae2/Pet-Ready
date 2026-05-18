-- V2.1 고도화 명세: 실시간 가감점제 및 공공데이터 구조동물 분석 기능 스키마 (MariaDB/MySQL 호환)

-- 1. 기기별 실시간 현재 점수 테이블
CREATE TABLE IF NOT EXISTS real_time_scores (
    device_id VARCHAR(50) NOT NULL COMMENT 'IoT 기기 고유 ID (PK, FK)',
    current_score INT NOT NULL DEFAULT 100 COMMENT '현재 실시간 점수 (0~100 사이 유지)',
    last_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '점수 최종 수정 시각',
    PRIMARY KEY (device_id),
    CONSTRAINT fk_real_time_score_device FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE
) COMMENT = '기기별 실시간 현재 점수를 저장하는 테이블';

-- 2. 점수 변동 이벤트 로그 테이블
CREATE TABLE IF NOT EXISTS score_events (
    id BIGINT AUTO_INCREMENT NOT NULL COMMENT '로그 고유 번호 (PK)',
    device_id VARCHAR(50) NOT NULL COMMENT 'IoT 기기 고유 ID (FK)',
    event_type VARCHAR(50) NOT NULL COMMENT '이벤트 타입 코드 (예: MISSION_FAST_COMPLETE 등)',
    delta INT NOT NULL COMMENT '점수 변동값 (부호 포함)',
    score_after INT NOT NULL COMMENT '변동 적용 후 최종 점수',
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '이벤트 발생 시각',
    PRIMARY KEY (id),
    CONSTRAINT fk_score_event_device FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE CASCADE
) COMMENT = '실시간 점수 변동 이벤트 히스토리를 기록하는 테이블';

-- 3. 구조동물 현황 캐시 테이블 (공공데이터)
CREATE TABLE IF NOT EXISTS rescue_animals_cache (
    id BIGINT AUTO_INCREMENT NOT NULL COMMENT '캐시 고유 번호 (PK)',
    animal_id VARCHAR(50) NOT NULL COMMENT '공공데이터 API 제공동물 고유 ID',
    species VARCHAR(50) COMMENT '축종 (개, 고양이 등)',
    breed VARCHAR(100) COMMENT '품종 명칭',
    age VARCHAR(50) COMMENT '동물 나이',
    shelter_name VARCHAR(100) COMMENT '관할 보호소명',
    region VARCHAR(50) COMMENT '구조 지역 (시도 단위)',
    image_url VARCHAR(500) COMMENT '동물 사진 URL',
    rescue_date DATE COMMENT '구조 일자',
    cached_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '데이터 수집 및 적재 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_animal_id (animal_id)
) COMMENT = '공공데이터 API에서 수집한 구조동물 현황 정보를 저장하는 캐시 테이블';

-- 4. AI 분석 결과 저장 테이블
CREATE TABLE IF NOT EXISTS user_analysis_results (
    id BIGINT AUTO_INCREMENT NOT NULL COMMENT '결과 고유 번호 (PK)',
    user_id BIGINT NOT NULL COMMENT '사용자 고유 번호 (FK)',
    user_type VARCHAR(50) NOT NULL COMMENT 'AI 판별 사용자 유형 코드 (예: READY_ACTIVE 등)',
    user_type_label VARCHAR(100) NOT NULL COMMENT '화면 표시용 유형 설명 문구',
    breed_type VARCHAR(50) COMMENT '추천 반려동물 유형 (소형견, 중형견 등)',
    breed_examples VARCHAR(255) COMMENT '추천 대표 품종 예시',
    breed_reason TEXT COMMENT '해당 품종 추천 이유 상세 설명',
    context_message TEXT COMMENT '사회적 맥락 교육용 텍스트 메시지',
    analyzed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'AI 분석 수행 시각',
    PRIMARY KEY (id),
    CONSTRAINT fk_user_analysis_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) COMMENT = '사용자의 활동 패턴에 대한 AI 분석 및 반려동물 추천 결과 테이블';
