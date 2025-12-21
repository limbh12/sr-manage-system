-- =====================================================
-- Wiki 알림 테이블 확장 마이그레이션 (PostgreSQL)
-- 날짜: 2025-12-20
-- 설명: OPEN API 현황조사 및 SR 알림 지원을 위한 필드 추가
-- =====================================================

-- 리소스 유형 컬럼 추가 (WIKI, SURVEY, SR 구분)
ALTER TABLE wiki_notifications ADD COLUMN IF NOT EXISTS resource_type VARCHAR(20);

-- 리소스 ID 컬럼 추가 (Survey ID 또는 SR ID)
ALTER TABLE wiki_notifications ADD COLUMN IF NOT EXISTS resource_id BIGINT;

-- 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_notification_resource ON wiki_notifications(resource_type, resource_id);

-- 기존 Wiki 알림에 resource_type 설정
UPDATE wiki_notifications
SET resource_type = 'WIKI'
WHERE resource_type IS NULL AND document_id IS NOT NULL;
