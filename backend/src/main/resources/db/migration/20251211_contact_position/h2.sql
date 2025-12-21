-- Migration for H2 Database
-- Add contact_position column to open_api_survey table
-- Date: 2025-12-11

ALTER TABLE open_api_survey ADD COLUMN IF NOT EXISTS contact_position VARCHAR(50);
