# PRD: AI-Powered Knowledge Wiki for SR Management System

**Product Requirements Document**

| 항목 | 내용 |
|------|------|
| **문서 버전** | 1.1 |
| **작성일** | 2024-12-21 |
| **상태** | Phase 1-5 완료 |
| **관련 문서** | [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md), [API.md](API.md), [DATABASE.md](DATABASE.md) |

---

## 1. Executive Summary

### 1.1 제품 개요

SR 관리 시스템에 AI 기반 지능형 위키 기능을 추가하여, **폐쇄망 환경**에서 지식의 체계적 축적과 AI를 활용한 지능형 검색 및 분석을 제공합니다.

### 1.2 핵심 가치 제안

| 가치 | 설명 |
|------|------|
| **지식 체계화** | SR 처리 과정에서 발생하는 기술 문서, 가이드, FAQ를 체계적으로 관리 |
| **PDF 자동 변환** | PDF 형태의 기술 문서를 마크다운으로 자동 변환하여 검색 및 편집 가능 |
| **AI 지능형 검색** | 자연어 질문에 대해 문서 기반 AI 답변 생성 (RAG) |
| **폐쇄망 지원** | Local LLM(Ollama)을 활용하여 외부 네트워크 없이 동작 |

### 1.3 목표 사용자

- SR 처리 담당자
- 기술 문서 작성자/관리자
- 신규 입사자 (온보딩)
- 시스템 관리자

---

## 2. Problem Statement

### 2.1 현재 문제점

| 문제 | 영향 | 심각도 |
|------|------|--------|
| 기술 문서 분산 관리 | 필요한 정보를 찾는 데 시간 소요 | HIGH |
| PDF 문서 검색 불가 | 문서 내용을 활용하기 어려움 | HIGH |
| 인수인계 시 정보 손실 | 담당자 변경 시 지식 단절 | MEDIUM |
| 반복 질문 처리 | 동일한 질문에 반복 답변 필요 | MEDIUM |

### 2.2 해결 방안

