-- Migration: Increase operation_status column length
-- Date: 2025-12-24
-- H2 Database용

ALTER TABLE open_api_survey ALTER COLUMN operation_status VARCHAR(30) NOT NULL;
