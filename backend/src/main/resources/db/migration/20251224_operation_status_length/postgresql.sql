-- Migration: Increase operation_status column length
-- Date: 2025-12-24
-- PostgreSQL용

ALTER TABLE open_api_survey ALTER COLUMN operation_status TYPE VARCHAR(30);
