# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

SR(Service Request) 관리 시스템 - PUBC 오픈API 전환 지원을 위한 웹 애플리케이션
- Backend: Spring Boot 3.2 (Java 17) + Maven
- Frontend: React 18 (TypeScript) + Vite + Redux Toolkit
- Database: H2 (기본/개발), CUBRID 10.x+, MySQL 8.x, PostgreSQL
- AI Tools: Python 3.13 + Ollama (로컬 LLM) - Wiki AI 검색용

## 개발 명령어

### 통합 실행 (권장)

**프론트엔드 + 백엔드 통합 실행 (프로덕션 모드)**
```bash
# 프로젝트 루트에서 실행
./backend/scripts/start.sh

# 서버 중지
./backend/scripts/stop.sh

# 로그 확인
tail -f backend/logs/server.log
```

**자동으로 수행되는 작업:**
1. 프론트엔드 빌드 (`npm run build`)
2. 빌드 결과물을 백엔드 static 폴더로 복사
3. 백엔드 Maven 빌드 (`mvn clean package -DskipTests`)
4. 서버 시작 (백그라운드 실행)

**접속 URL:**
- 통합 서버: http://localhost:8080 (프론트엔드 + API)
- H2 Console: http://localhost:8080/h2-console (개발 환경만)

### 개발 모드 (별도 실행)

개발 중에는 프론트엔드와 백엔드를 별도로 실행하여 Hot Reload를 활용할 수 있습니다.

**Backend (backend/ 디렉토리에서)**
```bash
# 개발 서버 실행 (H2 파일 모드)
mvn spring-boot:run

# 빌드
mvn clean package

# 테스트
mvn test

# 프로필 지정 실행
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
mvn spring-boot:run -Dspring-boot.run.profiles=postgresql
mvn spring-boot:run -Dspring-boot.run.profiles=cubrid
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

**Frontend (frontend/ 디렉토리에서)**
```bash
# 개발 서버 실행 (Hot Reload)
npm run dev

# 빌드
npm run build

# TypeScript 타입 체크 + 빌드
tsc && vite build

# Lint
npm run lint

# 프로덕션 빌드 미리보기
npm run preview
```

**개발 모드 접속 URL:**
- Frontend (개발 서버): http://localhost:5173
- Backend API: http://localhost:8080
- H2 Console: http://localhost:8080/h2-console (개발 환경만)

## 핵심 아키텍처

### Backend 레이어 구조
```
com.srmanagement/
├── config/         # SecurityConfig, CubridDialect 등
├── controller/     # REST API 엔드포인트 (@RestController)
├── service/        # 비즈니스 로직
├── repository/     # JPA Repository
├── entity/         # JPA 엔티티 (DB 테이블과 매핑)
├── dto/            # 데이터 전송 객체 (API 요청/응답)
├── security/       # JWT 인증 (JwtTokenProvider, JwtAuthenticationFilter)
├── converter/      # JPA Converter (AES-256 암호화/복호화)
└── exception/      # 예외 처리
```

**중요 규칙:**
- Controller는 **절대 Entity를 직접 반환하지 않음** → 항상 DTO로 매핑
- 모든 엔드포인트는 `ResponseEntity<T>` 사용
- 생성자 주입 사용 (Lombok `@RequiredArgsConstructor`)
- 민감 정보(이름, 전화번호, 이메일)는 JPA Converter로 자동 암호화 → **서비스에서 수동 암호화 금지**

### Frontend 구조
```
frontend/src/
├── components/     # 재사용 가능한 UI 컴포넌트
│   ├── common/     # 공통 컴포넌트
│   ├── sr/         # SR 관련 컴포넌트
│   ├── survey/     # 설문조사 관련 컴포넌트
│   └── user/       # 사용자 관리 컴포넌트
├── pages/          # 라우팅 페이지 컴포넌트
├── services/       # API 클라이언트 (axios)
│   └── api.ts      # **중앙 axios 인스턴스 (JWT 자동 처리)**
├── store/          # Redux Toolkit 스토어
│   ├── authSlice.ts
│   └── srSlice.ts
├── hooks/          # 커스텀 훅 (useAuth 등)
├── utils/          # 유틸리티 함수
└── types/          # TypeScript 타입 정의
```

**중요 규칙:**
- **절대 새로운 `axios.create()` 인스턴스를 생성하지 말 것**
  → `src/services/api.ts`의 export된 인스턴스만 사용
  → JWT 주입과 401 토큰 갱신 로직이 인터셉터에 이미 구현됨
- Redux Toolkit의 `createAsyncThunk`로 비동기 API 호출 처리
- 함수형 컴포넌트 + Hooks 사용

## 인증 시스템 (JWT)

- **Stateless JWT 인증**: Access Token (30분) + Refresh Token (7일)
- **자동 토큰 갱신**: `frontend/src/services/api.ts`의 axios 인터셉터가 401 응답 시 자동으로 refresh
- **컴포넌트에서 토큰 갱신 로직 구현 금지** → 중앙에서 자동 처리됨

### 관련 파일
- Backend: `backend/src/main/java/com/srmanagement/security/`
- Frontend: `frontend/src/hooks/useAuth.ts`, `frontend/src/services/api.ts`

## 데이터베이스 설정

### H2 (기본 프로필)
- **파일 모드** (`backend/data/srdb`) → 재시작해도 데이터 유지
- `ddl-auto: create` → 서버 시작 시 DB 초기화 (개발용)
- 운영 환경은 `ddl-auto: update` 사용 (데이터 유지)

### 프로필 전환
```bash
# MySQL
mvn spring-boot:run -Dspring-boot.run.profiles=mysql