```
┌─────────────────────────────────────────────────────────────┐
│                    AS-IS                                     │
│  - PDF 파일 개별 관리                                        │
│  - 이메일/메신저로 문서 공유                                 │
│  - 수동 검색 및 답변                                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    TO-BE                                     │
│  - 통합 위키 시스템에서 문서 관리                            │
│  - PDF 자동 변환 및 마크다운 편집                            │
│  - AI 기반 자연어 검색 및 자동 답변                          │
│  - SR과 위키 문서 연계                                       │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Goals & Success Metrics

### 3.1 비즈니스 목표

| 목표 | 측정 지표 | 목표치 |
|------|-----------|--------|
| 지식 축적 | 위키 문서 등록 수 | 100개 이상 (3개월) |
| 문서 활용 | PDF 변환 성공률 | 95% 이상 |
| AI 활용 | AI 검색 응답 시간 | 5초 이내 |
| 사용자 만족 | AI 답변 정확도 | 80% 이상 |

### 3.2 정성적 목표

- SR 처리 시 참고 문서를 쉽게 찾을 수 있음
- 신규 담당자 온보딩 시간 단축
- 반복 질문 감소 (AI 자동 답변)

---

## 4. Functional Requirements

### Epic 1: Core Wiki System (코어 위키 시스템)

#### FR-1.1: 마크다운 에디터 [✅ 완료]

| 항목 | 내용 |
|------|------|
| **Priority** | HIGH |
| **Story Points** | 5 |

**User Story**
> 사용자로서, 마크다운 형식으로 기술 문서를 작성하고 실시간으로 미리보기를 확인하고 싶습니다.

**Acceptance Criteria**
- [x] Toast UI Editor 컴포넌트 연동
- [x] 실시간 마크다운 미리보기 (Split View)
- [x] 코드 블록 Syntax Highlighting (Java, JavaScript, SQL, Bash 등)
- [x] 이미지 붙여넣기 및 업로드 지원
- [x] 테이블, 체크박스 등 확장 마크다운 지원

---

#### FR-1.2: 문서 버전 관리 [✅ 완료]

| 항목 | 내용 |
|------|------|
| **Priority** | MEDIUM |
| **Story Points** | 8 |

**User Story**
> 사용자로서, 위키 문서의 변경 이력을 추적하고 특정 버전으로 되돌릴 수 있어야 합니다.

**Acceptance Criteria**
- [x] 문서 수정 시 이전 버전 자동 저장
- [x] 버전 목록 조회 (수정일시, 수정자, 변경 요약)
- [x] 버전 간 Diff View (SR 이력과 동일한 UI 재사용)
- [x] 특정 버전으로 Rollback 기능
- [x] 버전 메타데이터 (commit message 형식)

---

#### FR-1.3: 파일 및 이미지 서버 [✅ 완료]

| 항목 | 내용 |
|------|------|
| **Priority** | HIGH |
| **Story Points** | 5 |

**User Story**
> 사용자로서, 위키 문서에 이미지와 파일을 첨부하고 로컬 서버에서 제공받고 싶습니다.

**Acceptance Criteria**
- [x] 파일 업로드 API (이미지: PNG, JPG, GIF / 문서: PDF, DOCX)
- [x] 로컬 스토리지에 파일 저장 (`backend/data/wiki-uploads/`)
- [x] 파일 다운로드 API (`/api/wiki/files/{fileId}`)
- [x] 이미지 URL 자동 생성 및 마크다운 삽입
- [x] 파일 크기 제한 (이미지: 5MB, 문서: 20MB)

---

#### FR-1.4: 계층형 카테고리 관리 [✅ 완료]

| 항목 | 내용 |
|------|------|
| **Priority** | MEDIUM |
| **Story Points** | 5 |

**User Story**
> 사용자로서, 문서를 폴더 구조로 분류하고 사이드바에서 트리 형태로 탐색하고 싶습니다.

**Acceptance Criteria**
- [x] 카테고리 CRUD (생성, 수정, 삭제)
- [x] 계층 구조 지원 (parent-child 관계)
- [x] 드래그 앤 드롭으로 문서 이동
- [x] 사이드바 트리 네비게이션 (접기/펼치기)
- [x] 카테고리별 문서 카운트 표시

---

### Epic 2: PDF Processing (PDF 지능형 변환)

#### FR-2.1: PDF to Markdown 변환 [✅ 완료]

| 항목 | 내용 |
|------|------|
| **Priority** | HIGH |
| **Story Points** | 8 |

**User Story**
> 사용자로서, PDF 파일을 업로드하면 자동으로 마크다운 문서로 변환되어 위키에 등록되기를 원합니다.

**Acceptance Criteria**
- [x] PDF 업로드 API
- [x] Apache Tika로 PDF 텍스트 추출
- [x] 텍스트를 마크다운 형식으로 변환
- [x] 제목, 단락, 리스트 자동 인식
- [x] 변환 상태 표시 (진행 중, 완료, 실패)
- [x] 변환 실패 시 에러 메시지 제공

---

#### FR-2.2: 멀티미디어 자산 추출 [✅ 완료]

| 항목 | 내용 |
|------|------|
| **Priority** | MEDIUM |
| **Story Points** | 5 |

**User Story**
> 사용자로서, PDF 내 이미지가 자동으로 추출되어 마크다운 문서에 삽입되기를 원합니다.

**Acceptance Criteria**
- [x] PDF에서 이미지 추출 (Apache PDFBox 사용)
- [x] 추출된 이미지를 로컬 스토리지에 저장
- [x] 마크다운에 이미지 URL 자동 삽입
- [x] 페이지별 이미지 위치 정보 기록
- [x] 원본 PDF 페이지 위치에 이미지 자동 배치
- [x] PNG 포맷으로 이미지 저장

---

#### FR-2.3: 목차 자동 생성 [✅ 완료]

| 항목 | 내용 |
|------|------|
| **Priority** | LOW |
| **Story Points** | 3 |

**User Story**
> 사용자로서, 마크다운 문서 저장 시 목차가 자동으로 생성되기를 원합니다.

**Acceptance Criteria**
- [x] MarkdownTocGenerator 유틸리티 구현
- [x] 마크다운 문서 저장 시 목차 자동 생성 옵션
- [x] GitHub/rehype-slug 호환 앵커 링크 생성
- [x] H2~H6 제목 자동 추출 및 계층 구조 유지
- [x] 프론트엔드 목차 체크박스 UI 추가

---

### Epic 3: AI Intelligence (AI 지능형 검색)

#### FR-3.1: RAG 기반 자연어 검색 [✅ 완료]

| 항목 | 내용 |
|------|------|
| **Priority** | HIGH |
| **Story Points** | 13 |

**User Story**
> 사용자로서, 자연어로 질문하면 위키 문서를 참고하여 AI가 답변을 생성해주기를 원합니다.

**Acceptance Criteria**
- [x] Ollama 서버 연동 (Local LLM) - `http://219.248.153.178:11434`
- [x] Spring AI를 통한 LLM 호출 - `gpt-oss:20b` 모델 사용
- [x] 위키 문서 임베딩 생성 (문서 저장 시 자동) - 비동기 처리
- [x] 커스텀 임베딩 테이블에 저장 - `wiki_document_embedding`
- [x] 사용자 질문 → 유사 문서 검색 (Top-K, 코사인 유사도)
- [x] 검색된 문서를 컨텍스트로 LLM에 전달
- [x] AI 답변 생성 및 반환
- [x] 임베딩 진행률 실시간 표시 (폴링 방식)

