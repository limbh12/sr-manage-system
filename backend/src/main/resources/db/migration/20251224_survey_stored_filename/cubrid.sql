-- Migration: Add stored_file_name column to open_api_survey table
-- Date: 2025-12-24
-- CUBRID용

ALTER TABLE open_api_survey ADD COLUMN stored_file_name VARCHAR(255);

-- 기존 데이터 마이그레이션
UPDATE open_api_survey
SET stored_file_name = CONCAT('survey_', CAST(id AS VARCHAR), '__', received_file_name)
WHERE received_file_name IS NOT NULL AND stored_file_name IS NULL;
