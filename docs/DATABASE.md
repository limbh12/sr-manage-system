# 데이터베이스 설계 문서

## 개요

SR Management System의 데이터베이스 스키마 설계 문서입니다.

### 지원 데이터베이스
- **CUBRID 10.x+ (Main)**
  - *Note: Spring Boot 3.x (Hibernate 6.x) 사용 시 기본 `CUBRIDDialect`가 호환되지 않을 수 있습니다. 본 프로젝트에서는 `MySQLDialect`를 상속받은 커스텀 `CubridDialect`를 사용하여 호환성 문제를 해결했습니다.*
- MySQL 8.x (Reference)
- PostgreSQL (Reference)

---

## ERD (Entity Relationship Diagram)

```
┌──────────────────┐          ┌──────────────────┐
│   organizations  │          │  open_api_survey │
├──────────────────┤          ├──────────────────┤
│ code (PK)        │          │ id (PK)          │
│ name             │          │ organization_code│
└──────────────────┘          │ ...              │
                              └──────────────────┘
                                        ▲
                                        │
┌──────────────────┐          ┌─────────┴────────┐          ┌──────────────────┐
│      users       │          │        sr        │          │    sr_history    │
├──────────────────┤          ├──────────────────┤          ├──────────────────┤
│ id (PK)          │◄─────────│ requester_id (FK)│◄─────────│ sr_id (FK)       │
│ username         │◄─────────│ assignee_id (FK) │          │ content          │
│ name             │          │ open_api_survey_id (FK)────►│ history_type     │
│ password         │          │ ...              │          │ created_by (FK)  │──┐
│ email            │          └──────────────────┘          └──────────────────┘  │
│ role             │                                                              │
│ ...              │◄─────────────────────────────────────────────────────────────┘
└──────────────────┘
         ▲
         │
┌────────┴─────────┐
│  refresh_tokens  │
├──────────────────┤
│ id (PK)          │
│ user_id (FK)     │
│ token            │
└──────────────────┘
```

---

## 테이블 정의

### 1. users (사용자)

사용자 정보를 저장하는 테이블

| 컬럼명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 사용자 고유 ID |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 사용자명 (로그인 ID) |
| name | VARCHAR(50) | NOT NULL | 사용자 이름 (실명) |
| password | VARCHAR(255) | NOT NULL | 암호화된 비밀번호 |
| email | VARCHAR(100) | NOT NULL, UNIQUE | 이메일 주소 |
| user_role | VARCHAR(20) | NOT NULL, DEFAULT 'USER' | 사용자 역할 (ADMIN, USER) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성 일시 |

**인덱스**
- PRIMARY KEY (id)
- UNIQUE INDEX idx_users_username (username)
- UNIQUE INDEX idx_users_email (email)

**DDL (CUBRID)**
```sql
CREATE SERIAL user_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '사용자명 (로그인 ID)',
    name VARCHAR(50) NOT NULL COMMENT '사용자 이름 (실명)',
    password VARCHAR(255) NOT NULL COMMENT '암호화된 비밀번호',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '이메일 주소',
    user_role VARCHAR(20) DEFAULT 'USER' NOT NULL COMMENT '사용자 역할 (ADMIN, USER)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성 일시'
) COMMENT='사용자 정보';

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
```

---

### 2. sr (서비스 요청)

SR 정보를 저장하는 테이블

| 컬럼명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | SR 고유 ID |
| sr_id | VARCHAR(20) | NOT NULL, UNIQUE | SR 식별 번호 (예: SR-2412-0001) |
| title | VARCHAR(200) | NOT NULL | SR 제목 |
| description | STRING | NULL | SR 상세 설명 |
| processing_details | STRING | NULL | 처리 내용 |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'OPEN' | SR 상태 |
| priority | VARCHAR(20) | NOT NULL, DEFAULT 'MEDIUM' | 우선순위 |
| requester_id | BIGINT | FK (users.id), NOT NULL | 요청자 ID |
| assignee_id | BIGINT | FK (users.id), NULL | 담당자 ID |
| open_api_survey_id | BIGINT | NULL | 연관된 Open API 현황조사 ID |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정 일시 |

