-- =====================================================
-- SR 소프트 삭제 기능 추가 마이그레이션 (H2)
-- 날짜: 2025-12-11
-- 설명: SR 테이블에 소프트 삭제 관련 컬럼 추가
-- =====================================================

-- H2 전용
-- =====================================================

-- sr 테이블에 삭제 관련 컬럼 추가
-- IF NOT EXISTS를 사용하여 이미 존재하면 무시
ALTER TABLE sr ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sr ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- 성능 향상을 위한 인덱스 추가 (선택사항)
CREATE INDEX IF NOT EXISTS idx_sr_deleted ON sr(deleted);

-- 기존 데이터 확인 (모든 기존 SR은 deleted = FALSE로 설정됨)
-- SELECT COUNT(*) FROM sr WHERE deleted = FALSE;

COMMIT;

-- =====================================================
-- H2 사용 시 참고사항
-- =====================================================
-- 1. ddl-auto: create 모드에서는 이 스크립트가 필요하지 않습니다
--    (엔티티 기반으로 자동 생성됨)
--
-- 2. ddl-auto: update 모드에서 데이터를 유지하고 싶을 때만 사용하세요
--
-- 3. H2 Console에서 실행 방법:
--    http://localhost:8080/h2-console
--    JDBC URL: jdbc:h2:file:/Users/byunglim/sr-manage-system/backend/data/srdb
--    Username: sa
--    Password: sa1234!
