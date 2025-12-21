-- Migration: Add contact_position column to open_api_survey table
-- Date: 2025-12-11
-- Description: OPEN API 현황조사 담당자 직급 필드 추가

-- CUBRID / MySQL / PostgreSQL 공통
ALTER TABLE open_api_survey ADD COLUMN contact_position VARCHAR(50);

-- 인덱스는 필요 시 추가 (선택사항)
-- CREATE INDEX idx_open_api_survey_contact_position ON open_api_survey(contact_position);
