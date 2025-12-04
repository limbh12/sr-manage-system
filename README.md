# SR Management System (SR관리 시스템)

PUBC 오픈API 전환 지원을 위한 SR관리를 위함

## 프로젝트 개요

SR(Service Request) 관리 시스템은 서비스 요청을 효율적으로 관리하고 추적하기 위한 웹 애플리케이션입니다.
사용자는 SR을 등록, 조회, 수정, 삭제할 수 있으며, JWT 기반 인증을 통해 보안을 유지합니다.

## 기술 스택

### Backend
- **Framework**: Spring Boot 3.x
- **Language**: Java 17+
- **Build Tool**: Maven
- **Authentication**: JWT (JSON Web Token)
- **Database**: H2 (Dev) / CUBRID 10.x+ / MySQL 8.x / PostgreSQL

### Frontend
- **Framework**: React 18.x
- **Language**: TypeScript
- **Build Tool**: Vite
- **State Management**: Redux Toolkit
- **HTTP Client**: Axios

## 프로젝트 구조

```
sr-manage-system/
├── docs/                    # 설계 문서
│   ├── API.md              # API 명세서
│   └── DATABASE.md         # 데이터베이스 설계 문서
├── backend/                 # Spring Boot 백엔드
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/srmanagement/
│       │   ├── config/      # 설정 클래스
│       │   ├── controller/  # REST 컨트롤러
│       │   ├── service/     # 비즈니스 로직
│       │   ├── repository/  # 데이터 접근 계층
│       │   ├── entity/      # JPA 엔티티
│       │   ├── dto/         # 데이터 전송 객체
│       │   ├── security/    # 보안 관련 클래스
│       │   └── exception/   # 예외 처리
│       └── resources/
│           └── application.yml
└── frontend/                # React 프론트엔드
    ├── package.json
    ├── vite.config.ts
    └── src/
        ├── components/      # 재사용 가능한 컴포넌트
        ├── pages/           # 페이지 컴포넌트
        ├── hooks/           # 커스텀 훅
        ├── services/        # API 서비스
        ├── store/           # Redux 스토어
        ├── utils/           # 유틸리티 함수
        └── types/           # TypeScript 타입 정의
```

## 주요 기능

### 인증 시스템
- JWT 기반 인증
- Access Token (유효기간: 30분)
- Refresh Token (유효기간: 7일)
- 자동 토큰 갱신

### SR 관리
- SR 등록/조회/수정/삭제 (CRUD)
- SR 상태 관리 (OPEN → IN_PROGRESS → RESOLVED → CLOSED)
- SR 우선순위 설정 (LOW, MEDIUM, HIGH, CRITICAL)
- 담당자 지정
- 변경 이력 및 상세 비교 (Diff View)

### 사용자 관리
- 로그인 (사용자 등록은 관리자 전용)
- 역할 기반 권한 관리 (ADMIN, USER)

### 기타
- 행정기관 검색 (행정표준코드 기반)
- OPEN API 현황조사 관리 (등록/수정/조회, 파일 첨부 및 다운로드)

## 실행 방법

### 사전 요구사항
- Java 17 이상
- Node.js 18 이상
- CUBRID 10.x 이상 (또는 MySQL/PostgreSQL)

### Backend 실행
### Backend 실행

```bash
cd backend

# 데이터베이스 설정 (application.yml 수정 필요)

# 빌드
mvn clean package

# 실행
mvn spring-boot:run
```
### Frontend 실행

#### 1. Dev Container 환경 (VS Code)
VS Code에서 Dev Container로 프로젝트를 열었다면, 의존성 설치(`npm install`)가 자동으로 완료됩니다.
터미널에서 바로 개발 서버를 실행하세요.

```bash
cd frontend
npm run dev
```

#### 2. 로컬 환경 (Node.js 직접 설치)
Node.js가 설치된 로컬 환경에서 실행하는 경우:

```bash
cd frontend

# 의존성 설치
npm install

# 개발 서버 실행
npm run dev
```

### 접속 정보
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080

## 환경 설정

상세한 설정 방법 및 운영 가이드는 [docs/OPERATION_GUIDE.md](docs/OPERATION_GUIDE.md)를 참조하세요.

### Backend 환경 변수 (application.yml)
```yaml
spring:
  datasource:
    # H2 File Mode (Default)
    url: jdbc:h2:file:/absolute/path/to/project/backend/data/srdb;AUTO_SERVER=TRUE
    username: sa
    password: sa1234!
    
    # CUBRID Example
    # url: jdbc:cubrid:localhost:33000:sr_db:::
    # username: your_username
    # password: your_password
    # driver-class-name: cubrid.jdbc.driver.CUBRIDDriver
  jpa:
    hibernate:
      ddl-auto: create # 서버 시작 시 DB 초기화 (create), 유지하려면 update 사용
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect # or com.srmanagement.config.CubridDialect

jwt:
  secret: your_jwt_secret_key
  access-token-validity: 1800000    # 30분 (ms)
  refresh-token-validity: 604800000 # 7일 (ms)
```

## API 문서

상세한 API 명세는 [docs/API.md](docs/API.md)를 참조하세요.

## 변경 이력

프로젝트의 주요 변경 사항은 `docs/HISTORY_YYYYMMDD.md` 형식의 파일로 날짜별로 관리됩니다.
가장 최신의 변경 이력은 [docs/HISTORY_20251202.md](docs/HISTORY_20251202.md)에서 확인할 수 있습니다.

## 데이터베이스 설계

데이터베이스 스키마 및 ERD는 [docs/DATABASE.md](docs/DATABASE.md)를 참조하세요.

## 라이선스

This project is licensed under the MIT License.