# PostgreSQL
mvn spring-boot:run -Dspring-boot.run.profiles=postgresql

# CUBRID (반드시 CubridDialect 사용)
mvn spring-boot:run -Dspring-boot.run.profiles=cubrid
```

### 설정 파일
- `backend/src/main/resources/application.yml`

## 보안 및 암호화

### 비밀번호
- BCrypt 해시 알고리즘으로 단방향 암호화

### 개인정보
- AES-256 양방향 암호화 (JPA Converter 자동 처리)
- 대상: 사용자 정보, SR 신청자 정보, OPEN API 현황조사 담당자 정보
- 암호화 키: `application.yml`의 `encryption.secret` (프로덕션에서는 환경변수 `ENCRYPTION_SECRET` 사용)

**중요:** 서비스 로직에서 암호화/복호화를 수동으로 하지 말 것 → JPA가 자동 처리

### 관련 파일
- `backend/src/main/java/com/srmanagement/converter/`
- 상세 문서: `docs/JPA_CONVERTER.md`

## 주요 문서

- **API 명세**: `docs/API.md`
- **데이터베이스 설계**: `docs/DATABASE.md`
- **개인정보 암호화 가이드**: `docs/JPA_CONVERTER.md`
- **운영 가이드**: `docs/OPERATION_GUIDE.md`
- **변경 이력**: `docs/HISTORY_YYYYMMDD.md` (최신: `docs/HISTORY_20251218.md`)
- **Wiki Phase 1**: `docs/HISTORY_20251219_WIKI_PHASE1.md`
- **프로젝트 전체 개요**: `docs/PROJECT_OVERVIEW.md`

## SR 관리 핵심 기능

- SR 상태 흐름: OPEN → IN_PROGRESS → RESOLVED → CLOSED
- 우선순위: LOW, MEDIUM, HIGH, CRITICAL
- 변경 이력 추적 및 Diff View 지원
- 담당자 지정 및 검색 (행정표준코드 기반)
- 처리예정일자 관리 (마감 임박 시 시각적 강조)

## Wiki 시스템 (AI 기반 지식 관리)

### 아키텍처
Wiki 기능은 `backend/src/main/java/com/srmanagement/wiki/` 패키지에 별도 구현됨:
```
wiki/
├── controller/     # WikiDocumentController, WikiCategoryController, WikiSearchController 등
├── service/        # WikiDocumentService, AiSearchService, ContentEmbeddingService 등
├── repository/     # WikiDocumentRepository, ContentEmbeddingRepository 등
├── entity/         # WikiDocument, WikiCategory, WikiVersion, WikiFile, ContentEmbedding 등
└── dto/            # Request/Response DTO
```

### 핵심 기능
- **마크다운 기반 문서**: Toast UI Editor (편집) + react-markdown (렌더링)
- **계층형 카테고리**: Self-referencing 구조 (무제한 depth)
- **버전 관리**: 문서 수정 시 자동 버전 생성, 롤백 지원
- **파일 첨부**: 이미지 업로드, 문서 첨부 (UUID 기반 저장)
- **SR-Wiki 연계**: ManyToMany 관계, 슬라이드 패널 네비게이션
- **AI 검색**: Ollama 임베딩 + 코사인 유사도 기반 시맨틱 검색
- **PDF 변환**: Apache Tika + AI 구조 보정 (표/수식 인식)

### AI 검색 관련 파일
- Backend: `wiki/service/AiSearchService.java`, `wiki/service/ContentEmbeddingService.java`
- Frontend: `services/aiSearchService.ts`, `components/wiki/AiSearch.tsx`
- 임베딩 모델: Ollama `nomic-embed-text` (768차원)
- 벡터 저장: H2 DB `content_embedding` 테이블

### PDF 변환 및 AI 구조 보정 (D-3)
- **PDF 텍스트 추출**: `wiki/service/PdfConversionService.java` (Apache Tika + PDFBox)
- **AI 구조 보정**: `wiki/service/StructureEnhancementService.java`
  - 표(Table) 구조 인식 및 마크다운 변환
  - 수식(LaTeX) 인식 및 변환
  - Pandoc 통합 (선택적, 고품질 표/수식 변환)
- **설정**: `application.yml`의 `wiki.structure-enhancement` 섹션
- **API**: `/api/wiki/files/upload-pdf-enhanced` (AI 구조 보정 적용 PDF 업로드)

### Wiki API 엔드포인트
- 문서: `/api/wiki/documents/**`
- 카테고리: `/api/wiki/categories/**`
- 버전: `/api/wiki/documents/{id}/versions/**`
- 파일: `/api/wiki/files/**`
- AI 검색: `/api/wiki/search/**`

## 흔한 실수 방지

### Backend
- Entity를 직접 API 응답으로 반환하지 말 것 (무한 재귀, 불필요한 필드 노출)
- 민감 정보 필드를 서비스에서 수동 암호화하지 말 것 (JPA Converter가 자동 처리)

### Frontend
- `axios.create()`로 새 인스턴스를 생성하지 말 것 (JWT 인터셉터 우회됨)
- 컴포넌트에서 토큰 갱신 로직을 직접 구현하지 말 것 (중앙 처리됨)

### Database
- CUBRID 사용 시 반드시 `CubridDialect` 확인
- H2는 파일 모드 (메모리 모드 아님)

### Wiki
- 문서 저장 시 AI 임베딩 자동 생성 (Ollama 서버 필요)
- 파일 업로드 경로: `./data/wiki-uploads/`
- PDF 변환 시 Apache PDFBox 사용 (`PdfConversionService.java`)
