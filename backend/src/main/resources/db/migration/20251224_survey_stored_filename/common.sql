-- Migration: Add stored_file_name column to open_api_survey table
-- Date: 2025-12-24
-- Description: 파일 업로드 시 UUID 해시 파일명 저장용 컬럼 추가
--              receivedFileName: 원본 파일명 (다운로드 시 사용)
--              storedFileName: 해시된 파일명 (서버 저장용)

-- CUBRID / MySQL / PostgreSQL 공통
ALTER TABLE open_api_survey ADD COLUMN stored_file_name VARCHAR(255);

-- 기존 데이터 마이그레이션 (receivedFileName이 있는 경우 storedFileName 생성 필요)
-- 기존 파일은 survey_{id}__{filename} 형태로 저장되어 있으므로 수동 마이그레이션 필요
-- 아래 주석 해제 후 실행 (MySQL 예시)
-- UPDATE open_api_survey
-- SET stored_file_name = CONCAT('survey_', id, '__', received_file_name)
-- WHERE received_file_name IS NOT NULL AND stored_file_name IS NULL;
