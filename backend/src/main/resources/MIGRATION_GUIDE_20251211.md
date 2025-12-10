# SR 소프트 삭제 기능 마이그레이션 가이드

**날짜**: 2025-12-11
**버전**: v1.0
**대상**: SR 관리 시스템 운영 서버

---

## 개요

SR 테이블에 소프트 삭제 기능을 추가하기 위한 데이터베이스 스키마 마이그레이션 가이드입니다.

### 변경 사항
- `sr` 테이블에 `deleted` 컬럼 추가 (BOOLEAN, 기본값: FALSE)
- `sr` 테이블에 `deleted_at` 컬럼 추가 (TIMESTAMP, NULL 허용)
- 성능 향상을 위한 인덱스 추가 (선택사항)

---

## 사전 준비

### 1. 백업 수행 (필수)

```bash
# CUBRID 백업 예시
cubrid backupdb -C -z srdb

# MySQL 백업 예시
mysqldump -u username -p srdb > srdb_backup_20251211.sql

# PostgreSQL 백업 예시
pg_dump -U username -F c srdb > srdb_backup_20251211.dump
```

### 2. 다운타임 계획

- **예상 소요 시간**: 1-5분 (데이터 양에 따라 다름)
- **권장 작업 시간**: 사용자가 적은 시간대 (예: 새벽 2-4시)
- **점검 모드 전환**: 프론트엔드에 점검 안내 표시

### 3. 롤백 계획 준비

- 롤백 스크립트 검토: `rollback_20251211_soft_delete.sql`
- 이전 버전 애플리케이션 배포 준비
- 백업 복구 절차 확인

---

## 마이그레이션 절차

### Step 1: 데이터베이스 연결

```bash
# CUBRID
csql -u dba srdb

# MySQL
mysql -u username -p srdb

# PostgreSQL
psql -U username -d srdb
```

### Step 2: 현재 상태 확인

```sql
-- SR 테이블 구조 확인
DESC sr;  -- MySQL/CUBRID
\d sr     -- PostgreSQL

-- 현재 SR 개수 확인
SELECT COUNT(*) as total_sr FROM sr;

-- 테이블 크기 확인 (선택사항)
-- MySQL
SELECT
    table_name,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS size_mb
FROM information_schema.TABLES
WHERE table_schema = 'srdb' AND table_name = 'sr';
```

### Step 3: 마이그레이션 실행

**데이터베이스 종류에 맞는 스크립트를 선택하여 실행하세요.**

#### CUBRID

```bash
csql -u dba srdb < migration_20251211_soft_delete.sql
```

또는 대화형 모드에서:

```sql
@migration_20251211_soft_delete.sql
```

#### MySQL

```bash
mysql -u username -p srdb < migration_20251211_soft_delete_mysql.sql
```

또는:

```sql
SOURCE migration_20251211_soft_delete_mysql.sql;
```

#### PostgreSQL

```bash
psql -U username -d srdb -f migration_20251211_soft_delete_postgresql.sql
```

또는:

```sql
\i migration_20251211_soft_delete_postgresql.sql
```

### Step 4: 마이그레이션 검증

```sql
-- 1. 컬럼 추가 확인
DESC sr;  -- MySQL/CUBRID
\d sr     -- PostgreSQL

-- 2. 기본값 확인
SELECT deleted, deleted_at FROM sr LIMIT 5;

-- 3. 모든 기존 데이터가 deleted = FALSE인지 확인
SELECT COUNT(*) as should_be_total FROM sr WHERE deleted = FALSE;
SELECT COUNT(*) as total_sr FROM sr;
-- 두 값이 동일해야 함

-- 4. 인덱스 확인
SHOW INDEX FROM sr;  -- MySQL
\di                  -- PostgreSQL
-- CUBRID: 자동으로 인덱스 목록에 표시됨

-- 5. NULL 값 확인 (deleted_at은 모두 NULL이어야 함)
SELECT COUNT(*) as should_be_zero FROM sr WHERE deleted_at IS NOT NULL;
```

### Step 5: 애플리케이션 배포

1. 백엔드 애플리케이션 중지
2. 새 버전 배포
3. 애플리케이션 시작
4. 헬스 체크 확인

```bash
# 헬스 체크 예시
curl http://localhost:8080/api/health

# SR 목록 조회 테스트
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/api/sr
```

### Step 6: 기능 테스트

1. **일반 사용자 로그인**
   - SR 목록 조회 확인
   - "삭제된 항목 포함" 체크박스가 보이지 않는지 확인
   - SR 삭제 기능 테스트

2. **관리자 로그인**
   - "삭제된 항목 포함" 체크박스 표시 확인
   - SR 삭제 후 체크박스 선택하여 삭제된 항목 조회 확인
   - 삭제된 항목의 "복구" 버튼 확인
   - SR 복구 기능 테스트
   - 복구 후 정상적으로 표시되는지 확인

3. **데이터베이스 직접 확인**

