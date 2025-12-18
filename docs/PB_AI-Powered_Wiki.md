# PB: AI-Powered Knowledge Wiki for SR Management System

SR 관리 시스템에 AI 기반 지능형 위키 기능을 추가하여, 폐쇄망 환경에서 지식의 체계적 축적과 AI를 활용한 지능형 검색 및 분석을 제공합니다.

---

## 🎯 0. 개발 목표 및 배경

### 배경
* SR 관리 과정에서 발생하는 다양한 기술 문서, 가이드, FAQ 등을 체계적으로 관리할 필요성 증가
* PDF 형태의 기술 문서를 효율적으로 검색하고 활용할 방법 부족
* 담당자 간 지식 공유 및 인수인계 시 정보 손실 발생
* 반복적인 질문에 대한 답변을 AI로 자동화하여 업무 효율 향상

### 목표
* SR과 연계된 기술 문서 위키 시스템 구축
* PDF 문서 자동 변환 및 지능형 검색 기능 제공
* 폐쇄망 환경에서 Local LLM(Ollama) 기반 AI 활용
* 기존 SR 관리 시스템과의 완벽한 통합

---

## 🛠 1. 기술 스택 (Tech Stack)

### 1.1 기존 시스템 (유지)
| 구분 | 상세 기술 | 버전 |
| --- | --- | --- |
| **Frontend** | React 18, TypeScript, Vite | 18.x |
| **State Management** | Redux Toolkit | - |
| **Backend** | Spring Boot, Spring Data JPA | 3.2.0 |
| **Language** | Java | 17 |
| **Database** | H2, CUBRID, MySQL, PostgreSQL | - |
| **Authentication** | JWT, Spring Security | - |

### 1.2 신규 추가 (위키 기능)
| 구분 | 상세 기술 | 용도 | 폐쇄망 지원 |
| --- | --- | --- | --- |
| **AI Engine** | Ollama + Llama 3.2 | Local LLM 추론 | ✅ |
| **AI Framework** | Spring AI | LLM 연동 | ✅ |
| **Vector Store** | Spring AI JdbcVectorStore (H2 기반) | 임베딩 벡터 저장 | ✅ |
| **Full-text Search** | H2 Full-text Index | 키워드 검색 (보조) | ✅ |
| **Document Parser** | Apache Tika | PDF/DOCX 파싱 | ✅ |
| **Markdown Editor** | Toast UI Editor | 위키 편집기 | ✅ |
| **Markdown Renderer** | react-markdown | 마크다운 렌더링 | ✅ |
| **Syntax Highlighting** | highlight.js | 코드 블록 강조 | ✅ |
| **PDF Converter** | Pandoc (Optional) | PDF→Markdown | ✅ |

**Note**: 별도 Vector DB(Chroma/Qdrant) 대신 **H2의 JdbcVectorStore**를 사용하여 벡터 저장 및 유사도 검색을 구현합니다. 이를 통해 외부 의존성 없이 통합 환경에서 RAG를 구현할 수 있습니다.

---

## 📋 2. 기능 요구사항 (Functional Requirements)

### Epic 1. 코어 위키 시스템 (Core Wiki)

#### W-1: 마크다운 에디터 구현
**Priority**: HIGH | **Estimate**: 5 Story Points

**User Story**
> 사용자로서, 마크다운 형식으로 기술 문서를 작성하고 실시간으로 미리보기를 확인하고 싶습니다.

**Acceptance Criteria**
* [ ] Toast UI Editor 컴포넌트 연동
* [ ] 실시간 마크다운 미리보기 (Split View)
* [ ] 코드 블록 Syntax Highlighting (Java, JavaScript, SQL, Bash 등)
* [ ] 이미지 붙여넣기 및 업로드 지원
* [ ] 테이블, 체크박스 등 확장 마크다운 지원

**Technical Notes**
```typescript
// frontend/src/components/wiki/WikiEditor.tsx
import { Editor } from '@toast-ui/react-editor';
import '@toast-ui/editor/dist/toastui-editor.css';
```

