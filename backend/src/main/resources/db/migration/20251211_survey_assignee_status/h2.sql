-- Migration for H2 Database
-- Add assignee and status columns to open_api_survey table
-- Date: 2025-12-11

ALTER TABLE open_api_survey ADD COLUMN IF NOT EXISTS assignee_id BIGINT;
ALTER TABLE open_api_survey ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PENDING' NOT NULL;

-- FK 제약조건 추가
-- ALTER TABLE open_api_survey ADD CONSTRAINT IF NOT EXISTS fk_survey_assignee FOREIGN KEY (assignee_id) REFERENCES users(id);
