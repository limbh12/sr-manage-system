
---

# 📑 Project: SR Management System

PUBC 오픈API 전환 지원을 위한 SR(Service Request) 관리 시스템입니다.
폐쇄망 환경을 고려한 통합 배포 구조와 다양한 데이터베이스 지원을 특징으로 합니다.

---

## 🛠 1. 기술 스택 (Tech Stack)

| 구분 | 상세 기술 | 버전 | 비고 |
| --- | --- | --- | --- |
| **Frontend** | React, TypeScript, Vite | 18.x | UI/UX 프레임워크 |
| **State Management** | Redux Toolkit | - | 전역 상태 관리 |
| **Backend** | Spring Boot, Spring Data JPA | 3.2.0 | RESTful API 서버 |
| **Language** | Java | 17 | 백엔드 개발 언어 |
| **Build Tool** | Maven | 3.x | 의존성 관리 및 빌드 |
| **Database** | H2 (기본), CUBRID, MySQL, PostgreSQL | - | 다중 DB 지원 |
| **Authentication** | JWT (JSON Web Token) | - | Stateless 인증 |
| **Security** | Spring Security, AES-256 | - | 인증/암호화 |

---

## 📋 2. 핵심 기능 (Core Features)

### Epic 1. SR(Service Request) 관리

* [x] **SR-1: SR 등록 및 수정**
  * 제목, 내용, 우선순위, 담당자 지정
  * 첨부파일 업로드 지원


* [x] **SR-2: SR 상태 관리**
  * 상태 흐름: OPEN → IN_PROGRESS → RESOLVED → CLOSED
  * 상태별 필터링 및 검색


* [x] **SR-3: SR 변경 이력 추적**
  * 모든 수정 내용 자동 기록
  * Diff View 기능으로 변경사항 비교


* [x] **SR-4: 대시보드 및 통계**
  * 상태별, 우선순위별, 담당자별 SR 현황
  * 월별 SR 처리 추이 그래프



### Epic 2. OPEN API 현황조사 관리

* [x] **API-1: 현황조사 등록 및 관리**
  * CSV 일괄 업로드 기능
  * 기관코드 기반 자동 매칭


* [x] **API-2: SR 자동 생성**
  * 현황조사 등록/수정 시 연계 SR 자동 생성
  * 상태(status) 기반 SR 생성 조건 관리


* [x] **API-3: 검색 및 필터링**
  * 기관명, 담당자, 전환방식 등 다중 필터
  * 처리예정일자 기반 정렬



### Epic 3. 사용자 및 권한 관리

* [x] **USER-1: JWT 기반 인증**
  * Access Token (30분) + Refresh Token (7일)
  * 자동 토큰 갱신 (axios 인터셉터)


* [x] **USER-2: 역할 기반 권한 관리**
  * ADMIN, MANAGER, USER 역할
  * 역할별 기능 접근 제어


* [x] **USER-3: 사용자 정보 관리**
  * 프로필 수정, 비밀번호 변경
  * AES-256 개인정보 암호화



### Epic 4. 공통 코드 관리

* [x] **CODE-1: 행정표준코드 관리**
  * 기관 코드/명 관리
  * 계층 구조 지원 (상위기관 참조)


* [x] **CODE-2: 시스템 공통 코드**
  * SR 우선순위, 상태 코드 등
  * 카테고리별 코드 그룹 관리


---

## 🏗 3. 시스템 아키텍처 (System Architecture)

### 3.1 전체 구조

```
┌─────────────────────────────────────────────────────┐
│                    User Browser                      │
│              (http://localhost:8080)                 │
└──────────────────────┬──────────────────────────────┘
                       │
                       │ HTTP/HTTPS
                       ▼
┌─────────────────────────────────────────────────────┐
│              Spring Boot Application                 │
│  ┌───────────────────────────────────────────────┐  │
│  │   Static Resources (React SPA)                │  │
│  │   /static/**, /index.html                     │  │
│  └───────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────┐  │
│  │   REST API Endpoints                          │  │
│  │   /api/auth, /api/sr, /api/survey, ...       │  │
│  └───────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────┐  │
│  │   Spring Security + JWT Filter                │  │
│  └───────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────┐  │
│  │   Service Layer (Business Logic)              │  │
│  └───────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────┐  │
│  │   JPA Repository + JPA Converter(AES-256)     │  │
│  └───────────────────────────────────────────────┘  │
└──────────────────────┬──────────────────────────────┘
                       │ JDBC
                       ▼
┌─────────────────────────────────────────────────────┐
│  Database (H2 / CUBRID / MySQL / PostgreSQL)        │
└─────────────────────────────────────────────────────┘
```

