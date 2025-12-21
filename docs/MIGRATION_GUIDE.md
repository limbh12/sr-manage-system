# SR 관리 시스템 - 데이터베이스 마이그레이션 가이드

이 문서는 SR 관리 시스템의 데이터베이스 스키마 변경 이력과 마이그레이션 방법을 설명합니다.

---

## 목차

1. [마이그레이션 스크립트 위치](#1-마이그레이션-스크립트-위치)
2. [마이그레이션 이력](#2-마이그레이션-이력)
3. [마이그레이션 실행 방법](#3-마이그레이션-실행-방법)
4. [롤백 방법](#4-롤백-방법)
5. [마이그레이션 상세](#5-마이그레이션-상세)
6. [트러블슈팅](#6-트러블슈팅)
7. [체크리스트](#7-체크리스트)

---

## 1. 마이그레이션 스크립트 위치

모든 마이그레이션 스크립트는 아래 경로에 날짜별로 정리되어 있습니다:

```
backend/src/main/resources/db/migration/
├── 20251211_sr_soft_delete/          # SR 소프트 삭제 기능
├── 20251211_contact_position/        # 담당자 직급 필드
├── 20251211_survey_assignee_status/  # 현황조사 담당자/상태
├── 20251219_wiki_tables/             # Wiki 테이블 전체
├── 20251220_notification_fields/     # 알림 확장 필드
├── migrate_method_values.sql         # 방식 값 변환 (일회성)
├── add_survey_status_column.sql      # 상태 컬럼 추가
└── add_operation_status_column.sql   # 운영상태 컬럼 추가
```

각 폴더에는 DB별 스크립트가 포함되어 있습니다:
- `h2.sql` - H2 데이터베이스용
- `mysql.sql` - MySQL 8.x용
- `postgresql.sql` - PostgreSQL용
- `cubrid.sql` - CUBRID용
- `rollback.sql` - 롤백 스크립트

---

## 2. 마이그레이션 이력

### 2025-12-20: Phase 4 알림 확장
**폴더:** `20251220_notification_fields/`

**변경 내용:**
- `wiki_notifications` 테이블에 `resource_type`, `resource_id` 컬럼 추가
- OPEN API 현황조사 및 SR 알림 지원

**적용 대상:** Wiki Phase 3 이후 배포된 시스템

---

### 2025-12-19: Wiki 기능 (Phase 1 + Phase 2)
**폴더:** `20251219_wiki_tables/`

**새로 생성되는 테이블:**
| 테이블명 | 설명 |
|---------|------|
| `wiki_category` | 위키 카테고리 (계층 구조) |
| `wiki_document` | 위키 문서 |
| `wiki_version` | 문서 버전 이력 |
| `wiki_file` | 첨부 파일/이미지 (PDF 변환 기능 포함) |
| `sr_wiki_document` | SR-Wiki 연계 (M:N) |

**Phase 2 추가 필드 (wiki_file):**
- `mime_type` - MIME 타입
- `conversion_status` - PDF 변환 상태 (NOT_APPLICABLE, PENDING, PROCESSING, COMPLETED, FAILED)
- `conversion_error_message` - 변환 실패 시 에러 메시지
- `converted_at` - 변환 완료 시각

---

### 2025-12-11: SR 소프트 삭제
**폴더:** `20251211_sr_soft_delete/`

**변경 내용:**
- `sr` 테이블에 `deleted` 컬럼 추가 (BOOLEAN, 기본값: FALSE)
- `sr` 테이블에 `deleted_at` 컬럼 추가 (TIMESTAMP, NULL 허용)
- 성능 향상을 위한 인덱스 추가

---

### 2025-12-11: 담당자 직급 필드
**폴더:** `20251211_contact_position/`

**변경 내용:**
- `open_api_survey` 테이블에 `contact_position` 컬럼 추가

---

### 2025-12-11: 현황조사 담당자/상태 관리
**폴더:** `20251211_survey_assignee_status/`

**변경 내용:**
- `open_api_survey` 테이블에 `assignee_id`, `status` 컬럼 추가
- 담당자 지정 및 작성 상태 관리 기능

---

## 3. 마이그레이션 실행 방법

### 3.1 사전 준비

#### 백업 수행 (필수)
```bash
# H2 파일 모드
cp backend/data/srdb.mv.db backend/data/srdb.mv.db.backup

# MySQL
mysqldump -u username -p database_name > backup_$(date +%Y%m%d).sql

# PostgreSQL
pg_dump -U username -F c database_name > backup_$(date +%Y%m%d).dump

# CUBRID
cubrid backupdb -C -z database_name
```

#### 다운타임 계획
- **예상 소요 시간**: 1-5분 (데이터 양에 따라 다름)
- **권장 작업 시간**: 사용자가 적은 시간대 (예: 새벽 2-4시)

---

### 3.2 H2 데이터베이스 (개발 환경)

**방법 1: H2 콘솔에서 직접 실행**
1. http://localhost:8080/h2-console 접속
2. 해당 `.sql` 파일 내용을 복사하여 실행

**방법 2: ddl-auto 설정**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # 변경된 스키마만 자동 반영
```

**방법 3: 자동 실행 (prod 프로필)**
`application.yml`의 `schema-locations`에 등록된 스크립트는 서버 시작 시 자동 실행됩니다.

---

### 3.3 MySQL 8.x

```bash
# 데이터베이스 연결
mysql -u username -p database_name

# 마이그레이션 실행
mysql -u username -p database_name < mysql.sql

# 또는 CLI에서 직접 실행
SOURCE /path/to/mysql.sql;
```

**검증:**
```sql
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME IN ('wiki_category', 'wiki_document', 'wiki_version', 'wiki_file', 'sr_wiki_document');
```

---

### 3.4 PostgreSQL

```bash
# 데이터베이스 연결
psql -U username -d database_name

# 마이그레이션 실행
psql -U username -d database_name -f postgresql.sql

# 또는 psql에서 직접 실행
\i /path/to/postgresql.sql
```

**검증:**
```sql
SELECT tablename FROM pg_tables
WHERE schemaname = 'public'
AND tablename IN ('wiki_category', 'wiki_document', 'wiki_version', 'wiki_file', 'sr_wiki_document');

-- ENUM 타입 확인
SELECT typname FROM pg_type WHERE typname LIKE 'wiki_%';

-- 트리거 확인
SELECT trigger_name FROM information_schema.triggers
WHERE event_object_table = 'wiki_document';
```

---

### 3.5 CUBRID

```bash
# 데이터베이스 연결
csql -u dba database_name

# 마이그레이션 실행
csql -u dba database_name < cubrid.sql

# 또는 csql에서 직접 실행
;run /path/to/cubrid.sql
```

**검증:**
```sql
SELECT class_name FROM db_class
WHERE class_name IN ('wiki_category', 'wiki_document', 'wiki_version', 'wiki_file', 'sr_wiki_document');

-- 시퀀스 확인
SELECT name FROM db_serial WHERE name LIKE 'wiki_%';

-- 트리거 확인
SELECT trigger_name FROM db_trigger WHERE target_class_name = 'wiki_document';
```

---

## 4. 롤백 방법

문제 발생 시 각 폴더의 `rollback.sql` 파일을 실행합니다.

```bash
# MySQL
mysql -u username -p database_name < rollback.sql

# PostgreSQL
psql -U username -d database_name -f rollback.sql

# CUBRID
csql -u dba database_name < rollback.sql
```

**주의사항:**
- 롤백은 데이터 손실을 야기할 수 있습니다
- 반드시 백업 후 진행하세요
- SR 소프트 삭제 롤백 시 `deleted = TRUE`인 SR은 영구적으로 조회 불가능

---

## 5. 마이그레이션 상세

### 5.1 SR 소프트 삭제 (20251211)

#### 추가되는 컬럼
| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `deleted` | BOOLEAN | 삭제 여부 (기본값: FALSE) |
| `deleted_at` | TIMESTAMP | 삭제 시각 (NULL 허용) |

#### 성능 고려사항
- **인덱스 추가**: `idx_sr_deleted` - `deleted = FALSE` 조건 쿼리 최적화
- **테이블 락 시간**:
  - 소규모 (< 1,000건): 1초 미만
  - 중규모 (1,000-10,000건): 1-10초
  - 대규모 (> 10,000건): 10-60초

#### 기능 테스트
1. 일반 사용자: SR 삭제 시 목록에서 사라지는지 확인
2. 관리자: "삭제된 항목 포함" 체크박스로 삭제된 SR 조회
3. 관리자: 삭제된 SR 복구 기능 확인

---

### 5.2 Wiki 테이블 (20251219)

#### 테이블 구조

**wiki_category** (계층형 카테고리)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT |
| name | VARCHAR(100) | 카테고리명 |
| parent_id | BIGINT | 상위 카테고리 FK (자기참조) |
| sort_order | INT | 정렬 순서 |

**wiki_document** (문서)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT | PK |
| title | VARCHAR(200) | 제목 |
| content | TEXT/CLOB | 내용 (Markdown) |
| category_id | BIGINT | 카테고리 FK |
| created_by | BIGINT | 작성자 FK |
| view_count | INT | 조회수 |

**wiki_file** (첨부파일 - Phase 2 PDF 변환 포함)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT | PK |
| document_id | BIGINT | 문서 FK |
| type | ENUM/VARCHAR | IMAGE, DOCUMENT, ATTACHMENT |
| conversion_status | VARCHAR | PDF 변환 상태 |
| mime_type | VARCHAR(50) | MIME 타입 |

#### 외래키 제약조건 (ON DELETE CASCADE)
- `wiki_category.parent_id` → 상위 카테고리 삭제 시 하위도 삭제
- `wiki_document` 삭제 → 관련 버전, 파일, SR 연계 자동 삭제
- `sr` 삭제 → SR-Wiki 연계만 삭제 (Wiki 문서는 유지)

#### Full-text 검색

**MySQL:**
```sql
SELECT * FROM wiki_document
WHERE MATCH(title, content) AGAINST('검색어' IN NATURAL LANGUAGE MODE);
```

**PostgreSQL:**
```sql
SELECT * FROM wiki_document
WHERE to_tsvector('english', title || ' ' || content) @@ to_tsquery('검색어');
```

---

### 5.3 알림 확장 필드 (20251220)

#### 추가되는 컬럼
| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `resource_type` | VARCHAR(20) | 리소스 유형 (WIKI, SURVEY, SR) |
| `resource_id` | BIGINT | 리소스 ID |

---

## 6. 트러블슈팅

### 문제 1: 컬럼/테이블 추가 실패

**증상:** `ALTER TABLE` 또는 `CREATE TABLE` 실행 시 오류

**원인:**
- 테이블 락이 걸려있음
- 디스크 공간 부족
- 권한 부족

**해결:**
```sql
-- MySQL: 락 확인
SHOW PROCESSLIST;
SHOW OPEN TABLES WHERE In_use > 0;

-- 디스크 공간 확인
df -h

-- 권한 확인
SHOW GRANTS;
```

---

### 문제 2: 외래키 오류

**증상:** `Cannot add foreign key constraint`

**원인:** 참조 테이블(`users`, `sr`)이 없음

**해결:**
```sql
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_NAME IN ('users', 'sr');

-- 기존 SR 시스템 마이그레이션 먼저 실행
```

---

### 문제 3: 시퀀스 충돌 (CUBRID)

**증상:** `Serial already exists`

**해결:**
```sql
DROP SERIAL wiki_category_id_seq;
CREATE SERIAL wiki_category_id_seq START WITH 1;
```

---

### 문제 4: 트리거 생성 실패 (PostgreSQL)

**증상:** `Function does not exist`

**해결:**
```sql
-- 함수 존재 확인
SELECT proname FROM pg_proc WHERE proname = 'update_wiki_document_updated_at';

-- 트리거 재생성
DROP TRIGGER IF EXISTS trigger_update_wiki_document_updated_at ON wiki_document;
```

---

### 문제 5: 기존 데이터 조회 안됨 (SR 소프트 삭제 후)

**증상:** 마이그레이션 후 기존 SR이 조회되지 않음

**해결:**
```sql
-- 데이터 확인
SELECT COUNT(*) FROM sr;
SELECT COUNT(*) FROM sr WHERE deleted = FALSE;

-- 기본값이 제대로 설정되었는지 확인
SELECT deleted FROM sr LIMIT 10;

-- 필요시 수동 업데이트
UPDATE sr SET deleted = FALSE WHERE deleted IS NULL;
```

---

## 7. 체크리스트

### 마이그레이션 전
- [ ] 데이터베이스 백업 완료
- [ ] 롤백 스크립트 검토
- [ ] 테스트 환경에서 마이그레이션 테스트 완료
- [ ] 다운타임 일정 확정 및 공지
- [ ] 롤백 계획 수립

### 마이그레이션 중
- [ ] 애플리케이션 중지
- [ ] 데이터베이스 연결 확인
- [ ] 현재 상태 확인 (레코드 수 등)
- [ ] 마이그레이션 스크립트 실행
- [ ] 마이그레이션 결과 검증 (테이블, 컬럼, 인덱스)
- [ ] 에러 로그 확인

### 마이그레이션 후
- [ ] 새 애플리케이션 배포
- [ ] 헬스 체크 통과
- [ ] 기능 테스트 완료
  - [ ] 일반 사용자 기능 확인
  - [ ] 관리자 기능 확인
  - [ ] 신규 기능 확인 (Wiki, 소프트 삭제 등)
- [ ] 성능 모니터링
- [ ] 에러 로그 확인
- [ ] 점검 완료 공지

---

## 참고 문서

- [운영 가이드](./OPERATION_GUIDE.md) - 백업, 복원, 서버 관리
- [API 문서](./API.md) - REST API 명세
- [데이터베이스 설계](./DATABASE.md) - 전체 스키마

---

**마지막 업데이트**: 2025-12-21
