# SR 관리 시스템 - AI 코딩 에이전트 지침

## 프로젝트 개요
서비스 요청(SR) 관리를 위한 풀스택 애플리케이션입니다.
- **백엔드**: Spring Boot 3.x (Java 17+), Maven, MySQL/PostgreSQL.
- **프론트엔드**: React 18.x (TypeScript), Vite, Redux Toolkit, Axios.
- **인증**: JWT 기반 무상태(Stateless) 인증.

## 아키텍처 및 패턴

### 백엔드 (Spring Boot)
- **빌드 도구**: Maven (`pom.xml`). *참고: README에는 Gradle로 잘못 기재되어 있음.*
- **API 설계**:
  - `com.srmanagement.controller` 패키지의 RESTful 컨트롤러.
  - DTO를 감싸는 `ResponseEntity<T>` 반환 (예: `SrResponse`).
  - **절대** 컨트롤러에서 Entity를 직접 반환하지 말 것. 항상 Response DTO로 매핑하여 반환.
- **보안**:
  - `SecurityConfig.java`에 설정됨.
  - 무상태(Stateless) 세션 관리.
  - `JwtAuthenticationFilter`가 토큰 유효성 검증 처리.
  - CSRF 비활성화됨.
- **예외 처리**:
  - `GlobalExceptionHandler.java`에서 중앙 집중식 처리.
  - 표준 `ErrorResponse` 구조 반환: `{ error, message, timestamp, path }`.
  - 비즈니스 로직 오류에는 `CustomException` 사용.
- **데이터 접근**:
  - `com.srmanagement.repository`의 JPA 리포지토리.
  - `com.srmanagement.entity`의 엔티티.

### 프론트엔드 (React + Vite)
- **API 클라이언트**:
  - 모든 HTTP 요청에 `src/services/api.ts` 사용.
  - **새로운 Axios 인스턴스를 생성하지 말 것.** 기본 export된 인스턴스가 JWT 주입 및 자동 토큰 갱신(401 재시도 로직)을 처리함.
- **상태 관리**:
  - Redux Toolkit (`src/store`).
  - API 상호작용에 `createAsyncThunk` 사용 (`src/store/srSlice.ts` 참조).
  - Slice에서 `loading`, `error`, 데이터 상태 관리.
- **컴포넌트**:
  - Hook을 사용하는 함수형 컴포넌트.
  - 재사용 가능한 UI 컴포넌트는 `src/components/common`에 위치.
  - 기능별 컴포넌트는 `src/components/{feature}`에 위치 (예: `src/components/sr`).
- **폼(Forms)**:
  - Redux 액션을 디스패치하기 전에 로컬 상태(useState)로 폼 상태 관리.
  - 패턴 참조: `SrForm.tsx`의 `onSubmit` prop이 액션 디스패치 처리.

## 주요 워크플로우 및 명령어

### 백엔드
- **실행**: `mvn spring-boot:run` (`backend/` 디렉토리에서)
- **테스트**: `mvn test`
- **패키징**: `mvn clean package`

### 프론트엔드
- **설치**: `npm install` (`frontend/` 디렉토리에서)
- **개발 서버**: `npm run dev`
- **빌드**: `npm run build`

## 코딩 컨벤션
- **Java**:
  - Lombok(`@Data`, `@Builder` 등)을 사용하여 보일러플레이트 감소.
  - 필드 주입(Field Injection) 지양; 생성자 주입(Constructor Injection) 사용 권장.
  - 컨트롤러 메서드는 `@Valid`를 사용하여 입력값 검증.
- **TypeScript/React**:
  - Strict 모드 활성화됨.
  - 모든 Props와 State에 대한 인터페이스 정의.
  - 공유 타입 정의(모델, DTO)는 `src/types/index.ts` 사용.
  - `.then()` 대신 `async/await` 선호.

## 주의사항 및 흔한 실수
- **토큰 갱신**: 프론트엔드 `api.ts` 인터셉터가 갱신을 자동으로 처리함. 컴포넌트 내에 수동 갱신 로직을 구현하지 말 것.
- **CORS**: `CorsConfig.java`에 설정됨. 새로운 Origin 추가 시 이곳을 수정.
- **Entity/DTO 매핑**: 양방향 매핑이 처리되는지 확인 (주로 Service 계층이나 Mapper/Builder 패턴을 통해).
