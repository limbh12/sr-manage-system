-- OPEN API 현황조사 테이블에 운영상태 컬럼 추가
-- 운영중(OPERATING), 폐기(DEPRECATED), 폐기예정(SCHEDULED_DEPRECATION)

ALTER TABLE open_api_survey
ADD COLUMN IF NOT EXISTS operation_status VARCHAR(20) NOT NULL DEFAULT 'OPERATING';

-- 기존 레코드에 기본값 설정 (이미 DEFAULT로 설정되지만 명시적으로 처리)
UPDATE open_api_survey
SET operation_status = 'OPERATING'
WHERE operation_status IS NULL;
