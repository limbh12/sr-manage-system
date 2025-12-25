-- Migration: Increase operation_status column length
-- Date: 2025-12-24
-- Description: SCHEDULED_DEPRECATION(21자)를 저장하기 위해 VARCHAR(20) → VARCHAR(30) 변경

-- MySQL / PostgreSQL
ALTER TABLE open_api_survey ALTER COLUMN operation_status TYPE VARCHAR(30);