---

#### W-2: 문서 버전 관리 및 이력
**Priority**: MEDIUM | **Estimate**: 8 Story Points

**User Story**
> 사용자로서, 위키 문서의 변경 이력을 추적하고 특정 버전으로 되돌릴 수 있어야 합니다.

**Acceptance Criteria**
* [ ] 문서 수정 시 이전 버전 자동 저장
* [ ] 버전 목록 조회 (수정일시, 수정자, 변경 요약)
* [ ] 버전 간 Diff View (SR 이력과 동일한 UI 재사용)
* [ ] 특정 버전으로 Rollback 기능
* [ ] 버전 메타데이터 (commit message 형식)

**Database Schema**
```sql
CREATE TABLE wiki_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    category_id BIGINT,
    sr_id BIGINT,                         -- SR 연계 (Optional)
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sr_id) REFERENCES sr(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE wiki_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    version INT NOT NULL,
    content TEXT,
    change_summary VARCHAR(200),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES wiki_document(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);
```

---

#### W-3: 파일 및 이미지 서버
**Priority**: HIGH | **Estimate**: 5 Story Points

**User Story**
> 사용자로서, 위키 문서에 이미지와 파일을 첨부하고 로컬 서버에서 제공받고 싶습니다.

**Acceptance Criteria**
* [ ] 파일 업로드 API (이미지: PNG, JPG, GIF / 문서: PDF, DOCX)
* [ ] 로컬 스토리지에 파일 저장 (`backend/data/wiki-uploads/`)
* [ ] 파일 다운로드 API (`/api/wiki/files/{fileId}`)
* [ ] 이미지 URL 자동 생성 및 마크다운 삽입
* [ ] 파일 크기 제한 (이미지: 5MB, 문서: 20MB)
* [ ] 파일 메타데이터 DB 저장

**API Endpoints**
```java
// backend/src/main/java/com/srmanagement/controller/WikiFileController.java
@PostMapping("/api/wiki/files/upload")
ResponseEntity<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file)

@GetMapping("/api/wiki/files/{fileId}")
ResponseEntity<Resource> downloadFile(@PathVariable Long fileId)
```

---

#### W-4: 계층형 카테고리 관리
**Priority**: MEDIUM | **Estimate**: 5 Story Points

**User Story**
> 사용자로서, 문서를 폴더 구조로 분류하고 사이드바에서 트리 형태로 탐색하고 싶습니다.

**Acceptance Criteria**
* [ ] 카테고리 CRUD (생성, 수정, 삭제)
* [ ] 계층 구조 지원 (parent-child 관계)
* [ ] 드래그 앤 드롭으로 문서 이동
* [ ] 사이드바 트리 네비게이션 (접기/펼치기)
* [ ] 카테고리별 문서 카운트 표시

**Database Schema**
```sql
CREATE TABLE wiki_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES wiki_category(id)
);
```

---

### Epic 2. PDF 지능형 변환 (PDF Processing)

#### D-1: PDF to Markdown 자동 변환
**Priority**: HIGH | **Estimate**: 8 Story Points

**User Story**
> 사용자로서, PDF 파일을 업로드하면 자동으로 마크다운 문서로 변환되어 위키에 등록되기를 원합니다.

**Acceptance Criteria**
* [ ] PDF 업로드 API
* [ ] Apache Tika로 PDF 텍스트 추출
* [ ] 텍스트를 마크다운 형식으로 변환
* [ ] 제목, 단락, 리스트 자동 인식
* [ ] 변환 상태 표시 (진행 중, 완료, 실패)
* [ ] 변환 실패 시 에러 메시지 제공

**Dependencies**
```xml
<!-- backend/pom.xml -->
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-core</artifactId>
    <version>2.9.1</version>
</dependency>
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-parsers-standard-package</artifactId>
    <version>2.9.1</version>
</dependency>
```

---

#### D-2: 멀티미디어 자산 추출
**Priority**: MEDIUM | **Estimate**: 5 Story Points

