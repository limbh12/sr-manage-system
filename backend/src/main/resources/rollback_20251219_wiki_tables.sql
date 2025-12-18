-- =====================================================
-- Wiki 테이블 롤백 스크립트 (모든 DB 공통)
-- 작성일: 2025-12-19
-- 설명: Wiki 기능 Phase 1 테이블 삭제
-- 주의: 데이터가 영구적으로 삭제됩니다!
-- =====================================================

-- 롤백 전 백업 권장
-- mysqldump -u root -p srdb wiki_category wiki_document wiki_version wiki_file sr_wiki_document > wiki_backup_20251219.sql
-- pg_dump -U postgres -t wiki_* -t sr_wiki_document srdb > wiki_backup_20251219.sql

-- =====================================================
-- 1. 외래키가 있는 테이블부터 순서대로 삭제
-- =====================================================

-- SR-Wiki 연계 테이블 삭제
DROP TABLE IF EXISTS sr_wiki_document;

-- Wiki 파일 테이블 삭제
DROP TABLE IF EXISTS wiki_file;

-- Wiki 버전 테이블 삭제
DROP TABLE IF EXISTS wiki_version;

-- Wiki 문서 테이블 삭제
DROP TABLE IF EXISTS wiki_document;

-- Wiki 카테고리 테이블 삭제 (마지막)
DROP TABLE IF EXISTS wiki_category;

-- =====================================================
-- 2. CUBRID 시퀀스 삭제 (CUBRID만 해당)
-- =====================================================
-- DROP SERIAL wiki_category_id_seq;
-- DROP SERIAL wiki_document_id_seq;
-- DROP SERIAL wiki_version_id_seq;
-- DROP SERIAL wiki_file_id_seq;

-- =====================================================
-- 3. PostgreSQL ENUM 타입 삭제 (PostgreSQL만 해당)
-- =====================================================
-- DROP TYPE IF EXISTS wiki_file_type;

-- =====================================================
-- 4. PostgreSQL 트리거/함수 삭제 (PostgreSQL만 해당)
-- =====================================================
-- DROP TRIGGER IF EXISTS trigger_update_wiki_document_updated_at ON wiki_document;
-- DROP FUNCTION IF EXISTS update_wiki_document_updated_at();

-- =====================================================
-- 롤백 검증 쿼리
-- =====================================================
-- H2/MySQL:
-- SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
-- WHERE TABLE_NAME LIKE 'wiki_%' OR TABLE_NAME = 'sr_wiki_document';

-- PostgreSQL:
-- SELECT tablename FROM pg_tables
-- WHERE schemaname = 'public'
-- AND (tablename LIKE 'wiki_%' OR tablename = 'sr_wiki_document');

-- CUBRID:
-- SELECT class_name FROM db_class
-- WHERE class_name LIKE 'wiki_%' OR class_name = 'sr_wiki_document';
