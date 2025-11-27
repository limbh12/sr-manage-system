# SR 관리 시스템 Copilot 지침

## 프로젝트 개요

이 프로젝트는 서비스 요청(SR)을 효율적으로 관리하고 추적하기 위한 SR 관리 시스템입니다. 시스템 구성:

- **백엔드**: Java 17+ 기반 Spring Boot 3.x 애플리케이션
- **프론트엔드**: TypeScript와 Vite를 사용한 React 18.x 애플리케이션

이 애플리케이션은 JWT 기반 인증, 서비스 요청에 대한 CRUD 작업, 역할 기반 접근 제어(ADMIN/USER)를 지원합니다.

## 프로젝트 구조

```
sr-manage-system/
├── docs/               # API 및 데이터베이스 문서
├── backend/            # Spring Boot 백엔드
│   └── src/main/java/com/srmanagement/
│       ├── config/     # 설정 클래스
│       ├── controller/ # REST 컨트롤러
│       ├── service/    # 비즈니스 로직
│       ├── repository/ # 데이터 접근 계층
│       ├── entity/     # JPA 엔티티
│       ├── dto/        # 데이터 전송 객체
│       ├── security/   # 보안 클래스 (JWT)
│       └── exception/  # 예외 처리
└── frontend/           # React 프론트엔드
    └── src/
        ├── components/ # 재사용 가능한 컴포넌트
        ├── pages/      # 페이지 컴포넌트
        ├── hooks/      # 커스텀 훅
        ├── services/   # API 서비스
        ├── store/      # Redux 스토어
        ├── utils/      # 유틸리티 함수
        └── types/      # TypeScript 타입 정의
```

## 빌드 및 테스트 명령어

### 백엔드 (Spring Boot)

```bash
cd backend

# 프로젝트 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 애플리케이션 실행
./gradlew bootRun
```

### 프론트엔드 (React/TypeScript)

```bash
cd frontend

# 의존성 설치
npm install

# 린트 실행
npm run lint

# 프로젝트 빌드
npm run build

# 개발 서버 실행
npm run dev
```

## 코딩 표준

### Java/Spring Boot (백엔드)

- Java 17+ 기능 사용
- 표준 Java 명명 규칙 준수 (변수/메서드는 camelCase, 클래스는 PascalCase)
- 엔티티 클래스에는 Lombok 어노테이션 사용 (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`)
- 클래스와 public 메서드에 Javadoc 주석 추가
- REST API 엔드포인트에는 `@RestController` 사용
- 비즈니스 로직에는 `@Service` 사용
- 데이터 접근에는 `@Repository` 사용
- `com.srmanagement` 하위의 기존 패키지 구조 준수

### TypeScript/React (프론트엔드)

- strict 모드가 활성화된 TypeScript 사용
- React 훅과 함께 함수형 컴포넌트 사용
- `types/` 디렉토리에서 타입과 인터페이스 내보내기
- import 시 경로 별칭(`@/`) 사용
- 컴포넌트와 함수에 JSDoc 주석 추가
- 기존 ESLint 규칙 준수
- 상태 관리에는 Redux Toolkit 사용
- HTTP 요청에는 Axios 사용

## 도메인 개념

### SR (서비스 요청)

- **상태(Status)**: OPEN → IN_PROGRESS → RESOLVED → CLOSED
- **우선순위(Priority)**: LOW, MEDIUM, HIGH, CRITICAL
- **요청자(Requester)**: SR을 생성한 사용자
- **담당자(Assignee)**: SR 처리를 담당하는 사용자

### 사용자 역할

- **ADMIN**: 모든 사용자와 SR을 관리할 수 있음
- **USER**: 자신의 SR만 생성하고 관리할 수 있음

## API 가이드라인

- 기본 URL: `/api`
- 인증에는 JWT Bearer 토큰 사용
- RESTful 규칙 준수
- JSON 형식으로 응답 반환
- 표준 HTTP 상태 코드 사용 (200, 201, 400, 401, 403, 404, 500)
- 자세한 API 명세는 `docs/API.md` 참조

## 테스트 요구사항

- 백엔드의 새로운 서비스 메서드에 대한 단위 테스트 작성
- 백엔드 테스트에는 JUnit 5와 Spring Test 사용
- 변경 사항 커밋 전 테스트 통과 확인
- 보안이 적용된 엔드포인트의 인증 및 권한 부여 테스트

## 보안 고려사항

- 민감한 데이터(비밀번호, API 키, JWT 시크릿)는 절대 커밋하지 않음
- 설정에는 환경 변수 사용
- 모든 사용자 입력 유효성 검사
- 토큰 처리 시 JWT 모범 사례 준수
- 인증 및 권한 부여에는 Spring Security 사용
