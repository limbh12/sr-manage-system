# SR Management System - AI Coding Agent Instructions

## 1. Project Context & Architecture
- **Goal**: Service Request (SR) management system for PUBC Open API transition support.
- **Stack**:
  - **Backend**: Spring Boot 3.2+ (Java 17), Maven.
  - **Frontend**: React 18 (TypeScript), Vite, Redux Toolkit.
  - **Database**: H2 (Dev/Default), CUBRID 10.x+, MySQL 8.x, PostgreSQL.
- **Key Documentation**:
  - `docs/API.md`: REST API specification.
  - `docs/DATABASE.md`: Schema & ERD.
  - `docs/JPA_CONVERTER.md`: Sensitive data encryption guide.

## 2. Critical Developer Workflows
- **Backend**:
  - Run: `mvn spring-boot:run` (in `backend/`).
  - Build: `mvn clean package`.
  - Profiles: Default is H2. Use `-Dspring.profiles.active=cubrid` for CUBRID.
- **Frontend**:
  - Run: `npm run dev` (in `frontend/`).
  - Build: `npm run build`.
  - API Proxy: Configured in `vite.config.ts` or via CORS in backend.

## 3. Codebase Conventions & Patterns

### Backend (Spring Boot)
- **Controller**:
  - **NEVER** return Entities directly. Always map to DTOs (`com.srmanagement.dto`).
  - Use `ResponseEntity<T>` for all endpoints.
  - Validate inputs with `@Valid`.
- **Service/Repository**:
  - Use Constructor Injection (Lombok `@RequiredArgsConstructor`).
  - **Encryption**: Sensitive fields (name, phone, email) are encrypted automatically via JPA Converters (`@Convert`). **DO NOT** manually encrypt/decrypt in service logic unless necessary.
- **Security**:
  - Stateless JWT authentication.
  - `JwtAuthenticationFilter` handles token validation.

### Frontend (React + TypeScript)
- **API Interaction**:
  - **ALWAYS** use the exported instance from `src/services/api.ts`.
  - **NEVER** create a new `axios.create()` instance. The default one handles JWT injection and 401 refresh logic automatically.
- **State Management**:
  - Use Redux Toolkit (`src/store`) for global state (auth, SR data).
  - Use `createAsyncThunk` for async API calls.
- **Components**:
  - Functional components with Hooks.
  - Place reusable UI in `src/components/common`.

## 4. Common Pitfalls & "Do Nots"
- **Token Refresh**: Do not implement token refresh logic in components. It is handled centrally in `api.ts` interceptors.
- **Database Dialect**: When working with CUBRID, ensure `CubridDialect` is used.
- **H2 Configuration**: Default H2 is in **FILE** mode (`backend/data/srdb`), not memory. Data persists across restarts.

## 5. Key File Paths
- **Auth Logic**: `backend/src/main/java/com/srmanagement/security/`, `frontend/src/hooks/useAuth.ts`
- **API Client**: `frontend/src/services/api.ts`
- **DB Config**: `backend/src/main/resources/application.yml`
- **Encryption**: `backend/src/main/java/com/srmanagement/converter/`

---

# SR 관리 시스템 - AI 코딩 에이전트 지침 (Korean)

## 1. 프로젝트 컨텍스트 및 아키텍처
- **목표**: PUBC 오픈API 전환 지원을 위한 SR(서비스 요청) 관리 시스템.
- **기술 스택**:
  - **백엔드**: Spring Boot 3.2+ (Java 17), Maven.
  - **프론트엔드**: React 18 (TypeScript), Vite, Redux Toolkit.
  - **데이터베이스**: H2 (개발/기본), CUBRID 10.x+, MySQL 8.x, PostgreSQL.
- **주요 문서**:
  - `docs/API.md`: REST API 명세서.
  - `docs/DATABASE.md`: 스키마 및 ERD.
  - `docs/JPA_CONVERTER.md`: 민감 데이터 암호화 가이드.

## 2. 핵심 개발자 워크플로우
- **백엔드**:
  - 실행: `mvn spring-boot:run` (`backend/` 폴더에서).
  - 빌드: `mvn clean package`.
  - 프로필: 기본값은 H2. CUBRID 사용 시 `-Dspring.profiles.active=cubrid` 사용.
- **프론트엔드**:
  - 실행: `npm run dev` (`frontend/` 폴더에서).
  - 빌드: `npm run build`.
  - API 프록시: `vite.config.ts`에 설정되거나 백엔드 CORS 설정 사용.

## 3. 코드베이스 컨벤션 및 패턴

### 백엔드 (Spring Boot)
- **Controller**:
  - **절대** Entity를 직접 반환하지 말 것. 항상 DTO(`com.srmanagement.dto`)로 매핑할 것.
  - 모든 엔드포인트에 `ResponseEntity<T>` 사용.
  - `@Valid`로 입력값 검증.
- **Service/Repository**:
  - 생성자 주입 사용 (Lombok `@RequiredArgsConstructor`).
  - **암호화**: 민감 필드(이름, 전화번호, 이메일)는 JPA Converter(`@Convert`)를 통해 자동 암호화됨. 서비스 로직에서 수동으로 암호화/복호화 **하지 말 것**.
- **Security**:
  - 무상태(Stateless) JWT 인증.
  - `JwtAuthenticationFilter`가 토큰 검증 처리.

### 프론트엔드 (React + TypeScript)
- **API 상호작용**:
  - **항상** `src/services/api.ts`에서 export된 인스턴스를 사용할 것.
  - **절대** 새로운 `axios.create()` 인스턴스를 생성하지 말 것. 기본 인스턴스가 JWT 주입 및 401 갱신 로직을 자동 처리함.
- **상태 관리**:
  - 전역 상태(인증, SR 데이터)는 Redux Toolkit(`src/store`) 사용.
  - 비동기 API 호출에는 `createAsyncThunk` 사용.
- **컴포넌트**:
  - Hook을 사용하는 함수형 컴포넌트.
  - 재사용 가능한 UI는 `src/components/common`에 위치.

## 4. 흔한 실수 및 "하지 말아야 할 것"
- **토큰 갱신**: 컴포넌트 내에 토큰 갱신 로직을 구현하지 말 것. `api.ts` 인터셉터에서 중앙 처리됨.
- **데이터베이스 방언**: CUBRID 사용 시 반드시 `CubridDialect`가 사용되는지 확인할 것.
- **H2 설정**: 기본 H2는 메모리가 아닌 **파일** 모드(`backend/data/srdb`)임. 재시작 후에도 데이터가 유지됨.

## 5. 주요 파일 경로
- **인증 로직**: `backend/src/main/java/com/srmanagement/security/`, `frontend/src/hooks/useAuth.ts`
- **API 클라이언트**: `frontend/src/services/api.ts`
- **DB 설정**: `backend/src/main/resources/application.yml`
- **암호화**: `backend/src/main/java/com/srmanagement/converter/`