**RAG Architecture**
```
사용자 질문 → Embedding (snowflake-arctic-embed:110m) → 코사인 유사도 검색
                                    ↓
                         관련 문서 Top-K → Prompt Template → Ollama (gpt-oss:20b) → AI 답변
```

---

#### FR-3.2: 근거 문서 하이라이팅 [✅ 완료]

| 항목 | 내용 |
|------|------|
| **Priority** | MEDIUM |
| **Story Points** | 5 |

**User Story**
> 사용자로서, AI 답변을 볼 때 어떤 문서를 참고했는지 링크와 함께 확인하고 싶습니다.

**Acceptance Criteria**
- [x] AI 답변과 함께 참고 문서 목록 반환
- [x] 문서 제목, 링크, 관련도 점수 표시
- [x] 문서 내 관련 단락 미리보기 (snippet)
- [x] 클릭 시 해당 문서로 이동

---

#### FR-3.3: 자동 요약 기능 [✅ 완료]

| 항목 | 내용 |
|------|------|
| **Priority** | LOW |
| **Story Points** | 5 |

**User Story**
> 사용자로서, 긴 위키 문서를 열었을 때 상단에 AI가 생성한 3줄 요약을 보고 싶습니다.

**Acceptance Criteria**
- [x] 문서 조회 시 요약 자동 생성 (캐싱)
- [x] 3줄 이내의 간결한 요약
- [x] 문서 상단에 "AI 요약" 섹션 표시
- [x] 요약 생성 중 로딩 인디케이터

---

#### FR-3.4: SR/Survey 통합 AI 검색 [✅ 완료]

| 항목 | 내용 |
|------|------|
| **Priority** | HIGH |
| **Story Points** | 8 |

**User Story**
> 사용자로서, AI 검색 시 Wiki뿐만 아니라 SR과 OPEN API 현황조사 데이터도 함께 검색하여 통합된 답변을 받고 싶습니다.

**Acceptance Criteria**
- [x] SR 데이터 임베딩 (SR ID, 제목, 요청사항, 처리내용, 분류)
- [x] 현황조사 데이터 임베딩 (시스템명, 기관, 운영환경, 서버정보)
- [x] SR/Survey 생성/수정 시 자동 임베딩 트리거
- [x] 리소스 타입별 필터링 (Wiki, SR, Survey)
- [x] 검색 결과에서 리소스 타입 구분 표시 (아이콘/뱃지)
- [x] 참고 자료 클릭 시 해당 페이지로 이동
- [x] 일괄 임베딩 API 제공

**API Response Format**
```json
{
  "answer": "국민건강보험공단 시스템은 Spring Framework를 사용하며...",
  "sources": [
    {
      "resourceType": "SR",
      "resourceId": 123,
      "resourceIdentifier": "SR-2412-0001",
      "title": "API 연동 오류 문의",
      "status": "RESOLVED",
      "snippet": "...",
      "relevanceScore": 0.85
    },
    {
      "resourceType": "SURVEY",
      "resourceId": 45,
      "title": "국민건강보험공단 (복지부)",
      "categoryName": "복지부",
      "snippet": "...",
      "relevanceScore": 0.78
    }
  ],
  "processingTimeMs": 3200
}
```

---

#### FR-3.5: AI 검색 이력 관리 [✅ 완료]