```sql
-- 삭제 테스트 후 확인
SELECT id, sr_id, title, deleted, deleted_at
FROM sr
WHERE deleted = TRUE
ORDER BY deleted_at DESC;

-- 복구 테스트 후 확인
SELECT id, sr_id, title, deleted, deleted_at
FROM sr
WHERE id = [복구한_SR_ID];
-- deleted = FALSE, deleted_at = NULL 이어야 함
```

---

## 롤백 절차

### 긴급 롤백이 필요한 경우

```bash
# 1. 애플리케이션 중지

# 2. 이전 버전 애플리케이션 배포

# 3. 롤백 스크립트 실행
# CUBRID
csql -u dba srdb < rollback_20251211_soft_delete.sql

# MySQL
mysql -u username -p srdb < rollback_20251211_soft_delete.sql

# PostgreSQL
psql -U username -d srdb -f rollback_20251211_soft_delete.sql

# 4. 애플리케이션 시작
```

**⚠️ 경고**: 롤백 시 `deleted = TRUE`인 SR은 영구적으로 조회 불가능하게 됩니다!

---

## 트러블슈팅

### 문제 1: 컬럼 추가 실패

**증상**: `ALTER TABLE` 실행 시 오류 발생

**원인**:
- 테이블 락이 걸려있음
- 디스크 공간 부족
- 권한 부족

**해결방법**:
```sql
-- 락 확인 (MySQL)
SHOW PROCESSLIST;
SHOW OPEN TABLES WHERE In_use > 0;

-- 디스크 공간 확인
df -h

-- 권한 확인
SHOW GRANTS;
```

### 문제 2: 인덱스 생성 실패

**증상**: `CREATE INDEX` 실행 시 오류 발생

**해결방법**:
- 인덱스는 선택사항이므로 생략 가능
- 나중에 별도로 추가 가능:
  ```sql
  CREATE INDEX idx_sr_deleted ON sr(deleted);
  ```

### 문제 3: 애플리케이션 연결 오류

**증상**: 애플리케이션이 DB에 연결하지 못함

**해결방법**:
```bash
# 커넥션 풀 설정 확인
# application.yml 확인
hikari:
  maximum-pool-size: 10
  minimum-idle: 5

# DB 연결 테스트
telnet localhost 33000  # CUBRID
telnet localhost 3306   # MySQL
telnet localhost 5432   # PostgreSQL
```

### 문제 4: 기존 데이터 조회 안됨

**증상**: 마이그레이션 후 기존 SR이 조회되지 않음

**해결방법**:
```sql
-- 데이터 확인
SELECT COUNT(*) FROM sr;
SELECT COUNT(*) FROM sr WHERE deleted = FALSE;

-- 기본값이 제대로 설정되었는지 확인
SELECT deleted FROM sr LIMIT 10;

-- 필요시 수동으로 업데이트
UPDATE sr SET deleted = FALSE WHERE deleted IS NULL;
```

---

## 성능 고려사항

### 인덱스 추가의 영향

- **장점**: `deleted = FALSE` 조건의 쿼리 성능 향상
- **단점**: INSERT/UPDATE/DELETE 시 약간의 오버헤드
- **권장**: SR 테이블이 10,000건 이상일 경우 인덱스 추가 권장

### 테이블 락 시간

- **소규모** (< 1,000건): 1초 미만
- **중규모** (1,000-10,000건): 1-10초
- **대규모** (> 10,000건): 10-60초

### 리소스 사용량

- **디스크**: 기존 테이블 크기의 약 1-2% 추가 필요
- **메모리**: 큰 영향 없음
- **CPU**: ALTER TABLE 실행 중 일시적으로 증가

---

## 체크리스트

### 마이그레이션 전

- [ ] 백업 완료
- [ ] 롤백 계획 수립
- [ ] 점검 안내 공지
- [ ] 마이그레이션 스크립트 검토
- [ ] 테스트 환경에서 마이그레이션 테스트 완료
- [ ] 다운타임 일정 확정

### 마이그레이션 중

- [ ] 애플리케이션 중지
- [ ] 데이터베이스 연결 확인
- [ ] 현재 상태 확인 (SR 개수 등)
- [ ] 마이그레이션 스크립트 실행
- [ ] 마이그레이션 결과 검증
- [ ] 새 애플리케이션 배포
- [ ] 헬스 체크 통과

### 마이그레이션 후

- [ ] 기능 테스트 완료
  - [ ] 일반 사용자 기능 확인
  - [ ] 관리자 기능 확인
  - [ ] 삭제 기능 확인
  - [ ] 복구 기능 확인
- [ ] 성능 모니터링
- [ ] 에러 로그 확인
- [ ] 사용자 피드백 수집
- [ ] 점검 완료 공지

---

## 문의

마이그레이션 중 문제 발생 시:

1. 즉시 롤백 수행
2. 로그 파일 확인
3. 개발팀 문의

---

## 참고 문서

- [HISTORY_20251211.md](../../../docs/HISTORY_20251211.md) - 상세 변경 이력
- [DATABASE.md](../../../docs/DATABASE.md) - 데이터베이스 스키마 문서
- [API.md](../../../docs/API.md) - API 명세서

---

**마지막 업데이트**: 2025-12-11