**인덱스**
- PRIMARY KEY (id)
- UNIQUE INDEX idx_sr_sr_id (sr_id)
- INDEX idx_sr_status (status)
- INDEX idx_sr_priority (priority)
- INDEX idx_sr_requester_id (requester_id)
- INDEX idx_sr_assignee_id (assignee_id)
- INDEX idx_sr_created_at (created_at)

**외래 키**
- FK_sr_requester: requester_id → users(id)
- FK_sr_assignee: assignee_id → users(id)

**DDL (CUBRID)**
```sql
CREATE SERIAL sr_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE sr (
    id BIGINT PRIMARY KEY,
    sr_id VARCHAR(20) NOT NULL UNIQUE COMMENT 'SR 식별 번호',
    title VARCHAR(200) NOT NULL COMMENT 'SR 제목',
    description STRING COMMENT 'SR 상세 설명',
    processing_details STRING COMMENT '처리 내용',
    status VARCHAR(20) DEFAULT 'OPEN' NOT NULL COMMENT 'SR 상태',
    priority VARCHAR(20) DEFAULT 'MEDIUM' NOT NULL COMMENT '우선순위',
    requester_id BIGINT NOT NULL COMMENT '요청자 ID',
    assignee_id BIGINT COMMENT '담당자 ID',
    open_api_survey_id BIGINT COMMENT '연관된 Open API 현황조사 ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성 일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정 일시',
    CONSTRAINT fk_sr_requester FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_sr_assignee FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL
) COMMENT='서비스 요청 정보';

CREATE INDEX idx_sr_sr_id ON sr(sr_id);
CREATE INDEX idx_sr_status ON sr(status);
CREATE INDEX idx_sr_priority ON sr(priority);
CREATE INDEX idx_sr_requester_id ON sr(requester_id);
CREATE INDEX idx_sr_assignee_id ON sr(assignee_id);
CREATE INDEX idx_sr_created_at ON sr(created_at);
```

---

### 3. sr_history (SR 이력)

SR 변경 이력 및 댓글을 저장하는 테이블

| 컬럼명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 이력 고유 ID |
| sr_id | BIGINT | FK (sr.id), NOT NULL | 관련 SR ID |
| content | STRING | NOT NULL | 이력 내용 (변경 내역 또는 댓글) |
| history_type | VARCHAR(20) | NOT NULL | 이력 유형 (COMMENT, STATUS_CHANGE, etc.) |
| previous_value | STRING | NULL | 변경 전 값 (상세 변경 추적용) |
| new_value | STRING | NULL | 변경 후 값 (상세 변경 추적용) |
| created_by | BIGINT | FK (users.id), NOT NULL | 작성자 ID |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성 일시 |

**인덱스**
- PRIMARY KEY (id)
- INDEX idx_sr_history_sr_id (sr_id)
- INDEX idx_sr_history_created_at (created_at)

**외래 키**
- FK_sr_history_sr: sr_id → sr(id)
**DDL (CUBRID)**
```sql
CREATE SERIAL sr_history_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE sr_history (
    id BIGINT PRIMARY KEY,
    sr_id BIGINT NOT NULL COMMENT '관련 SR ID',
    content STRING NOT NULL COMMENT '이력 내용',
    history_type VARCHAR(20) NOT NULL COMMENT '이력 유형',
    previous_value STRING COMMENT '변경 전 값',
    new_value STRING COMMENT '변경 후 값',
    created_by BIGINT NOT NULL COMMENT '작성자 ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성 일시',
    CONSTRAINT fk_sr_history_sr FOREIGN KEY (sr_id) REFERENCES sr(id) ON DELETE CASCADE,
    CONSTRAINT fk_sr_history_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
) COMMENT='SR 변경 이력 및 댓글';

CREATE INDEX idx_sr_history_sr_id ON sr_history(sr_id);
CREATE INDEX idx_sr_history_created_at ON sr_history(created_at);
```

