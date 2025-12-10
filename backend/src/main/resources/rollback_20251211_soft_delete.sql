-- =====================================================
-- SR 소프트 삭제 기능 롤백 스크립트
-- 날짜: 2025-12-11
-- 경고: 이 스크립트를 실행하면 삭제된 SR 데이터를 복구할 수 없습니다!
-- =====================================================

-- CUBRID / MySQL / PostgreSQL 공통
-- =====================================================

-- 주의: 롤백 전 반드시 백업 수행!

-- 1. 삭제된 SR이 있는지 확인
SELECT COUNT(*) as deleted_sr_count FROM sr WHERE deleted = TRUE;

-- 2. 삭제된 SR 목록 확인 (필요시)
-- SELECT id, sr_id, title, deleted_at FROM sr WHERE deleted = TRUE ORDER BY deleted_at DESC;

-- 3. 인덱스 삭제
DROP INDEX idx_sr_deleted;

-- 4. 컬럼 삭제
ALTER TABLE sr DROP COLUMN deleted_at;
ALTER TABLE sr DROP COLUMN deleted;

-- 5. 롤백 완료 확인
-- SELECT COUNT(*) FROM sr;

COMMIT;

-- =====================================================
-- 롤백 주의사항
-- =====================================================
-- 1. deleted = TRUE인 SR은 영구적으로 조회 불가능하게 됩니다
-- 2. 애플리케이션 코드도 함께 롤백해야 합니다
-- 3. 반드시 백업 후 실행하세요
-- 4. 운영 환경에서는 점검 시간에 수행하세요
