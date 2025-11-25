# 데이터베이스 설계 문서

## 개요

SR Management System의 데이터베이스 스키마 설계 문서입니다.

### 지원 데이터베이스
- MySQL 8.x
- PostgreSQL

---

## ERD (Entity Relationship Diagram)

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  ┌──────────────────┐         ┌──────────────────┐             │
│  │      users       │         │   refresh_tokens │             │
│  ├──────────────────┤         ├──────────────────┤             │
│  │ id (PK)          │───┐     │ id (PK)          │             │
│  │ username         │   │     │ token            │             │
│  │ password         │   │     │ user_id (FK)     │───┐         │
│  │ email            │   │     │ expiry_date      │   │         │
│  │ role             │   │     └──────────────────┘   │         │
│  │ created_at       │   │                            │         │
│  └──────────────────┘   │                            │         │
│           │             │                            │         │
│           │             └────────────────────────────┘         │
│           │                                                     │
│           │                                                     │
│           ▼                                                     │
│  ┌──────────────────┐                                          │
│  │        sr        │                                          │
│  ├──────────────────┤                                          │
│  │ id (PK)          │                                          │
│  │ title            │                                          │
│  │ description      │                                          │
│  │ status           │                                          │
│  │ priority         │                                          │
│  │ requester_id (FK)│◄─────────────────────────────────────────│
│  │ assignee_id (FK) │◄─────────────────────────────────────────│
│  │ created_at       │                                          │
│  │ updated_at       │                                          │
│  └──────────────────┘                                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 테이블 정의

### 1. users (사용자)

사용자 정보를 저장하는 테이블

| 컬럼명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 사용자 고유 ID |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 사용자명 (로그인 ID) |
| password | VARCHAR(255) | NOT NULL | 암호화된 비밀번호 |
| email | VARCHAR(100) | NOT NULL, UNIQUE | 이메일 주소 |
| role | VARCHAR(20) | NOT NULL, DEFAULT 'USER' | 사용자 역할 (ADMIN, USER) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성 일시 |

**인덱스**
- PRIMARY KEY (id)
- UNIQUE INDEX idx_users_username (username)
- UNIQUE INDEX idx_users_email (email)

**DDL (MySQL)**
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_users_username (username),
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 2. sr (서비스 요청)

SR 정보를 저장하는 테이블

| 컬럼명 | 데이터 타입 | 제약조건 | 설명 |
|--------|-------------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | SR 고유 ID |
| title | VARCHAR(200) | NOT NULL | SR 제목 |
| description | TEXT | NULL | SR 상세 설명 |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'OPEN' | SR 상태 |
| priority | VARCHAR(20) | NOT NULL, DEFAULT 'MEDIUM' | 우선순위 |
| requester_id | BIGINT | FK (users.id), NOT NULL | 요청자 ID |
| assignee_id | BIGINT | FK (users.id), NULL | 담당자 ID |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성 일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정 일시 |

**인덱스**
- PRIMARY KEY (id)
- INDEX idx_sr_status (status)
- INDEX idx_sr_priority (priority)
- INDEX idx_sr_requester_id (requester_id)
- INDEX idx_sr_assignee_id (assignee_id)
- INDEX idx_sr_created_at (created_at)

**외래 키**
- FK_sr_requester: requester_id → users(id)
- FK_sr_assignee: assignee_id → users(id)

**DDL (MySQL)**
```sql
CREATE TABLE sr (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    requester_id BIGINT NOT NULL,
    assignee_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_sr_status (status),
    INDEX idx_sr_priority (priority),
    INDEX idx_sr_requester_id (requester_id),
    INDEX idx_sr_assignee_id (assignee_id),
    INDEX idx_sr_created_at (created_at),
    
    CONSTRAINT fk_sr_requester FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_sr_assignee FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 3. refresh_tokens (리프레시 토큰)

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

**DDL (MySQL)**
```sql
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

## 초기 데이터

### 관리자 계정 생성

```sql
-- 비밀번호는 BCrypt로 암호화된 값 (예: 'admin123')
INSERT INTO users (username, password, email, role, created_at)
VALUES ('admin', '$2a$10$N.zmN8YXqK7M6hLfr7xkqOkzxZQd8V8qe6X9jvJX8x8x8x8x8x8x8', 'admin@example.com', 'ADMIN', NOW());
```

### 샘플 SR 데이터

```sql
INSERT INTO sr (title, description, status, priority, requester_id, created_at, updated_at)
VALUES 
    ('로그인 오류 수정', '로그인 시 500 에러가 발생합니다.', 'OPEN', 'HIGH', 1, NOW(), NOW()),
    ('대시보드 UI 개선', '대시보드 레이아웃 개선이 필요합니다.', 'IN_PROGRESS', 'MEDIUM', 1, NOW(), NOW()),
    ('보고서 기능 추가', '월별 보고서 출력 기능을 추가해주세요.', 'OPEN', 'LOW', 1, NOW(), NOW());
```

---

## PostgreSQL DDL

MySQL DDL과 다른 부분만 표시합니다.

```sql
-- users 테이블
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- sr 테이블
CREATE TABLE sr (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    requester_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assignee_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- refresh_tokens 테이블
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date TIMESTAMP NOT NULL
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
```

---

## 백업 및 복구

### MySQL 백업
```bash
mysqldump -u username -p sr_management > backup.sql
```

### MySQL 복구
```bash
mysql -u username -p sr_management < backup.sql
```

### PostgreSQL 백업
```bash
pg_dump -U username sr_management > backup.sql
```

### PostgreSQL 복구
```bash
psql -U username sr_management < backup.sql
```