---

### 4. refresh_tokens (리프레시 토큰)

JWT Refresh Token을 저장하는 테이블

| 컬럼명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 토큰 고유 ID |
| token | VARCHAR(500) | NOT NULL, UNIQUE | Refresh Token 값 |
| user_id | BIGINT | FK (users.id), NOT NULL | 사용자 ID |
| expiry_date | TIMESTAMP | NOT NULL | 토큰 만료 일시 |

**인덱스**
- PRIMARY KEY (id)
- UNIQUE INDEX idx_refresh_tokens_token (token)
- INDEX idx_refresh_tokens_user_id (user_id)
- INDEX idx_refresh_tokens_expiry_date (expiry_date)

**외래 키**
- FK_refresh_tokens_user: user_id → users(id)

**DDL (CUBRID)**
```sql
CREATE TABLE refresh_tokens (
    id VARCHAR(36) PRIMARY KEY COMMENT '토큰 고유 ID (UUID)',
    token VARCHAR(500) NOT NULL UNIQUE COMMENT 'Refresh Token 값',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    expiry_date TIMESTAMP NOT NULL COMMENT '토큰 만료 일시',
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) COMMENT='JWT Refresh Token';

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expiry_date ON refresh_tokens(expiry_date);
```

---

### 5. open_api_survey (Open API 현황조사)

Open API 기술지원 현황조사 정보를 저장하는 테이블

| 컬럼명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 조사 고유 ID |
| organization_code | VARCHAR(20) | NOT NULL | 기관 코드 |
| department | VARCHAR(100) | NOT NULL | 부서명 |
| contact_name | VARCHAR(50) | NOT NULL | 담당자명 |
| contact_phone | VARCHAR(20) | NOT NULL | 담당자 연락처 |
| contact_email | VARCHAR(100) | NOT NULL | 담당자 이메일 |
| received_file_name | VARCHAR(255) | NULL | 수신 파일명 |
| received_date | DATE | NOT NULL | 수신 일자 |
| system_name | VARCHAR(100) | NOT NULL | 시스템명 |
| current_method | VARCHAR(20) | NOT NULL | 현행 방식 (CENTRAL, DISTRIBUTED) |
| desired_method | VARCHAR(20) | NOT NULL | 희망 방식 (CENTRAL_IMPROVED, DISTRIBUTED_IMPROVED) |
| reason_for_distributed | STRING | NULL | 분산개선형 선택 사유 |
| maintenance_operation | VARCHAR(30) | NOT NULL | 유지보수 운영인력 (INTERNAL, PROFESSIONAL_RESIDENT, PROFESSIONAL_NON_RESIDENT, OTHER) |
| maintenance_location | VARCHAR(20) | NOT NULL | 유지보수 수행장소 |
| maintenance_address | VARCHAR(255) | NULL | 유지보수 주소 |
| maintenance_note | STRING | NULL | 유지보수 담당자 정보 |
| operation_env | VARCHAR(20) | NOT NULL | 운영환경 구분 |
| server_location | VARCHAR(255) | NULL | 서버 위치 |
| web_server_os | VARCHAR(20) | NULL | WEB 서버 OS |
| web_server_os_type | VARCHAR(50) | NULL | WEB 서버 OS 종류 |
| web_server_os_version | VARCHAR(50) | NULL | WEB 서버 OS 버전 |
| web_server_type | VARCHAR(20) | NULL | WEB 서버 종류 |
| web_server_type_other | VARCHAR(50) | NULL | WEB 서버 종류 (기타) |
| web_server_version | VARCHAR(50) | NULL | WEB 서버 버전 |
| was_server_os | VARCHAR(20) | NULL | WAS 서버 OS |
| was_server_os_type | VARCHAR(50) | NULL | WAS 서버 OS 종류 |
| was_server_os_version | VARCHAR(50) | NULL | WAS 서버 OS 버전 |
| was_server_type | VARCHAR(20) | NULL | WAS 서버 종류 |
| was_server_type_other | VARCHAR(50) | NULL | WAS 서버 종류 (기타) |
| was_server_version | VARCHAR(50) | NULL | WAS 서버 버전 |
| db_server_os | VARCHAR(20) | NULL | DB 서버 OS |
| db_server_os_type | VARCHAR(50) | NULL | DB 서버 OS 종류 |
| db_server_os_version | VARCHAR(50) | NULL | DB 서버 OS 버전 |
| db_server_type | VARCHAR(20) | NULL | DB 서버 종류 |
| db_server_type_other | VARCHAR(50) | NULL | DB 서버 종류 (기타) |
| db_server_version | VARCHAR(50) | NULL | DB 서버 버전 |
| dev_language | VARCHAR(20) | NULL | 개발 언어 |
| dev_language_other | VARCHAR(50) | NULL | 개발 언어 (기타) |
| dev_language_version | VARCHAR(50) | NULL | 개발 언어 버전 |
| dev_framework | VARCHAR(20) | NULL | 개발 프레임워크 |
| dev_framework_other | VARCHAR(50) | NULL | 개발 프레임워크 (기타) |
| dev_framework_version | VARCHAR(50) | NULL | 개발 프레임워크 버전 |
| other_requests | STRING | NULL | 기타 요청사항 |
| note | STRING | NULL | 비고 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정 일시 |