**User Story**
> 사용자로서, PDF 내 이미지가 자동으로 추출되어 마크다운 문서에 삽입되기를 원합니다.

**Acceptance Criteria**
* [ ] PDF에서 이미지 추출 (Apache PDFBox 사용)
* [ ] 추출된 이미지를 로컬 스토리지에 저장
* [ ] 마크다운에 이미지 URL 자동 삽입
* [ ] 이미지 포맷 변환 (TIFF → PNG)
* [ ] 이미지 크기 최적화 (리사이징)

---

#### D-3: AI 기반 구조 보정 (Optional)
**Priority**: LOW | **Estimate**: 13 Story Points

**User Story**
> 사용자로서, 복잡한 표나 수식이 포함된 PDF도 정교하게 마크다운으로 변환되기를 원합니다.

**Acceptance Criteria**
* [ ] Pandoc 엔진 통합 (외부 프로세스 실행)
* [ ] 표(Table) 구조 인식 및 마크다운 변환
* [ ] 수식(LaTeX) 인식 및 변환
* [ ] Llama Vision 모델로 이미지 내 표 추출 (고급 기능)

**Note**: 이 기능은 선택적이며, Pandoc 설치가 필요합니다.

---

### Epic 3. AI 지능형 검색 및 분석 (AI Intelligence)

#### A-1: RAG 기반 자연어 검색
**Priority**: HIGH | **Estimate**: 13 Story Points

**User Story**
> 사용자로서, 자연어로 질문하면 위키 문서를 참고하여 AI가 답변을 생성해주기를 원합니다.

**Acceptance Criteria**
* [ ] Ollama 서버 연동 (Local LLM)
* [ ] Spring AI를 통한 LLM 호출
* [ ] 위키 문서 임베딩 생성 (문서 저장 시 자동)
* [ ] H2 JdbcVectorStore에 임베딩 저장
* [ ] 사용자 질문 → 유사 문서 검색 (Top-K, 코사인 유사도)
* [ ] 검색된 문서를 컨텍스트로 LLM에 전달
* [ ] AI 답변 생성 및 반환
* [ ] (보조) H2 Full-text Index를 이용한 키워드 검색

**Architecture**
```
사용자 질문 → Embedding → H2 Vector Search (Cosine Similarity) → 관련 문서 Top-3
→ Prompt Template → Ollama (Llama 3.2) → AI 답변
```

**Dependencies**
```xml
<!-- Spring AI -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
    <version>1.0.0-M1</version>
</dependency>
<!-- JDBC Vector Store (H2 지원) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-jdbc-store</artifactId>
    <version>1.0.0-M1</version>
</dependency>
```

**Configuration**
```yaml
# backend/src/main/resources/application.yml
spring:
  ai:
    ollama:
      base-url: http://219.248.153.178:11434
      chat:
        model: gpt-oss:20b
        options:
          temperature: 0.7
          top-p: 0.9
          stream: false
    vectorstore:
      jdbc:
        # H2 기존 DataSource 재사용
        initialize-schema: true
        table-name: vector_store
        distance-type: COSINE_SIMILARITY
```

**Vector Store 테이블 스키마 (자동 생성)**
```sql
CREATE TABLE vector_store (
    id VARCHAR(255) PRIMARY KEY,
    content TEXT,
    metadata TEXT,
    embedding ARRAY  -- H2에서 ARRAY 타입으로 벡터 저장
);

-- 성능 최적화를 위한 인덱스
CREATE INDEX idx_vector_store_embedding ON vector_store(embedding);
```

**Full-text Search 보조 기능 (Optional)**
```sql
-- H2 Full-text Index 생성
CREATE ALIAS IF NOT EXISTS FTL_INIT FOR "org.h2.fulltext.FullText.init";
CALL FTL_INIT();

-- 위키 문서 테이블에 Full-text Index 추가
CALL FTL_CREATE_INDEX('PUBLIC', 'WIKI_DOCUMENT', 'TITLE,CONTENT');
```

---

