# Wiki 테이블 마이그레이션 가이드

**작성일**: 2025-12-19
**대상 버전**: Wiki Phase 1 (기본 Wiki 시스템)
**영향 범위**: 새 테이블 5개 추가 (기존 데이터 영향 없음)

---

## 📋 개요

Wiki 기능을 위한 데이터베이스 테이블을 추가합니다. 기존 SR 테이블과 데이터는 영향을 받지 않습니다.

### 추가되는 테이블

1. **wiki_category** - Wiki 카테고리 (계층 구조)
2. **wiki_document** - Wiki 문서
3. **wiki_version** - 문서 버전 이력
4. **wiki_file** - 첨부 파일
5. **sr_wiki_document** - SR-Wiki 연계 (Many-to-Many)

---

## 🚀 마이그레이션 실행 방법

### 1. H2 데이터베이스 (개발/테스트 환경)

**현재 설정**: `ddl-auto: create` → 서버 재시작 시 자동 생성됨

**수동 실행 (필요 시)**:
```bash
# H2 Console 접속: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:file:/Users/byunglim/sr-manage-system/backend/data/srdb
# User: sa
# Password: sa1234!

# SQL 실행
source migration_20251219_wiki_tables_h2.sql
```

**application.yml 설정 변경 (프로덕션 전환 시)**:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # create → update로 변경 (데이터 보존)
```

---

### 2. MySQL 8.x (프로덕션 환경)

**사전 준비**:
```bash
# 1. 데이터베이스 백업
mysqldump -u root -p srdb > backup_before_wiki_$(date +%Y%m%d).sql

# 2. 테이블 확인
mysql -u root -p srdb -e "SHOW TABLES LIKE 'wiki_%'"
```

**마이그레이션 실행**:
```bash
# 방법 1: 파일 실행
mysql -u root -p srdb < migration_20251219_wiki_tables_mysql.sql

# 방법 2: MySQL CLI에서 직접 실행
mysql -u root -p srdb
source /path/to/migration_20251219_wiki_tables_mysql.sql
```

**검증**:
```sql
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'srdb'
AND TABLE_NAME IN ('wiki_category', 'wiki_document', 'wiki_version', 'wiki_file', 'sr_wiki_document');
```

---

### 3. PostgreSQL (프로덕션 환경)

**사전 준비**:
```bash
# 1. 데이터베이스 백업
pg_dump -U postgres srdb > backup_before_wiki_$(date +%Y%m%d).sql

# 2. 테이블 확인
psql -U postgres -d srdb -c "SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename LIKE 'wiki_%';"
```

**마이그레이션 실행**:
```bash
# 방법 1: 파일 실행
psql -U postgres -d srdb -f migration_20251219_wiki_tables_postgresql.sql

# 방법 2: psql CLI에서 직접 실행
psql -U postgres -d srdb
\i /path/to/migration_20251219_wiki_tables_postgresql.sql
```

**검증**:
```sql
SELECT tablename FROM pg_tables
WHERE schemaname = 'public'
AND tablename IN ('wiki_category', 'wiki_document', 'wiki_version', 'wiki_file', 'sr_wiki_document');

-- ENUM 타입 확인
SELECT typname FROM pg_type WHERE typname = 'wiki_file_type';

-- 트리거 확인
SELECT trigger_name FROM information_schema.triggers
WHERE event_object_table = 'wiki_document';
```

---

### 4. CUBRID 10.x+ (프로덕션 환경)

**사전 준비**:
```bash
# 1. 데이터베이스 백업
cubrid backupdb -S srdb

# 2. 테이블 확인
csql srdb -c "SELECT class_name FROM db_class WHERE class_name LIKE 'wiki_%'"
```

**마이그레이션 실행**:
```bash
# 방법 1: 파일 실행
csql -u dba srdb < migration_20251219_wiki_tables_cubrid.sql

# 방법 2: csql CLI에서 직접 실행
csql -u dba srdb
;run migration_20251219_wiki_tables_cubrid.sql
```

**검증**:
```sql
SELECT class_name FROM db_class
WHERE class_name IN ('wiki_category', 'wiki_document', 'wiki_version', 'wiki_file', 'sr_wiki_document');

-- 시퀀스 확인
SELECT name FROM db_serial
WHERE name LIKE 'wiki_%';