**인덱스**
- PRIMARY KEY (id)
- INDEX idx_survey_org_code (organization_code)
- INDEX idx_survey_created_at (created_at)

**DDL (CUBRID)**
```sql
CREATE SERIAL open_api_survey_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE open_api_survey (
    id BIGINT PRIMARY KEY COMMENT '조사 고유 ID',
    organization_code VARCHAR(20) NOT NULL COMMENT '기관 코드',
    department VARCHAR(100) NOT NULL COMMENT '부서명',
    contact_name VARCHAR(50) NOT NULL COMMENT '담당자명',
    contact_phone VARCHAR(20) NOT NULL COMMENT '담당자 연락처',
    contact_email VARCHAR(100) NOT NULL COMMENT '담당자 이메일',
    received_file_name VARCHAR(255) COMMENT '수신 파일명',
    received_date DATE NOT NULL COMMENT '수신 일자',
    system_name VARCHAR(100) NOT NULL COMMENT '시스템명',
    current_method VARCHAR(20) NOT NULL COMMENT '현행 방식',
    desired_method VARCHAR(20) NOT NULL COMMENT '희망 방식',
    reason_for_distributed STRING COMMENT '분산개선형 선택 사유',
    maintenance_operation VARCHAR(30) NOT NULL COMMENT '유지보수 운영인력',
    maintenance_location VARCHAR(20) NOT NULL COMMENT '유지보수 수행장소',
    maintenance_address VARCHAR(255) COMMENT '유지보수 주소',
    maintenance_note STRING COMMENT '유지보수 담당자 정보',
    operation_env VARCHAR(20) NOT NULL COMMENT '운영환경 구분',
    server_location VARCHAR(255) COMMENT '서버 위치',
    web_server_os VARCHAR(20) COMMENT 'WEB 서버 OS',
    web_server_os_type VARCHAR(50) COMMENT 'WEB 서버 OS 종류',
    web_server_os_version VARCHAR(50) COMMENT 'WEB 서버 OS 버전',
    web_server_type VARCHAR(20) COMMENT 'WEB 서버 종류',
    web_server_type_other VARCHAR(50) COMMENT 'WEB 서버 종류 (기타)',
    web_server_version VARCHAR(50) COMMENT 'WEB 서버 버전',
    was_server_os VARCHAR(20) COMMENT 'WAS 서버 OS',
    was_server_os_type VARCHAR(50) COMMENT 'WAS 서버 OS 종류',
    was_server_os_version VARCHAR(50) COMMENT 'WAS 서버 OS 버전',
    was_server_type VARCHAR(20) COMMENT 'WAS 서버 종류',
    was_server_type_other VARCHAR(50) COMMENT 'WAS 서버 종류 (기타)',
    was_server_version VARCHAR(50) COMMENT 'WAS 서버 버전',
    db_server_os VARCHAR(20) COMMENT 'DB 서버 OS',
    db_server_os_type VARCHAR(50) COMMENT 'DB 서버 OS 종류',
    db_server_os_version VARCHAR(50) COMMENT 'DB 서버 OS 버전',
    db_server_type VARCHAR(20) COMMENT 'DB 서버 종류',
    db_server_type_other VARCHAR(50) COMMENT 'DB 서버 종류 (기타)',
    db_server_version VARCHAR(50) COMMENT 'DB 서버 버전',
    dev_language VARCHAR(20) COMMENT '개발 언어',
    dev_language_other VARCHAR(50) COMMENT '개발 언어 (기타)',
    dev_language_version VARCHAR(50) COMMENT '개발 언어 버전',
    dev_framework VARCHAR(20) COMMENT '개발 프레임워크',
    dev_framework_other VARCHAR(50) COMMENT '개발 프레임워크 (기타)',
    dev_framework_version VARCHAR(50) COMMENT '개발 프레임워크 버전',
    other_requests STRING COMMENT '기타 요청사항',
    note STRING COMMENT '비고',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '생성 일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '수정 일시'
) COMMENT='Open API 기술지원 현황조사';

CREATE INDEX idx_survey_org_code ON open_api_survey(organization_code);
CREATE INDEX idx_survey_created_at ON open_api_survey(created_at);
```

