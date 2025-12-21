-- =====================================================
-- SR 소프트 삭제 기능 추가 마이그레이션 (PostgreSQL)
-- 날짜: 2025-12-11
-- 설명: SR 테이블에 소프트 삭제 관련 컬럼 추가
-- =====================================================

-- PostgreSQL 전용
-- =====================================================

-- sr 테이블에 삭제 관련 컬럼 추가
ALTER TABLE sr
  ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN deleted_at TIMESTAMP;

-- 성능 향상을 위한 인덱스 추가 (선택사항)
CREATE INDEX idx_sr_deleted ON sr(deleted);

-- 기존 데이터 확인 (모든 기존 SR은 deleted = FALSE로 설정됨)
-- SELECT COUNT(*) FROM sr WHERE deleted = FALSE;

COMMIT;
