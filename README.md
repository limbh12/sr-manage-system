# SR Management System (SR관리 시스템)

PUBC 오픈API 전환 지원을 위한 SR관리를 위함

## 프로젝트 개요

SR(Service Request) 관리 시스템은 서비스 요청을 효율적으로 관리하고 추적하기 위한 웹 애플리케이션입니다.
사용자는 SR을 등록, 조회, 수정, 삭제할 수 있으며, JWT 기반 인증을 통해 보안을 유지합니다.

## 기술 스택

### Backend
- **Framework**: Spring Boot 3.x
- **Language**: Java 17+
- **Build Tool**: Gradle
- **Authentication**: JWT (JSON Web Token)
- **Database**: MySQL 8.x / PostgreSQL

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
│   ├── build.gradle
│   ├── settings.gradle
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

### 사용자 관리
- 회원가입/로그인
- 역할 기반 권한 관리 (ADMIN, USER)

## 실행 방법

### 사전 요구사항
- Java 17 이상
- Node.js 18 이상
- MySQL 8.x 또는 PostgreSQL

### Backend 실행

```bash
cd backend

# 데이터베이스 설정 (application.yml 수정 필요)
# Gradle Wrapper 실행 권한 부여 (Unix/Linux/Mac)
chmod +x gradlew

# 빌드
./gradlew build

# 실행
./gradlew bootRun
```

### Frontend 실행

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

### Backend 환경 변수 (application.yml)
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/sr_management
    username: your_username
    password: your_password
  jpa:
    hibernate:
      ddl-auto: update

jwt:
  secret: your_jwt_secret_key
  access-token-validity: 1800000    # 30분 (ms)
  refresh-token-validity: 604800000 # 7일 (ms)
```

## API 문서

상세한 API 명세는 [docs/API.md](docs/API.md)를 참조하세요.

## 데이터베이스 설계

데이터베이스 스키마 및 ERD는 [docs/DATABASE.md](docs/DATABASE.md)를 참조하세요.

## 라이선스

This project is licensed under the MIT License.