### 6. organizations (행정기관)

행정표준코드 및 기관명 정보를 저장하는 테이블

| 컬럼명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| code | VARCHAR(20) | PK | 행정표준코드 |
| name | VARCHAR(100) | NOT NULL | 기관명 |

**인덱스**
- PRIMARY KEY (code)
- INDEX idx_organizations_name (name)

**DDL (CUBRID)**
```sql
CREATE TABLE organizations (
    code VARCHAR(20) PRIMARY KEY COMMENT '행정표준코드',
    name VARCHAR(100) NOT NULL COMMENT '기관명'
) COMMENT='행정기관 정보';

CREATE INDEX idx_organizations_name ON organizations(name);
```

---

## Enum 정의

### Role (사용자 역할)

| 값 | 설명 |
|----|------|
| ADMIN | 관리자 - 모든 권한 |
| USER | 일반 사용자 |

### SrStatus (SR 상태)

| 값 | 설명 | 상태 전이 |
|----|------|----------|
| OPEN | 신규 등록 | → IN_PROGRESS, CLOSED |
| IN_PROGRESS | 처리 중 | → RESOLVED, OPEN, CLOSED |
| RESOLVED | 해결됨 | → CLOSED, IN_PROGRESS |
| CLOSED | 종료 | (최종 상태) |

**상태 전이 다이어그램**
```
    ┌───────────────────────────────────────┐
    │                                       │
    ▼                                       │
 ┌──────┐      ┌─────────────┐      ┌──────────┐      ┌────────┐
 │ OPEN │ ───► │ IN_PROGRESS │ ───► │ RESOLVED │ ───► │ CLOSED │
 └──────┘      └─────────────┘      └──────────┘      └────────┘
    │                │                    │                ▲
    │                │                    │                │
    │                └────────────────────┴────────────────┘
    │                                                      │
    └──────────────────────────────────────────────────────┘
```