### 3.2 백엔드 레이어 구조

```
com.srmanagement/
├── config/              # 설정 클래스
│   ├── SecurityConfig   # Spring Security 설정
│   ├── CorsConfig       # CORS 설정
│   └── CubridDialect    # CUBRID 방언
├── controller/          # REST API 엔드포인트
│   ├── AuthController
│   ├── SrController
│   ├── OpenApiSurveyController
│   ├── UserController
│   └── CommonCodeController
├── service/             # 비즈니스 로직
├── repository/          # JPA Repository (DB 접근)
├── entity/              # JPA 엔티티 (테이블 매핑)
├── dto/                 # DTO (요청/응답 객체)
│   ├── request/
│   └── response/
├── security/            # JWT 인증
│   ├── JwtTokenProvider
│   └── JwtAuthenticationFilter
├── converter/           # JPA Converter (암호화)
│   └── StringCryptoConverter
└── exception/           # 예외 처리
```

### 3.3 프론트엔드 구조

```
frontend/src/
├── components/          # UI 컴포넌트
│   ├── common/          # Header, Sidebar, Loading 등
│   ├── sr/              # SR 관련 컴포넌트
│   ├── survey/          # 현황조사 컴포넌트
│   ├── user/            # 사용자 관리 컴포넌트
│   └── admin/           # 공통코드 관리 컴포넌트
├── pages/               # 페이지 컴포넌트
├── services/            # API 클라이언트
│   └── api.ts           # 중앙 axios 인스턴스 (JWT 자동 처리)
├── store/               # Redux 스토어
│   ├── authSlice.ts
│   ├── srSlice.ts
│   └── themeSlice.ts
├── hooks/               # 커스텀 훅
├── utils/               # 유틸리티 함수
└── types/               # TypeScript 타입 정의
```

---

## ⚙️ 4. 배포 및 운영 (Deployment)

### 4.1 통합 배포 방식

**프론트엔드 + 백엔드 단일 JAR 배포**
```bash
# 1. 프론트엔드 빌드
cd frontend && npm run build

# 2. 빌드 결과물을 백엔드 static 폴더로 복사
cp -r dist/* ../backend/src/main/resources/static/

# 3. 백엔드 Maven 빌드
cd ../backend && mvn clean package -DskipTests

# 4. JAR 실행
java -jar target/sr-management-0.0.1-SNAPSHOT.jar
```

**자동 배포 스크립트 제공**
```bash
# 프로젝트 루트에서
./backend/scripts/start.sh      # 통합 빌드 및 실행
./backend/scripts/stop.sh       # 서버 중지
tail -f backend/logs/server.log # 로그 확인
```

### 4.2 데이터베이스 프로필

**H2 (기본 - 개발용)**
```bash
mvn spring-boot:run
# 또는
java -jar sr-management.jar
```

**CUBRID (운영용 권장)**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=cubrid
# 또는
java -jar sr-management.jar --spring.profiles.active=cubrid
```

**MySQL / PostgreSQL**
```bash
java -jar sr-management.jar --spring.profiles.active=mysql
java -jar sr-management.jar --spring.profiles.active=postgresql
```

### 4.3 환경 변수

프로덕션 환경에서는 환경 변수로 민감 정보를 관리합니다.

```bash
# JWT 시크릿 키
export JWT_SECRET=your-production-secret-key-min-256-bits

# 암호화 키 (AES-256, 32자)
export ENCRYPTION_SECRET=your-32-character-secret-key!!