#### A-2: 근거 문서 하이라이팅
**Priority**: MEDIUM | **Estimate**: 5 Story Points

**User Story**
> 사용자로서, AI 답변을 볼 때 어떤 문서를 참고했는지 링크와 함께 확인하고 싶습니다.

**Acceptance Criteria**
* [ ] AI 답변과 함께 참고 문서 목록 반환
* [ ] 문서 제목, 링크, 관련도 점수 표시
* [ ] 문서 내 관련 단락 미리보기 (snippet)
* [ ] 클릭 시 해당 문서로 이동

**Response Format**
```json
{
  "answer": "SR 생성 API는 POST /api/sr 엔드포인트를 사용합니다...",
  "sources": [
    {
      "documentId": 123,
      "title": "SR API 가이드",
      "snippet": "...POST /api/sr 엔드포인트는...",
      "relevanceScore": 0.92
    }
  ]
}
```

---

#### A-3: 자동 요약 기능
**Priority**: LOW | **Estimate**: 5 Story Points

**User Story**
> 사용자로서, 긴 위키 문서를 열었을 때 상단에 AI가 생성한 3줄 요약을 보고 싶습니다.

**Acceptance Criteria**
* [ ] 문서 조회 시 요약 자동 생성 (캐싱)
* [ ] 3줄 이내의 간결한 요약
* [ ] 문서 상단에 "AI 요약" 섹션 표시
* [ ] 요약 생성 중 로딩 인디케이터
* [ ] 요약 갱신 버튼 (수동 갱신)

---

#### A-4: 이미지/OCR 검색 (Future)
**Priority**: LOW | **Estimate**: 13 Story Points

**User Story**
> 사용자로서, 이미지 내 텍스트와 다이어그램도 검색 대상에 포함되기를 원합니다.

**Acceptance Criteria**
* [ ] Tesseract OCR로 이미지 내 텍스트 추출
* [ ] 추출된 텍스트를 검색 인덱스에 추가
* [ ] Llama Vision으로 이미지 캡셔닝
* [ ] 이미지 설명을 메타데이터로 저장

**Note**: 이 기능은 Phase 4 이후 고려됩니다.

---

## 🏗 3. 시스템 아키텍처 (Architecture)

### 3.1 전체 구조

```
┌─────────────────────────────────────────────────────┐
│                    User Browser                      │
└──────────────────────┬──────────────────────────────┘
                       │
                       │ HTTP/HTTPS
                       ▼
┌─────────────────────────────────────────────────────┐
│              Spring Boot Application                 │
│  ┌───────────────────────────────────────────────┐  │
│  │   REST API: /api/wiki/**                      │  │
│  │   - WikiController                            │  │
│  │   - WikiFileController                        │  │
│  │   - WikiSearchController (AI 검색)            │  │
│  └───────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────┐  │
│  │   Service Layer                               │  │
│  │   - WikiService                               │  │
│  │   - PdfConversionService                      │  │
│  │   - AiSearchService (Spring AI)               │  │
│  │   - VectorStoreService (JdbcVectorStore)      │  │
│  └───────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────┐  │
│  │   Repository + Vector Store                   │  │
│  │   - WikiDocumentRepository (JPA)              │  │
│  │   - JdbcVectorStore (Spring AI)               │  │
│  └───────────────────────────────────────────────┘  │
└──────────────┬──────────────────┬───────────────────┘
               │                  │
               │ JDBC             │ HTTP
               ▼                  ▼
       ┌─────────────────────────────────────┐
       │      H2 Database (통합)              │
       │  ┌─────────────────────────────┐   │
       │  │ 일반 테이블                  │   │
       │  │ - wiki_document             │   │
       │  │ - wiki_version              │   │
       │  │ - wiki_category             │   │
       │  │ - sr (기존 테이블)           │   │
       │  └─────────────────────────────┘   │
       │  ┌─────────────────────────────┐   │
       │  │ Vector Store 테이블          │   │
       │  │ - vector_store              │   │
       │  │   (embedding ARRAY)         │   │
       │  └─────────────────────────────┘   │
       │  ┌─────────────────────────────┐   │
       │  │ Full-text Index (보조)       │   │
       │  │ - FTL_INDEX                 │   │
       │  └─────────────────────────────┘   │
       └─────────────────────────────────────┘
                       ▲
                       │ HTTP
                       │
               ┌───────────────┐
               │ Ollama Server │
               │ (localhost)   │
               └───────────────┘
```

