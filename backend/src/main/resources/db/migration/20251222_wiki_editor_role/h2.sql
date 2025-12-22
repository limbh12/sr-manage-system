-- 20251222: WIKI_EDITOR Role 추가를 위한 Check Constraint 업데이트
-- H2 데이터베이스용

-- H2에서 Hibernate가 생성한 Check Constraint 삭제
-- CONSTRAINT_4는 user_role 컬럼의 Check Constraint 이름

-- 시도 1: 직접 constraint 삭제 (H2 2.x 문법)
ALTER TABLE users DROP CONSTRAINT IF EXISTS CONSTRAINT_4;

-- 시도 2: 다른 이름일 경우 대비
ALTER TABLE users DROP CONSTRAINT IF EXISTS CONSTRAINT_3;
ALTER TABLE users DROP CONSTRAINT IF EXISTS CONSTRAINT_5;
ALTER TABLE users DROP CONSTRAINT IF EXISTS CONSTRAINT_6;

-- 컬럼 재정의로 모든 Check Constraint 제거
ALTER TABLE users ALTER COLUMN user_role SET DATA TYPE VARCHAR(20);

-- Hibernate가 Enum 값을 검증하므로 DB level constraint 불필요