| 항목 | 내용 |
|------|------|
| **Priority** | MEDIUM |
| **Story Points** | 5 |

**User Story**
> 사용자로서, 이전에 검색했던 질문들을 다시 확인하고 빠르게 재검색하고 싶습니다.

**Acceptance Criteria**
- [x] AI 검색 시 이력 자동 저장 (비동기 처리)
- [x] 최근 검색 이력 조회 (기본 5개)
- [x] 검색창 포커스 시 최근 검색 드롭다운 표시
- [x] 이력 클릭 시 해당 질문으로 자동 입력
- [x] 개별 이력 삭제 기능
- [x] 검색 이력 키워드 검색

**Database Schema**
```sql
CREATE TABLE ai_search_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    question VARCHAR(1000) NOT NULL,
    answer TEXT,
    source_count INT,
    resource_types VARCHAR(100),
    processing_time_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_search_history_user ON ai_search_history(user_id);
CREATE INDEX idx_search_history_created ON ai_search_history(created_at DESC);
```

---

## 5. Technical Requirements

### 5.1 기술 스택

#### 기존 시스템 (유지)

| 구분 | 기술 | 버전 |
|------|------|------|
| Frontend | React, TypeScript, Vite | 18.x |
| State Management | Redux Toolkit | - |
| Backend | Spring Boot, Spring Data JPA | 3.2.0 |
| Language | Java | 17 |
| Database | H2, CUBRID, MySQL, PostgreSQL | - |
| Authentication | JWT, Spring Security | - |

#### 신규 추가 (위키 기능)

| 구분 | 기술 | 용도 | 폐쇄망 지원 |
|------|------|------|-------------|
| AI Engine | Ollama + gpt-oss:20b | Local LLM 추론 | ✅ |
| AI Framework | Spring AI | LLM 연동 | ✅ |
| Vector Store | H2 JdbcVectorStore | 임베딩 벡터 저장 | ✅ |
| Document Parser | Apache Tika, PDFBox | PDF 파싱 | ✅ |
| Markdown Editor | Toast UI Editor | 위키 편집기 | ✅ |
| Markdown Renderer | react-markdown | 마크다운 렌더링 | ✅ |