### 3.2 RAG 처리 흐름

```
[사용자 질문] "SR 생성 API는 어떻게 사용하나요?"
        ↓
[1] Embedding 생성 (Spring AI + Ollama Embeddings)
   → 질문 텍스트를 벡터로 변환 (1536차원)
        ↓
[2] Vector Search (H2 JdbcVectorStore)
   → SELECT * FROM vector_store
     ORDER BY COSINE_SIMILARITY(embedding, ?)
     LIMIT 3
   → 유사 문서 Top-3 검색
   → 관련도 점수 계산 (0~1)
        ↓
[2-1] (Optional) Full-text Search 병행
   → SELECT * FROM FTL_SEARCH_DATA('SR API', 0, 0)
   → 키워드 매칭 결과 결합
        ↓
[3] Context 생성
   → 검색된 문서 내용 결합
   → 메타데이터 추가 (제목, 카테고리)
        ↓
[4] Prompt Template
   → "다음 문서를 참고하여 답변하세요: [문서 내용]"
   → 사용자 질문 추가
        ↓
[5] LLM 추론 (Ollama gpt-oss:20b)
   → HTTP POST http://219.248.153.178:11434/api/generate
   → 답변 생성 (stream: false)
        ↓
[6] 답변 생성 + 참고 문서 링크
   → 응답 JSON: { answer, sources }
        ↓
[Frontend] AI 답변 표시 (마크다운 렌더링)
```

**성능 최적화 포인트**
1. **Embedding 캐싱**: 동일한 질문은 캐시에서 임베딩 재사용
2. **벡터 검색 인덱스**: H2에서 ARRAY 타입에 대한 인덱스 활용
3. **병렬 검색**: Vector Search + Full-text Search 병렬 실행 후 결과 병합
4. **답변 캐싱**: 자주 묻는 질문(FAQ)에 대한 답변 캐시

---

## 🔧 4. 기술 구현 세부사항

### 4.1 Backend 패키지 구조 (추가)

```
backend/src/main/java/com/srmanagement/
├── wiki/                    # 신규 패키지
│   ├── controller/
│   │   ├── WikiController
│   │   ├── WikiFileController
│   │   └── WikiSearchController
│   ├── service/
│   │   ├── WikiService
│   │   ├── PdfConversionService
│   │   └── AiSearchService
│   ├── repository/
│   │   ├── WikiDocumentRepository
│   │   ├── WikiVersionRepository
│   │   ├── WikiCategoryRepository
│   │   └── WikiFileRepository
│   ├── entity/
│   │   ├── WikiDocument
│   │   ├── WikiVersion
│   │   ├── WikiCategory
│   │   └── WikiFile
│   └── dto/
│       ├── WikiDocumentRequest
│       ├── WikiDocumentResponse
│       ├── AiSearchRequest
│       └── AiSearchResponse
└── config/
    ├── OllamaConfig          # Ollama 연동 설정
    └── VectorStoreConfig     # H2 JdbcVectorStore 설정
```

### 4.2 Frontend 컴포넌트 구조 (추가)

```
frontend/src/
├── components/
│   └── wiki/                # 신규 디렉토리
│       ├── WikiEditor.tsx
│       ├── WikiViewer.tsx
│       ├── WikiSidebar.tsx
│       ├── WikiCategoryTree.tsx
│       ├── WikiVersionHistory.tsx
│       ├── PdfUploadModal.tsx
│       └── AiSearchBox.tsx
├── pages/
│   ├── WikiPage.tsx
│   └── WikiSearchPage.tsx
└── services/
    ├── wikiService.ts
    └── aiSearchService.ts
```

---

