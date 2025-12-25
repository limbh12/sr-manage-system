-- Rollback: Remove stored_file_name column from open_api_survey table
-- Date: 2025-12-24

-- 주의: 롤백 전 기존 파일명 형식으로 되돌리는 작업이 필요할 수 있음
-- 새 UUID 형식으로 저장된 파일은 수동으로 이름 변경 필요

ALTER TABLE open_api_survey DROP COLUMN stored_file_name;