### 5.2 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                      User Browser                            │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP/HTTPS
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                 Spring Boot Application                      │
│  ┌─────────────────────────────────────────────────────┐    │
│  │   REST API: /api/wiki/**                            │    │
│  │   - WikiController (문서 CRUD)                      │    │
│  │   - WikiFileController (파일 업로드/다운로드)       │    │
│  │   - WikiSearchController (AI 검색)                  │    │
│  └─────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │   Service Layer                                      │    │
│  │   - WikiService                                      │    │
│  │   - PdfConversionService                             │    │
│  │   - AiSearchService (Spring AI)                      │    │
│  └─────────────────────────────────────────────────────┘    │
└────────────────┬────────────────────┬───────────────────────┘
                 │ JDBC               │ HTTP
                 ▼                    ▼
┌────────────────────────────┐  ┌───────────────────────────┐
│     H2 Database (통합)      │  │      Ollama Server        │
│  - wiki_document           │  │  (219.248.153.178:11434)  │
│  - wiki_version            │  │  - gpt-oss:20b            │
│  - wiki_category           │  └───────────────────────────┘
│  - vector_store            │
└────────────────────────────┘
```

### 5.3 데이터베이스 스키마

```sql
-- 위키 문서
CREATE TABLE wiki_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    category_id BIGINT,
    sr_id BIGINT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 문서 버전
CREATE TABLE wiki_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    version INT NOT NULL,
    content TEXT,
    change_summary VARCHAR(200),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 카테고리
CREATE TABLE wiki_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 벡터 스토어 (AI 검색용)
CREATE TABLE vector_store (
    id VARCHAR(255) PRIMARY KEY,
    content TEXT NOT NULL,
    metadata TEXT,
    embedding ARRAY NOT NULL
);
```

### 5.4 환경 설정

```yaml
# application.yml
spring:
  ai:
    ollama:
      base-url: http://219.248.153.178:11434
      chat:
        model: gpt-oss:20b
        options:
          temperature: 0.7
          num-ctx: 4096
      embedding:
        model: gpt-oss:20b
    vectorstore:
      jdbc:
        initialize-schema: true
        table-name: vector_store
        distance-type: COSINE_SIMILARITY

wiki:
  upload:
    base-path: ./data/wiki-uploads
    max-file-size: 20971520  # 20MB
  ai:
    search:
      top-k: 3
      similarity-threshold: 0.7
```

---

## 6. Non-Functional Requirements

### 6.1 성능 요구사항

| 항목 | 요구사항 |
|------|----------|
| AI 검색 응답 시간 | 5초 이내 |
| 문서 로딩 시간 | 2초 이내 |
| PDF 변환 시간 | 30초 이내 (20MB 기준) |
| 동시 사용자 | 50명 이상 |

### 6.2 보안 요구사항

| 항목 | 요구사항 |
|------|----------|
| 인증 | JWT 기반 (기존 시스템 연동) |
| 파일 업로드 | 허용된 확장자만 허용, 파일 크기 제한 |
| API 접근 | 인증된 사용자만 접근 가능 |

### 6.3 운영 요구사항

| 항목 | 요구사항 |
|------|----------|
| 폐쇄망 지원 | 외부 네트워크 없이 동작 |
| 백업 | 일일 백업 지원 |
| 모니터링 | 서버 상태 및 AI 모델 상태 모니터링 |

---

## 7. Development Roadmap

### Phase 1: MVP - 기본 위키 기능 ✅ 완료

**목표**: 마크다운 기반 문서 작성 및 조회

| 기능 | 상태 |
|------|------|
| 마크다운 에디터 (Toast UI Editor) | ✅ 완료 |
| 문서 버전 관리 및 이력 | ✅ 완료 |
| 파일 및 이미지 업로드 | ✅ 완료 |
| 계층형 카테고리 관리 | ✅ 완료 |
| SR과 위키 문서 연계 | ✅ 완료 |

---

### Phase 2: PDF 변환 기능 ✅ 완료

**목표**: PDF 문서를 위키로 자동 변환

| 기능 | 상태 |
|------|------|
| PDF to Markdown 변환 | ✅ 완료 |
| PDF 내 이미지 추출 | ✅ 완료 |
| 목차 자동 생성 | ✅ 완료 |
| PDF 업로드 UI | ✅ 완료 |

---

### Phase 3: AI 검색 기능 ✅ 완료

**목표**: RAG 기반 자연어 검색 및 AI 답변

| 기능 | 상태 |
|------|------|
| Ollama 서버 연동 | ✅ 완료 |
| Spring AI 통합 | ✅ 완료 |
| RAG 기반 검색 | ✅ 완료 |
| 근거 문서 하이라이팅 | ✅ 완료 |
| 벡터 DB 임베딩 | ✅ 완료 |
| 임베딩 진행률 표시 | ✅ 완료 |
| 임베딩 상태 동기화 | ✅ 완료 |

**주요 구현 내용**
- Ollama 서버: `http://219.248.153.178:11434`
- Chat 모델: `gpt-oss:20b`
- Embedding 모델: `snowflake-arctic-embed:110m`
- 문서 청킹: 2000자 단위, 200자 오버랩
- 비동기 임베딩 생성 + 폴링 기반 진행률 표시
- 상세 개발 이력: [HISTORY_20251220_WIKI_PHASE3.md](HISTORY_20251220_WIKI_PHASE3.md)

---

### Phase 4: 고급 기능 및 최적화 ✅ 완료

**목표**: 시스템 안정화 및 추가 기능

| 기능 | 상태 |
|------|------|
| 자동 요약 기능 | ✅ 완료 |
| 검색 성능 최적화 (Caffeine 캐싱) | ✅ 완료 |
| 사용자 권한 관리 (WIKI_EDITOR) | ✅ 완료 |
| 알림 기능 | ✅ 완료 |
| 백업/복원 스크립트 | ✅ 완료 |

---

### Phase 5: SR/Survey 통합 AI 검색 ✅ 완료

**목표**: SR 및 OPEN API 현황조사 데이터를 AI 검색에 통합

| 기능 | 상태 |
|------|------|
| 통합 콘텐츠 임베딩 시스템 | ✅ 완료 |
| SR/Survey 자동 임베딩 | ✅ 완료 |
| 통합 AI 검색 (Wiki + SR + Survey) | ✅ 완료 |
| 리소스 타입별 필터링 | ✅ 완료 |
| 일괄 임베딩 API | ✅ 완료 |
| 프론트엔드 통합 검색 UI | ✅ 완료 |
| AI 검색 이력 관리 | ✅ 완료 |

**주요 구현 내용**
- ContentEmbedding 엔티티: Wiki, SR, Survey 통합 임베딩 테이블
- 자동 임베딩: SR/Survey 생성/수정 시 비동기 임베딩 트리거
- 통합 검색: 리소스 타입별 필터링 및 코사인 유사도 검색
- UI: 리소스 타입 아이콘(📄/📋/📊), 필터 체크박스, 상태 뱃지
- 검색 이력: 사용자별 검색 이력 자동 저장, 최근 검색 드롭다운 UI

---

## 8. Risk Assessment

### 8.1 기술적 위험

| 위험 | 영향 | 확률 | 대응 방안 |
|------|------|------|-----------|
| Ollama 성능 부족 | HIGH | MEDIUM | 경량 모델 사용, 타임아웃 설정 |
| PDF 변환 품질 저하 | MEDIUM | HIGH | Pandoc 하이브리드 방식, 수동 편집 지원 |
| H2 Vector Store 성능 저하 | MEDIUM | MEDIUM | 임베딩 차원 축소, 아카이빙 정책 |

### 8.2 운영적 위험

| 위험 | 영향 | 확률 | 대응 방안 |
|------|------|------|-----------|
| 사용자 교육 부족 | MEDIUM | MEDIUM | 사용 가이드 문서 제공, 온보딩 세션 |
| 데이터 유실 | HIGH | LOW | 정기 백업, 버전 관리 |

---

## 9. Dependencies

### 9.1 외부 의존성

| 의존성 | 용도 | 상태 |
|--------|------|------|
| Ollama Server (219.248.153.178:11434) | Local LLM | ✅ 설치 완료 |
| gpt-oss:20b 모델 | Chat/Embedding | ✅ 설치 완료 |

### 9.2 내부 의존성

| 의존성 | 용도 |
|--------|------|
| 기존 SR 관리 시스템 | 사용자 인증, SR 연계 |
| H2 Database | 데이터 저장, Vector Store |

---

## 10. Appendix

### A. API Endpoints

```
# Wiki Document
POST   /api/wiki/documents           # 문서 생성
GET    /api/wiki/documents           # 문서 목록 조회
GET    /api/wiki/documents/{id}      # 문서 상세 조회
PUT    /api/wiki/documents/{id}      # 문서 수정
DELETE /api/wiki/documents/{id}      # 문서 삭제

# Wiki File
POST   /api/wiki/files/upload        # 파일 업로드
GET    /api/wiki/files/{fileId}      # 파일 다운로드

# Wiki Category
POST   /api/wiki/categories          # 카테고리 생성
GET    /api/wiki/categories          # 카테고리 목록 조회
PUT    /api/wiki/categories/{id}     # 카테고리 수정
DELETE /api/wiki/categories/{id}     # 카테고리 삭제

# AI Search
POST   /api/wiki/search/ai           # AI 검색 (통합)
GET    /api/wiki/search/embedding/status  # 임베딩 상태 조회

# 통합 임베딩 (SR/Survey)
POST   /api/wiki/search/embeddings/sr/all       # 전체 SR 임베딩
POST   /api/wiki/search/embeddings/survey/all   # 전체 현황조사 임베딩
POST   /api/wiki/search/embeddings/sr/{srId}    # 개별 SR 임베딩
POST   /api/wiki/search/embeddings/survey/{id}  # 개별 현황조사 임베딩
GET    /api/wiki/search/embeddings/stats        # 임베딩 통계

# 검색 이력
GET    /api/wiki/search/history/recent          # 최근 검색 이력 조회
GET    /api/wiki/search/history                 # 검색 이력 페이징 조회
GET    /api/wiki/search/history/search          # 검색 이력 키워드 검색
DELETE /api/wiki/search/history/{historyId}     # 검색 이력 삭제
```

### B. 관련 문서

- [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md) - SR 관리 시스템 현황
- [API.md](API.md) - REST API 명세
- [DATABASE.md](DATABASE.md) - 데이터베이스 설계
- [PB_AI-Powered_Wiki.md](PB_AI-Powered_Wiki.md) - 상세 기술 명세

---

**문의사항이나 추가 요구사항은 프로젝트 관리자에게 연락하세요.**