## ⚙️ 5. 배포 및 환경 설정

### 5.1 Ollama 서버 연동 (폐쇄망 환경)

**✅ 현재 상태: Ollama 서버 이미 설치 완료**

**서버 정보**
- **URL**: `http://219.248.153.178:11434`
- **모델**: `gpt-oss:20b` (이미 설치됨)
- **포트**: 11434

**API 호출 테스트**
```bash
# Chat API 테스트
curl http://219.248.153.178:11434/api/generate -d '{
  "model": "gpt-oss:20b",
  "prompt": "하늘은 왜 파란색이야?",
  "stream": false
}'

# 사용 가능한 모델 목록 확인
curl http://219.248.153.178:11434/api/tags

# 모델 정보 확인
curl http://219.248.153.178:11434/api/show -d '{
  "name": "gpt-oss:20b"
}'
```

**추가 모델 설치 (필요 시)**

만약 Embedding 전용 모델이나 다른 모델이 필요한 경우:

```bash
# 외부망에서 모델 다운로드
ollama pull nomic-embed-text    # Embedding 모델
ollama pull llama3.2            # 다른 LLM 모델

# 모델 파일 압축
tar -czf ollama-models.tar.gz ~/.ollama/models/

# 폐쇄망 서버로 복사 후 압축 해제
scp ollama-models.tar.gz user@219.248.153.178:/path/to/
ssh user@219.248.153.178
cd /path/to/
tar -xzf ollama-models.tar.gz -C ~/.ollama/
```

**주의사항**
- 현재 `gpt-oss:20b` 모델이 Chat과 Embedding을 모두 지원하는지 확인 필요
- Embedding 전용 모델이 필요하면 `nomic-embed-text` 또는 `all-minilm` 추가 설치 권장

### 5.2 H2 Vector Store 설정

**별도 설치 불필요!** 기존 H2 데이터베이스를 그대로 사용합니다.

**Vector Store 테이블 자동 생성**
```yaml
spring:
  ai:
    vectorstore:
      jdbc:
        initialize-schema: true  # 자동으로 vector_store 테이블 생성
```

**수동 생성 (필요 시)**
```sql
-- backend/src/main/resources/db/migration/create_vector_store.sql
CREATE TABLE IF NOT EXISTS vector_store (
    id VARCHAR(255) PRIMARY KEY,
    content TEXT NOT NULL,
    metadata TEXT,
    embedding ARRAY NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_vector_store_id ON vector_store(id);
```

### 5.3 Application 설정

**기본 설정 (application-wiki.yml)**
```yaml
# backend/src/main/resources/application-wiki.yml
spring:
  ai:
    # Ollama 연동 설정 (폐쇄망 서버)
    ollama:
      base-url: http://219.248.153.178:11434
      chat:
        model: gpt-oss:20b    # 현재 설치된 모델
        options:
          temperature: 0.7    # 창의성 (0~1, 낮을수록 보수적)
          top-p: 0.9          # 확률 임계값
          num-ctx: 4096       # 컨텍스트 윈도우 크기
          stream: false       # 스트리밍 비활성화 (안정성)
      embedding:
        model: gpt-oss:20b    # Chat 모델로 Embedding도 생성 (또는 전용 모델)
        options:
          dimensions: 768     # 임베딩 차원 (모델에 따라 조정)

    # JDBC Vector Store 설정
    vectorstore:
      jdbc:
        initialize-schema: true
        table-name: vector_store
        distance-type: COSINE_SIMILARITY
        # schema-name: PUBLIC  # H2의 기본 스키마

# 위키 기능 설정
wiki:
  upload:
    base-path: ./data/wiki-uploads
    max-file-size: 20971520        # 20MB
    allowed-extensions:
      - pdf
      - docx
      - png
      - jpg
      - gif
  pdf:
    conversion:
      enabled: true
      use-pandoc: false              # Pandoc 사용 여부 (optional)
  fulltext:
    enabled: true                    # H2 Full-text Index 사용
  ai:
    search:
      top-k: 3                       # 상위 K개 문서 검색
      similarity-threshold: 0.7       # 유사도 임계값 (0~1)
      cache-ttl: 3600                # 답변 캐시 TTL (초)
```