### Priority (우선순위)

| 값 | 설명 | 처리 권장 시간 |
|----|------|----------------|
| LOW | 낮음 | 5일 이내 |
| MEDIUM | 보통 | 3일 이내 |
| HIGH | 높음 | 1일 이내 |
| CRITICAL | 긴급 | 4시간 이내 |

---

### 초기 데이터

### 관리자 계정 생성

```sql
-- 비밀번호는 BCrypt로 암호화된 값 (예: 'admin123')
INSERT INTO users (username, name, password, email, user_role, created_at)
VALUES ('admin', '관리자', '$2a$10$N.zmN8YXqK7M6hLfr7xkqOkzxZQd8V8qe6X9jvJX8x8x8x8x8x8x8', 'admin@example.com', 'ADMIN', NOW());
```

### 샘플 SR 데이터

```sql
INSERT INTO sr (sr_id, title, description, status, priority, requester_id, created_at, updated_at)
VALUES 
    ('SR-2512-0001', '로그인 오류 수정', '로그인 시 500 에러가 발생합니다.', 'OPEN', 'HIGH', 1, NOW(), NOW()),
    ('SR-2512-0002', '대시보드 UI 개선', '대시보드 레이아웃 개선이 필요합니다.', 'IN_PROGRESS', 'MEDIUM', 1, NOW(), NOW()),
    ('SR-2512-0003', '보고서 기능 추가', '월별 보고서 출력 기능을 추가해주세요.', 'OPEN', 'LOW', 1, NOW(), NOW());
```

---

## Reference: MySQL DDL

