-- Migration: Add assignee and status columns to open_api_survey table
-- Date: 2025-12-11
-- Description: OPEN API 현황조사에 담당자 지정 및 작성상태 관리 기능 추가

-- CUBRID / MySQL / PostgreSQL 공통
ALTER TABLE open_api_survey ADD COLUMN assignee_id BIGINT;
ALTER TABLE open_api_survey ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

-- FK 제약조건 추가 (선택사항 - CUBRID에서는 지원하지 않을 수 있음)
-- ALTER TABLE open_api_survey ADD CONSTRAINT fk_survey_assignee FOREIGN KEY (assignee_id) REFERENCES users(id);

-- 기존 데이터에 대한 기본값 설정
UPDATE open_api_survey SET status = 'PENDING' WHERE status IS NULL;