**프로덕션 환경 (application-prod.yml)**
```yaml
spring:
  ai:
    ollama:
      base-url: ${OLLAMA_URL:http://219.248.153.178:11434}
      chat:
        model: ${OLLAMA_CHAT_MODEL:gpt-oss:20b}
      embedding:
        model: ${OLLAMA_EMBEDDING_MODEL:gpt-oss:20b}
    vectorstore:
      jdbc:
        initialize-schema: false  # 프로덕션에서는 수동 관리
```

**환경 변수 설정 (Optional)**
```bash
# Ollama 서버 URL 커스터마이징
export OLLAMA_URL=http://219.248.153.178:11434

# 모델 변경 (다른 모델 설치 시)
export OLLAMA_CHAT_MODEL=llama3.2
export OLLAMA_EMBEDDING_MODEL=nomic-embed-text
```

---

## 🚀 6. 단계별 개발 로드맵 (Roadmap)

### Phase 1: MVP - 기본 위키 기능 (4주) ✅ 완료
**목표**: 마크다운 기반 문서 작성 및 조회

* [x] W-1: 마크다운 에디터 구현 (Toast UI Editor)
* [x] W-2: 문서 버전 관리 및 이력
* [x] W-3: 파일 및 이미지 업로드
* [x] W-4: 계층형 카테고리 관리
* [x] 위키 문서 CRUD API
* [x] 위키 페이지 UI 구현
* [x] SR과 위키 문서 연계 (Many-to-Many 관계)
* [x] 슬라이드 패널 네비게이션 (SR ↔ Wiki 상호 이동)

**Deliverables** ✅
- ✅ 사용자가 마크다운으로 기술 문서를 작성할 수 있음
- ✅ 카테고리별로 문서를 분류하고 조회할 수 있음
- ✅ 이미지를 업로드하고 문서에 삽입할 수 있음
- ✅ 문서 버전 관리 및 롤백 기능
- ✅ SR과 Wiki 문서 다대다 연계 및 상호 네비게이션
- ✅ 슬라이드 패널을 통한 seamless한 문서 탐색

---

### Phase 2: PDF 변환 기능 (3주)
**목표**: PDF 문서를 위키로 자동 변환

* [ ] D-1: PDF to Markdown 변환 (Apache Tika)
* [ ] D-2: PDF 내 이미지 추출 및 저장
* [ ] PDF 업로드 UI 및 진행 상태 표시
* [ ] 변환 결과 미리보기
* [ ] W-2: 문서 버전 관리 및 이력

**Deliverables**
- PDF 파일을 업로드하면 자동으로 마크다운 변환됨
- 변환된 문서를 위키에 저장하고 편집 가능
- 문서 수정 이력을 추적하고 이전 버전으로 복구 가능

---

### Phase 3: AI 검색 기능 (5주)
**목표**: RAG 기반 자연어 검색 및 AI 답변

* [ ] Ollama 서버 연동 및 테스트
* [ ] Spring AI 통합
* [ ] A-1: RAG 기반 자연어 검색
* [ ] A-2: 근거 문서 하이라이팅
* [ ] Vector DB 임베딩 자동 생성
* [ ] AI 검색 UI (챗봇 스타일)

**Deliverables**
- 사용자가 자연어로 질문하면 AI가 답변 생성
- 답변 생성 시 참고한 문서 링크 제공
- 위키 문서가 자동으로 벡터 DB에 임베딩됨

---

### Phase 4: 고급 기능 및 최적화 (4주)
**목표**: 시스템 안정화 및 추가 기능

* [ ] A-3: 자동 요약 기능
* [ ] D-3: Pandoc 연동 (Optional)
* [ ] 검색 성능 최적화 (캐싱)
* [ ] 사용자 권한 관리 (위키 편집 권한)
* [ ] 알림 기능 (문서 업데이트 시)
* [ ] 백업 및 마이그레이션 스크립트

