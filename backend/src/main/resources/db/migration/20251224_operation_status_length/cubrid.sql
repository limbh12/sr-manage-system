-- Migration: Increase operation_status column length
-- Date: 2025-12-24
-- CUBRID용

ALTER TABLE open_api_survey MODIFY COLUMN operation_status VARCHAR(30) NOT NULL DEFAULT 'OPERATING';