```sql
-- users 테이블
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    user_role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_users_username (username),
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- sr 테이블
CREATE TABLE sr (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sr_id VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    processing_details TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    requester_id BIGINT NOT NULL,
    assignee_id BIGINT,
    open_api_survey_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_sr_sr_id (sr_id),
    INDEX idx_sr_status (status),
    INDEX idx_sr_priority (priority),
    INDEX idx_sr_requester_id (requester_id),
    INDEX idx_sr_assignee_id (assignee_id),
    INDEX idx_sr_created_at (created_at),
    
    CONSTRAINT fk_sr_requester FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_sr_assignee FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- sr_history 테이블
CREATE TABLE sr_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sr_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    history_type VARCHAR(20) NOT NULL,
    previous_value TEXT,
    new_value TEXT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_sr_history_sr_id (sr_id),
    INDEX idx_sr_history_created_at (created_at),
    
    CONSTRAINT fk_sr_history_sr FOREIGN KEY (sr_id) REFERENCES sr(id) ON DELETE CASCADE,
    CONSTRAINT fk_sr_history_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- refresh_tokens 테이블
CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    
    INDEX idx_refresh_tokens_token (token),
    INDEX idx_refresh_tokens_user_id (user_id),
    INDEX idx_refresh_tokens_expiry_date (expiry_date),
    
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- open_api_survey 테이블
CREATE TABLE open_api_survey (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_code VARCHAR(20) NOT NULL,
    department VARCHAR(100) NOT NULL,
    contact_name VARCHAR(50) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    contact_email VARCHAR(100) NOT NULL,
    received_file_name VARCHAR(255),
    received_date DATE NOT NULL,
    system_name VARCHAR(100) NOT NULL,
    current_method VARCHAR(20) NOT NULL,
    desired_method VARCHAR(20) NOT NULL,
    reason_for_distributed TEXT,
    maintenance_operation VARCHAR(20) NOT NULL,
    maintenance_location VARCHAR(20) NOT NULL,
    maintenance_address VARCHAR(255),
    maintenance_note TEXT,
    operation_env VARCHAR(20) NOT NULL,
    server_location VARCHAR(255),
    web_server_os VARCHAR(20),
    web_server_os_type VARCHAR(50),
    web_server_os_version VARCHAR(50),
    web_server_type VARCHAR(20),
    web_server_type_other VARCHAR(50),
    web_server_version VARCHAR(50),
    was_server_os VARCHAR(20),
    was_server_os_type VARCHAR(50),
    was_server_os_version VARCHAR(50),
    was_server_type VARCHAR(20),
    was_server_type_other VARCHAR(50),
    was_server_version VARCHAR(50),
    db_server_os VARCHAR(20),
    db_server_os_type VARCHAR(50),
    db_server_os_version VARCHAR(50),
    db_server_type VARCHAR(20),
    db_server_type_other VARCHAR(50),
    db_server_version VARCHAR(50),
    dev_language VARCHAR(20),
    dev_language_other VARCHAR(50),
    dev_language_version VARCHAR(50),
    dev_framework VARCHAR(20),
    dev_framework_other VARCHAR(50),
    dev_framework_version VARCHAR(50),
    other_requests TEXT,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_survey_org_code (organization_code),
    INDEX idx_survey_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- organizations 테이블
CREATE TABLE organizations (
    code VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    
    INDEX idx_organizations_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## Reference: PostgreSQL DDL

```sql
-- users 테이블
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    user_role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- sr 테이블
CREATE TABLE sr (
    id BIGSERIAL PRIMARY KEY,
    sr_id VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    processing_details TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    requester_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assignee_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    open_api_survey_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- sr_history 테이블
CREATE TABLE sr_history (
    id BIGSERIAL PRIMARY KEY,
    sr_id BIGINT NOT NULL REFERENCES sr(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    history_type VARCHAR(20) NOT NULL,
    created_by BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- refresh_tokens 테이블
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date TIMESTAMP NOT NULL
);

-- open_api_survey 테이블
CREATE TABLE open_api_survey (
    id BIGSERIAL PRIMARY KEY,
    organization_code VARCHAR(20) NOT NULL,
    department VARCHAR(100) NOT NULL,
    contact_name VARCHAR(50) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    contact_email VARCHAR(100) NOT NULL,
    received_file_name VARCHAR(255),
    received_date DATE NOT NULL,
    system_name VARCHAR(100) NOT NULL,
    current_method VARCHAR(20) NOT NULL,
    desired_method VARCHAR(20) NOT NULL,
    reason_for_distributed TEXT,
    maintenance_operation VARCHAR(20) NOT NULL,
    maintenance_location VARCHAR(20) NOT NULL,
    maintenance_address VARCHAR(255),
    maintenance_note TEXT,
    operation_env VARCHAR(20) NOT NULL,
    server_location VARCHAR(255),
    web_server_os VARCHAR(20),
    web_server_os_type VARCHAR(50),
    web_server_os_version VARCHAR(50),
    web_server_type VARCHAR(20),
    web_server_type_other VARCHAR(50),
    web_server_version VARCHAR(50),
    was_server_os VARCHAR(20),
    was_server_os_type VARCHAR(50),
    was_server_os_version VARCHAR(50),
    was_server_type VARCHAR(20),
    was_server_type_other VARCHAR(50),
    was_server_version VARCHAR(50),
    db_server_os VARCHAR(20),
    db_server_os_type VARCHAR(50),
    db_server_os_version VARCHAR(50),
    db_server_type VARCHAR(20),
    db_server_type_other VARCHAR(50),
    db_server_version VARCHAR(50),
    dev_language VARCHAR(20),
    dev_language_other VARCHAR(50),
    dev_language_version VARCHAR(50),
    dev_framework VARCHAR(20),
    dev_framework_other VARCHAR(50),
    dev_framework_version VARCHAR(50),
    other_requests TEXT,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- organizations 테이블
CREATE TABLE organizations (
    code VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

-- updated_at 자동 갱신을 위한 트리거 함수 (PostgreSQL)
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_sr_updated_at
    BEFORE UPDATE ON sr
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_survey_updated_at
    BEFORE UPDATE ON open_api_survey
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```