**Deliverables**
- 긴 문서에 대한 AI 요약 제공
- 검색 응답 속도 개선 (캐싱)
- 위키 시스템 안정화 및 프로덕션 배포 준비

---

## 📊 7. 성공 지표 (Success Metrics)

### 정량적 지표
* 위키 문서 등록 수: **100개 이상** (첫 3개월)
* PDF 변환 성공률: **95% 이상**
* AI 검색 응답 시간: **5초 이내**
* AI 답변 정확도: **80% 이상** (사용자 피드백 기반)
* 사용자 만족도: **4점 이상** (5점 만점)

### 정성적 지표
* SR 처리 시 참고 문서를 쉽게 찾을 수 있음
* 신규 담당자 온보딩 시간 단축
* 반복 질문 감소 (AI 자동 답변)

---

## ⚠️ 8. 위험 요소 및 대응 방안

### 위험 1: Ollama 성능 (폐쇄망 서버 스펙 부족)
**영향**: HIGH | **확률**: MEDIUM

**대응 방안**
* CPU 전용 모드로 실행 (GPU 없어도 작동)
* 경량 모델 사용 (Llama 3.2 3B 버전)
* 응답 타임아웃 설정 (10초)

### 위험 2: PDF 변환 품질 저하
**영향**: MEDIUM | **확률**: HIGH

**대응 방안**
* Apache Tika + Pandoc 하이브리드 방식
* 변환 실패 시 원본 PDF 첨부 유지
* 사용자가 수동으로 편집 가능

### 위험 3: H2 Vector Store 성능 저하 (문서 증가 시)
**영향**: MEDIUM | **확률**: MEDIUM

**대응 방안**
* 임베딩 차원 축소 (1536 → 768 또는 384)
* 벡터 검색 전 Full-text 필터링으로 후보 축소
* 오래된 문서 임베딩 아카이빙 (별도 테이블로 이동)
* 필요 시 PostgreSQL의 pgvector 확장 또는 전용 Vector DB로 마이그레이션

---

## 📚 9. 참고 자료

### 공식 문서
* **Spring AI**: https://docs.spring.io/spring-ai/reference/
* **Ollama**: https://ollama.com/docs
* **Apache Tika**: https://tika.apache.org/
* **Toast UI Editor**: https://ui.toast.com/tui-editor
* **Chroma**: https://docs.trychroma.com/

### 기술 블로그
* RAG with Spring AI: [spring.io/blog/2024/04/10/rag-spring-ai](https://spring.io/blog/2024/04/10/rag-with-spring-ai)
* Local LLM 활용 사례

---

## 🎯 10. 다음 단계

### 즉시 착수 가능한 작업
1. **Spike**: Spring AI + Ollama 연동 PoC (1일)
   - 폐쇄망 Ollama 서버 (`http://219.248.153.178:11434`) 연결 테스트
   - `gpt-oss:20b` 모델로 Chat API 호출 검증
   - Embedding 생성 가능 여부 확인
2. **Spike**: Apache Tika PDF 변환 테스트 (1일)
3. **Story**: Wiki 엔티티 설계 및 JPA 구현 (3일)
4. **Story**: Toast UI Editor 컴포넌트 연동 (2일)

### 기술 검토 필요
* ✅ Vector DB: H2 JdbcVectorStore 사용 (결정 완료)
* ✅ Ollama 서버: 폐쇄망 환경 설치 완료 (`219.248.153.178:11434`)
* `gpt-oss:20b` 모델의 Embedding 지원 여부 확인
* 필요 시 Embedding 전용 모델 추가 설치 검토
* Pandoc 도입 여부 결정 (Phase 2에서)

---

**문의사항이나 추가 요구사항은 프로젝트 관리자에게 연락하세요.**

**관련 문서**
* [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md) - 현재 SR 관리 시스템 현황
* [API.md](API.md) - 기존 REST API 명세
* [DATABASE.md](DATABASE.md) - 기존 데이터베이스 설계