-- 트리거 확인
SELECT trigger_name FROM db_trigger
WHERE target_class_name = 'wiki_document';
```

---

## ⚠️ 주의사항

### 1. 다운타임 최소화

**무중단 마이그레이션**:
- Wiki 테이블은 기존 SR 테이블과 독립적
- `sr_wiki_document`는 외래키만 추가 (SR 테이블 잠금 최소)
- **권장**: 새벽 시간대 또는 사용자 적은 시간대 실행

### 2. 롤백 준비

**롤백 스크립트**: `rollback_20251219_wiki_tables.sql`

```bash
# 롤백 실행 (모든 Wiki 데이터 삭제됨!)
mysql -u root -p srdb < rollback_20251219_wiki_tables.sql  # MySQL
psql -U postgres -d srdb -f rollback_20251219_wiki_tables.sql  # PostgreSQL
csql -u dba srdb < rollback_20251219_wiki_tables.sql  # CUBRID
```

**롤백 전 확인 사항**:
- Wiki 데이터 백업 여부
- 사용자 영향도 평가
- 관리자 승인

### 3. 외래키 제약조건

**ON DELETE CASCADE 적용 테이블**:
- `wiki_category.parent_id` → 상위 카테고리 삭제 시 하위 카테고리도 삭제
- `wiki_document` 삭제 → 관련 버전, 파일, SR 연계 자동 삭제
- `sr` 삭제 → SR-Wiki 연계만 삭제 (Wiki 문서는 유지)

---

## 📊 성능 최적화

### 인덱스 전략

**자동 생성된 인덱스**:
- Primary Key 인덱스 (모든 테이블)
- Foreign Key 인덱스
- Unique 제약조건 인덱스

**추가 인덱스**:
- `wiki_document.updated_at` (최근 문서 조회)
- `wiki_document.view_count` (인기 문서 조회)
- Full-text 인덱스 (MySQL, PostgreSQL)

### Full-text 검색

**MySQL**:
```sql
-- 문서 검색 쿼리
SELECT * FROM wiki_document
WHERE MATCH(title, content) AGAINST('검색어' IN NATURAL LANGUAGE MODE);
```

**PostgreSQL**:
```sql
-- 문서 검색 쿼리
SELECT * FROM wiki_document
WHERE to_tsvector('english', title || ' ' || content) @@ to_tsquery('검색어');
```

---

## 🔍 트러블슈팅

### 문제 1: 외래키 오류

**증상**: `Cannot add foreign key constraint`

**원인**: `users` 또는 `sr` 테이블 없음

**해결**:
```sql
-- 테이블 존재 확인
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_NAME IN ('users', 'sr');

-- 기존 SR 시스템 마이그레이션 먼저 실행
```

### 문제 2: 시퀀스 충돌 (CUBRID)

**증상**: `Serial already exists`

**해결**:
```sql
-- 기존 시퀀스 삭제 후 재생성
DROP SERIAL wiki_category_id_seq;
CREATE SERIAL wiki_category_id_seq START WITH 1;
```

### 문제 3: 트리거 생성 실패 (PostgreSQL)

**증상**: `Function does not exist`

**해결**:
```sql
-- 함수 먼저 생성 확인
SELECT proname FROM pg_proc WHERE proname = 'update_wiki_document_updated_at';

-- 트리거 재생성
DROP TRIGGER IF EXISTS trigger_update_wiki_document_updated_at ON wiki_document;
-- 이후 migration 스크립트 재실행
```

---

## ✅ 마이그레이션 체크리스트

**실행 전**:
- [ ] 데이터베이스 백업 완료
- [ ] 사용자 공지 (다운타임 필요 시)
- [ ] 롤백 스크립트 준비
- [ ] 테스트 환경에서 검증 완료

**실행 중**:
- [ ] 마이그레이션 스크립트 실행
- [ ] 에러 로그 확인
- [ ] 테이블 생성 확인

**실행 후**:
- [ ] 검증 쿼리 실행
- [ ] 인덱스 생성 확인
- [ ] 애플리케이션 재시작
- [ ] Wiki 기능 동작 테스트
- [ ] 사용자 공지 (완료)

---

## 📞 지원

**문의**: 프로젝트 관리자
**긴급 연락처**: -
**관련 문서**:
- `docs/HISTORY_20251219_WIKI_PHASE1.md`
- `docs/DATABASE.md`