# 데이터베이스 접속 정보
export DB_URL=jdbc:cubrid:localhost:33000:srdb:::
export DB_USERNAME=dba
export DB_PASSWORD=your-db-password
```

---

## 🔒 5. 보안 (Security)

### 5.1 인증 체계

**JWT (JSON Web Token) Stateless 인증**
* Access Token: 30분 유효
* Refresh Token: 7일 유효
* 자동 갱신: axios 인터셉터가 401 응답 시 자동 갱신

### 5.2 개인정보 보호

**AES-256 양방향 암호화 (JPA Converter 자동 처리)**
* 사용자 정보: 이름, 전화번호, 이메일
* SR 신청자 정보: 이름, 전화번호, 이메일
* 현황조사 담당자 정보: 이름, 전화번호, 이메일

**비밀번호 암호화**
* BCrypt 단방향 해시 알고리즘

### 5.3 권한 관리

**역할 기반 접근 제어 (RBAC)**
* ADMIN: 모든 기능 접근 가능
* MANAGER: SR 관리, 현황조사 관리
* USER: 조회 권한만

---

## 📊 6. 주요 테이블 구조 (Database Schema)

```sql
-- SR 테이블
CREATE TABLE sr (
    id BIGINT PRIMARY KEY,
    title VARCHAR(200),
    description TEXT,
    status VARCHAR(20),
    priority VARCHAR(20),
    assignee_id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- OPEN API 현황조사 테이블
CREATE TABLE open_api_survey (
    id BIGINT PRIMARY KEY,
    organization_code VARCHAR(10),
    organization_name VARCHAR(100),
    manager_name VARCHAR(50),    -- AES-256 암호화
    manager_phone VARCHAR(20),   -- AES-256 암호화
    manager_email VARCHAR(100),  -- AES-256 암호화
    current_method VARCHAR(50),
    target_method VARCHAR(50),
    status VARCHAR(20),
    created_at TIMESTAMP
);

-- 사용자 테이블
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) UNIQUE,
    password VARCHAR(100),       -- BCrypt 해시
    name VARCHAR(50),            -- AES-256 암호화
    email VARCHAR(100),          -- AES-256 암호화
    phone VARCHAR(20),           -- AES-256 암호화
    role VARCHAR(20),
    created_at TIMESTAMP
);

-- 공통코드 테이블
CREATE TABLE common_code (
    id BIGINT PRIMARY KEY,
    category VARCHAR(50),
    code VARCHAR(50),
    name VARCHAR(100),
    parent_code VARCHAR(50),
    sort_order INT
);
```

---

## 🚀 7. 개발 가이드 (Development Guide)

### 7.1 개발 환경 설정

**필수 요구사항**
* Java 17 이상
* Node.js 18 이상
* Maven 3.x

**프로젝트 클론 및 실행**
```bash
# 1. 프로젝트 클론
git clone <repository-url>
cd sr-manage-system

# 2. 백엔드 실행 (별도 터미널)
cd backend
mvn spring-boot:run

# 3. 프론트엔드 실행 (별도 터미널)
cd frontend
npm install
npm run dev

# 접속 URL
# Frontend: http://localhost:5173
# Backend API: http://localhost:8080
# H2 Console: http://localhost:8080/h2-console
```

### 7.2 코딩 규칙

**Backend**
* Controller는 절대 Entity를 직접 반환하지 않음 → DTO 사용
* 민감 정보는 JPA Converter가 자동 암호화 → 서비스에서 수동 암호화 금지
* 생성자 주입 사용 (Lombok `@RequiredArgsConstructor`)

**Frontend**
* 절대 새로운 `axios.create()` 인스턴스 생성 금지 → `services/api.ts` 사용
* 컴포넌트에서 토큰 갱신 로직 구현 금지 → 중앙에서 자동 처리
* Redux Toolkit의 `createAsyncThunk`로 비동기 처리

---

## 📚 8. 관련 문서 (Documentation)

* **[CLAUDE.md](../CLAUDE.md)** - Claude Code 작업 가이드
* **[API.md](API.md)** - REST API 명세서
* **[DATABASE.md](DATABASE.md)** - 데이터베이스 설계
* **[JPA_CONVERTER.md](JPA_CONVERTER.md)** - 개인정보 암호화 가이드
* **[OPERATION_GUIDE.md](OPERATION_GUIDE.md)** - 운영 가이드
* **[HISTORY_20251205.md](HISTORY_20251205.md)** - 최신 변경 이력

---

## 🎯 9. 향후 개발 계획 (Future Plans)

### Phase 1 - 완료 ✅
* SR 관리 핵심 기능
* OPEN API 현황조사 관리
* JWT 인증 및 권한 관리
* 다중 DB 지원 (H2, CUBRID, MySQL, PostgreSQL)
* 통합 배포 스크립트

### Phase 2 - 진행 중 🚧
* 다크모드 지원 (완료)
* 대시보드 고도화
* 파일 첨부 기능 강화
* 알림 기능

### Phase 3 - 계획 📋
* 이메일 알림 기능
* 배치 작업 스케줄링
* 감사 로그(Audit Log)
* 데이터 백업/복구 자동화

---

**문의사항이나 기여를 원하시면 프로젝트 관리자에게 연락하세요.**