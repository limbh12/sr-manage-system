# 변경 이력 - Wiki 기능 Phase 2: PDF 변환 및 목차 자동 생성 (2025-12-19)

## 개요
Wiki 기능 Phase 2로 PDF 문서 자동 변환, 이미지 추출, 목차 자동 생성 기능을 구현하였습니다.
사용자는 PDF 파일을 업로드하면 자동으로 마크다운 문서로 변환되며, 이미지는 원본 페이지 위치에 배치됩니다.
또한 마크다운 문서 작성 시 목차를 자동으로 생성할 수 있습니다.

## 주요 변경사항

### 1. PDF 변환 기능

#### 1.1 Backend 구현

**PdfConversionService.java** (신규)
- 위치: `backend/src/main/java/com/srmanagement/wiki/service/PdfConversionService.java`
- 기능: PDF를 마크다운으로 변환하고 이미지를 추출하는 핵심 서비스
- 주요 메서드:
  - `convertPdfToMarkdownWithImages()`: PDF를 페이지별로 텍스트와 이미지 위치 마커로 변환
  - `extractTextByPages()`: PDF를 페이지 단위로 텍스트 추출
  - `extractTextFromPdf()`: Apache Tika를 이용한 전체 텍스트 추출
  - `convertToMarkdown()`: 텍스트를 마크다운 형식으로 변환 (헤더 자동 감지)
  - `extractImages()`: PDF에서 이미지 추출 및 저장
  - `extractMetadata()`: PDF 메타데이터 추출 (제목, 작성자, 페이지 수)
- 사용 라이브러리:
  - Apache Tika 2.9.1: 텍스트 추출
  - Apache PDFBox 2.0.30: 이미지 추출
- 반환 타입: `PdfConversionResult` (마크다운 텍스트 + 총 페이지 수)

**WikiFileService.java** (수정)
- `convertPdfToWikiDocument()` 메서드 대폭 개선:
  - 페이지별 이미지 배치: `{{IMAGES_PAGE_N}}` 마커를 실제 이미지 링크로 대체
  - 버전 1 자동 생성: 새 문서 생성 시 자동으로 버전 이력 생성
  - PDF 변환 상태 관리: `ConversionStatus` ENUM 사용
  - 에러 처리: 변환 실패 시 상세 에러 메시지 저장
- 이미지 그룹화 로직:
  - `ExtractedImage.pageNumber` 기반으로 이미지를 페이지별로 분류
  - 각 페이지 끝에 "📷 이미지" 섹션으로 배치
  - 마크다운 형식: `![이미지 설명](다운로드URL)`
- 변환 상태 추적:
  - `PENDING` → `PROCESSING` → `COMPLETED` / `FAILED`
  - `convertedAt` 타임스탬프 자동 기록

**WikiFile.java** (Entity 수정)
- 신규 필드 추가:
  - `mimeType`: MIME 타입 (application/pdf, image/png 등)
  - `conversionStatus`: 변환 상태 (NOT_APPLICABLE, PENDING, PROCESSING, COMPLETED, FAILED)
  - `conversionErrorMessage`: 변환 실패 시 에러 메시지
  - `convertedAt`: 변환 완료 시각
- ENUM 추가:
  ```java
  public enum ConversionStatus {
      NOT_APPLICABLE,  // 변환 불필요 (이미지, 일반 첨부파일)
      PENDING,         // 변환 대기
      PROCESSING,      // 변환 중
      COMPLETED,       // 변환 완료
      FAILED           // 변환 실패
  }
  ```

**pom.xml** (의존성 추가)
```xml
<!-- PDF 처리 -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>2.0.30</version>
</dependency>

<!-- PDF 텍스트 추출 -->
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

#### 1.2 Frontend 구현

**PdfUploadModal.tsx** (신규)
- 위치: `frontend/src/components/wiki/PdfUploadModal.tsx`
- 기능: PDF 파일 업로드 및 Wiki 문서 변환 모달
- 주요 기능:
  - 드래그 앤 드롭 파일 업로드
  - 파일 크기 검증 (최대 20MB)
  - PDF 파일 타입 검증
  - 카테고리 선택 기능
  - 업로드 진행률 표시 (TODO: 백엔드 지원 필요)
  - 변환 완료 후 자동 페이지 이동
- 드래그 앤 드롭 버그 수정:
  - `event.preventDefault()` + `event.stopPropagation()`로 브라우저 PDF 뷰어 방지
  - `onDragEnter`, `onDragLeave`, `onDragOver`, `onDrop` 이벤트 핸들러 구현

**WikiPage.tsx** (수정)
- PDF 업로드 버튼 추가:
  - 위치: 툴바 우측 ("📄 PDF 업로드" 버튼)
  - 클릭 시 `PdfUploadModal` 오픈
- 카테고리 정보 전달:
  - 현재 선택된 카테고리를 모달에 자동 전달
  - 업로드 후 새 문서로 자동 이동

**wikiService.ts** (수정)
- `convertPdfToWiki()` API 추가:
  ```typescript
  convertPdfToWiki: async (fileId: number, categoryId?: number) => {
    const response = await api.post<WikiDocument>(
      `/api/wiki/files/${fileId}/convert`,
      null,
      { params: { categoryId } }
    );
    return response.data;
  }
  ```

**wiki.ts** (타입 수정)
- `WikiFile` 인터페이스에 필드 추가:
  ```typescript
  export interface WikiFile {
    // ... 기존 필드
    conversionStatus?: 'NOT_APPLICABLE' | 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
    conversionErrorMessage?: string;
    convertedAt?: string;
  }
  ```

#### 1.3 PDF 변환 워크플로우

```
1. 사용자가 PDF 파일 업로드
   ↓
2. WikiFileController.uploadFile()
   - 파일 저장 (./data/wiki-uploads/)
   - WikiFile 엔티티 생성 (type=DOCUMENT, conversionStatus=PENDING)
   ↓
3. 프론트엔드에서 변환 요청
   ↓
4. WikiFileController.convertPdfToWikiDocument()
   ↓
5. WikiFileService.convertPdfToWikiDocument()
   - 상태: PROCESSING
   - PdfConversionService 호출
   ↓
6. PdfConversionService.convertPdfToMarkdownWithImages()
   - 페이지별 텍스트 추출
   - 이미지 위치 마커 삽입 ({{IMAGES_PAGE_N}})
   ↓
7. PdfConversionService.extractImages()
   - 페이지별 이미지 추출
   - PNG 파일로 저장
   ↓
8. WikiFileService: 마커 대체
   - 페이지별 이미지 그룹화
   - {{IMAGES_PAGE_N}} → 실제 이미지 마크다운 링크
   ↓
9. WikiDocument 생성 + 버전 1 생성
   ↓
10. 상태: COMPLETED, convertedAt 기록
    ↓
11. 프론트엔드로 문서 ID 반환 → 자동 페이지 이동
```

---

### 2. 목차 자동 생성 기능

#### 2.1 Backend 구현

**MarkdownTocGenerator.java** (신규)
- 위치: `backend/src/main/java/com/srmanagement/wiki/util/MarkdownTocGenerator.java`
- 기능: 마크다운 문서에서 제목(Heading)을 추출하여 목차(Table of Contents) 생성
- 주요 메서드:
  - `generateTableOfContents()`: 메인 진입점, 목차 생성 여부 옵션 처리
  - `removeExistingToc()`: 기존 `<!-- TOC -->` ~ `<!-- /TOC -->` 제거
  - `extractHeadings()`: 정규표현식으로 H2~H6 추출 (H1은 문서 제목이므로 제외)
  - `buildToc()`: 목차 마크다운 생성 (들여쓰기 + 앵커 링크)
  - `generateAnchor()`: GitHub/rehype-slug 호환 앵커 생성
  - `insertToc()`: 첫 번째 제목 앞에 목차 삽입
- 앵커 링크 생성 규칙:
  1. 소문자 변환
  2. 공백을 하이픈(-)으로 변환
  3. 특수문자 제거 (알파벳, 숫자, 한글, 하이픈, 언더스코어만 유지)
  4. 연속된 하이픈을 하나로 축약
  5. 앞뒤 하이픈 제거
- 목차 형식:
  ```markdown
  <!-- TOC -->
  ## 📑 목차

  - [제목1](#제목1)
    - [제목1-1](#제목1-1)
  - [제목2](#제목2)

  <!-- /TOC -->
  ```

**WikiDocumentService.java** (수정)
- `createDocument()` 메서드 수정:
  - `request.getGenerateToc()` 옵션 확인
  - `true`일 경우 `MarkdownTocGenerator.generateTableOfContents()` 호출
  - 목차가 포함된 content를 WikiDocument와 WikiVersion에 저장
- `updateDocument()` 메서드 수정:
  - 동일한 목차 생성 로직 적용
  - 내용 변경 시 버전 생성

**WikiDocumentRequest.java** (DTO 수정)
- 신규 필드 추가:
  ```java
  /**
   * 목차 자동 생성 여부 (기본값: false)
   */
  private Boolean generateToc;
  ```

#### 2.2 Frontend 구현

**WikiViewer.tsx** (수정)
- `rehype-slug` 플러그인 추가:
  - 마크다운 제목에 자동으로 ID 속성 부여
  - GitHub 스타일 앵커 링크 생성
  - 설치: `npm install rehype-slug --legacy-peer-deps`
- 링크 처리 로직 개선:
  ```tsx
  components={{
    a: ({ href, children }) => {
      // 앵커 링크 (#로 시작) 또는 상대 경로는 같은 페이지에서 열기
      if (href?.startsWith('#') || href?.startsWith('/')) {
        return <a href={href}>{children}</a>;
      }
      // 외부 링크는 새 탭에서 열기
      return (
        <a href={href} target="_blank" rel="noopener noreferrer">
          {children}
        </a>
      );
    },
  }}
  ```
- 버그 수정: 목차 링크 클릭 시 새 탭으로 열리며 401 오류 발생하던 문제 해결
  - 원인: 모든 링크에 `target="_blank"` 적용
  - 해결: 앵커 링크(#)는 같은 페이지에서 스크롤, 외부 링크만 새 탭

**WikiPage.tsx** (수정)
- 목차 자동 생성 체크박스 추가:
  ```tsx
  <label className="toc-checkbox-label">
    <input
      type="checkbox"
      checked={generateToc}
      onChange={(e) => setGenerateToc(e.target.checked)}
    />
    📑 목차 자동 생성
  </label>
  ```
- 위치: 편집 모드 툴바, "저장" 버튼 왼쪽
- 저장 시 `generateToc` 값을 API 요청에 포함

**wiki.ts** (타입 수정)
- `WikiDocumentRequest` 인터페이스에 필드 추가:
  ```typescript
  export interface WikiDocumentRequest {
    // ... 기존 필드
    generateToc?: boolean; // 목차 자동 생성 옵션
  }
  ```

**package.json** (의존성 추가)
```json
{
  "rehype-slug": "^6.0.0"
}
```

#### 2.3 목차 생성 워크플로우

```
1. 사용자가 마크다운 문서 작성
   ↓
2. "목차 자동 생성" 체크박스 선택
   ↓
3. "저장" 버튼 클릭
   ↓
4. WikiDocumentController.createDocument() / updateDocument()
   ↓
5. WikiDocumentService: generateToc=true 확인
   ↓
6. MarkdownTocGenerator.generateTableOfContents()
   - 제목 추출 (H2~H6)
   - 앵커 링크 생성
   - 목차 마크다운 생성
   - 첫 번째 제목 앞에 삽입
   ↓
7. 목차가 포함된 content를 DB에 저장
   ↓
8. 프론트엔드: ReactMarkdown + rehype-slug로 렌더링
   ↓
9. 목차 링크 클릭 → 같은 페이지 내 스크롤 이동
```

---

### 3. 데이터베이스 스키마 변경

#### 3.1 마이그레이션 스크립트 업데이트

**Phase 1 + Phase 2 통합 스크립트**
- 파일:
  - `migration_20251219_wiki_tables_mysql.sql`
  - `migration_20251219_wiki_tables_h2.sql`
  - `migration_20251219_wiki_tables_postgresql.sql`
  - `migration_20251219_wiki_tables_cubrid.sql`
- 변경: Phase 1 스크립트에 Phase 2 컬럼 추가
- 설명 수정: "Wiki 기능 (Phase 1 + Phase 2 PDF 변환)"

#### 3.2 wiki_file 테이블 컬럼 추가

**MySQL**
```sql
CREATE TABLE IF NOT EXISTS wiki_file (
    -- ... 기존 컬럼
    mime_type VARCHAR(50) COMMENT 'MIME 타입 (application/pdf, image/png 등)',
    conversion_status VARCHAR(20) NOT NULL DEFAULT 'NOT_APPLICABLE'
        COMMENT '변환 상태: NOT_APPLICABLE, PENDING, PROCESSING, COMPLETED, FAILED',
    conversion_error_message VARCHAR(1000) COMMENT 'PDF 변환 실패 시 에러 메시지',
    converted_at TIMESTAMP COMMENT 'PDF 변환 완료 시각',
    -- ...
    INDEX idx_wiki_file_conversion_status (conversion_status)
);
```

**PostgreSQL**
```sql
CREATE TYPE IF NOT EXISTS wiki_conversion_status AS ENUM (
    'NOT_APPLICABLE', 'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'
);

CREATE TABLE IF NOT EXISTS wiki_file (
    -- ... 기존 컬럼
    mime_type VARCHAR(50),
    conversion_status wiki_conversion_status NOT NULL DEFAULT 'NOT_APPLICABLE',
    conversion_error_message VARCHAR(1000),
    converted_at TIMESTAMP,
    -- ...
);

-- 컬럼 설명 추가
COMMENT ON COLUMN wiki_file.mime_type IS 'MIME 타입 (application/pdf, image/png 등)';
COMMENT ON COLUMN wiki_file.conversion_status IS '변환 상태: NOT_APPLICABLE, PENDING, PROCESSING, COMPLETED, FAILED';
COMMENT ON COLUMN wiki_file.conversion_error_message IS 'PDF 변환 실패 시 에러 메시지';
COMMENT ON COLUMN wiki_file.converted_at IS 'PDF 변환 완료 시각';
```

**CUBRID**
```sql
CREATE TABLE IF NOT EXISTS wiki_file (
    -- ... 기존 컬럼
    mime_type VARCHAR(50),
    conversion_status VARCHAR(20) NOT NULL DEFAULT 'NOT_APPLICABLE',
    conversion_error_message VARCHAR(1000),
    converted_at TIMESTAMP,
    -- ...
    CONSTRAINT chk_wiki_file_conversion_status
        CHECK (conversion_status IN ('NOT_APPLICABLE', 'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_wiki_file_conversion_status ON wiki_file(conversion_status);
```

**H2**
```sql
CREATE TABLE IF NOT EXISTS wiki_file (
    -- ... 기존 컬럼
    mime_type VARCHAR(50),
    conversion_status VARCHAR(20) NOT NULL DEFAULT 'NOT_APPLICABLE',
    conversion_error_message VARCHAR(1000),
    converted_at TIMESTAMP,
    -- ...
);

CREATE INDEX IF NOT EXISTS idx_wiki_file_conversion_status ON wiki_file(conversion_status);
```

#### 3.3 인덱스 추가

- `idx_wiki_file_conversion_status`: 변환 상태별 조회 성능 향상
- 사용 케이스: 변환 대기/실패 파일 조회

---

### 4. 주요 기능 설명

#### 4.1 PDF 페이지별 이미지 배치

**문제**
- PDF 변환 시 모든 이미지가 문서 끝에 몰림
- 원본 PDF의 이미지 위치 정보 손실

**해결**
1. **페이지별 텍스트 추출**
   - `PdfConversionService.extractTextByPages()` 사용
   - PDFBox로 PDF를 페이지 단위로 분리
   - Tika로 각 페이지의 텍스트 추출

2. **이미지 위치 마커 삽입**
   - 각 페이지 끝에 `{{IMAGES_PAGE_N}}` 마커 추가
   - 예: `{{IMAGES_PAGE_1}}`, `{{IMAGES_PAGE_2}}`

3. **이미지 추출 및 페이지 번호 기록**
   - `ExtractedImage.pageNumber` 필드에 페이지 정보 저장
   - 파일명 형식: `page_1_img_1.png`, `page_2_img_1.png`

4. **마커를 실제 이미지 링크로 대체**
   - 페이지별 이미지 그룹화
   - 마커를 마크다운 이미지 링크로 변환
   - 형식:
     ```markdown
     ### 📷 이미지

     ![이미지 1](http://localhost:8080/api/wiki/files/123)

     ![이미지 2](http://localhost:8080/api/wiki/files/124)
     ```

**결과**
- 이미지가 원본 PDF의 페이지 위치에 배치됨
- 문서의 가독성과 구조 유지

#### 4.2 PDF 변환 시 버전 1 자동 생성

**문제**
- PDF 변환으로 생성된 문서는 버전 이력이 없음
- 수정 시 버전 2부터 시작되어 이력 추적 어려움

**해결**
```java
// WikiFileService.convertPdfToWikiDocument()
if (isNewDocument) {
    WikiVersion firstVersion = WikiVersion.builder()
            .document(savedDocument)
            .version(1)
            .content(markdown)
            .changeSummary("PDF 변환으로 생성")
            .createdBy(wikiFile.getUploadedBy())
            .build();
    wikiVersionRepository.save(firstVersion);
}
```

**결과**
- PDF 변환 문서도 버전 1부터 이력 추적 가능
- 일반 문서와 동일한 버전 관리 경험

#### 4.3 목차 앵커 링크 호환성

**문제**
- Backend 생성 앵커와 Frontend 렌더링 앵커 불일치
- 목차 링크 클릭 시 스크롤 작동 안 함

**해결**
1. **Backend: rehype-slug 알고리즘 구현**
   ```java
   private static String generateAnchor(String text) {
       return text.toLowerCase()
                  .replaceAll("[^a-z0-9가-힣\\s_-]", "")  // 특수문자 제거
                  .replaceAll("\\s+", "-")                 // 공백 → 하이픈
                  .replaceAll("-+", "-")                   // 연속 하이픈 제거
                  .replaceAll("^-|-$", "");                // 앞뒤 하이픈 제거
   }
   ```

2. **Frontend: rehype-slug 플러그인 사용**
   ```tsx
   <ReactMarkdown
     rehypePlugins={[rehypeSlug, rehypeHighlight, rehypeRaw]}
   >
   ```

**결과**
- Backend 생성 `[제목](#제목)` ↔ Frontend 렌더링 `<h2 id="제목">` 정확히 매칭
- 목차 링크 클릭 시 부드럽게 스크롤 이동

#### 4.4 드래그 앤 드롭 PDF 뷰어 방지

**문제**
- PDF 파일을 드래그하면 Chrome이 PDF 뷰어로 열어버림
- 파일 업로드 동작이 중단됨

**해결**
```tsx
const handleDrop = (e: React.DragEvent) => {
  e.preventDefault();      // 브라우저 기본 동작 방지
  e.stopPropagation();     // 이벤트 버블링 방지

  setIsDragging(false);
  const files = e.dataTransfer.files;
  if (files.length > 0) {
    handleFileSelect(files[0]);
  }
};
```

**결과**
- PDF 파일을 드래그해도 뷰어가 열리지 않음
- 정상적으로 파일 업로드 진행

---

### 5. 파일 변경 목록

#### 5.1 Backend 신규 파일 (2개)

**Service (1개)**
- `wiki/service/PdfConversionService.java`

**Util (1개)**
- `wiki/util/MarkdownTocGenerator.java`

#### 5.2 Backend 수정 파일 (5개)

**Service (1개)**
- `wiki/service/WikiFileService.java`
  - `convertPdfToWikiDocument()` 메서드 대폭 개선
  - 페이지별 이미지 배치 로직 추가
  - 버전 1 자동 생성 로직 추가

- `wiki/service/WikiDocumentService.java`
  - `createDocument()`: 목차 생성 옵션 처리
  - `updateDocument()`: 목차 생성 옵션 처리

**Entity (1개)**
- `wiki/entity/WikiFile.java`
  - `mimeType`, `conversionStatus`, `conversionErrorMessage`, `convertedAt` 필드 추가
  - `ConversionStatus` ENUM 추가

**DTO (1개)**
- `wiki/dto/WikiDocumentRequest.java`
  - `generateToc` 필드 추가

**Build (1개)**
- `pom.xml`
  - Apache PDFBox 2.0.30 의존성 추가
  - Apache Tika 2.9.1 의존성 추가

#### 5.3 Frontend 신규 파일 (2개)

**Components (1개)**
- `components/wiki/PdfUploadModal.tsx`
- `components/wiki/PdfUploadModal.css`

#### 5.4 Frontend 수정 파일 (6개)

**Components (1개)**
- `components/wiki/WikiViewer.tsx`
  - `rehype-slug` 플러그인 추가
  - 링크 처리 로직 개선 (앵커 링크 vs 외부 링크)

**Pages (1개)**
- `pages/WikiPage.tsx`
  - PDF 업로드 버튼 추가
  - 목차 자동 생성 체크박스 추가
  - `PdfUploadModal` 컴포넌트 통합

**Services (1개)**
- `services/wikiService.ts`
  - `convertPdfToWiki()` API 추가

**Types (1개)**
- `types/wiki.ts`
  - `WikiFile` 인터페이스에 변환 관련 필드 추가
  - `WikiDocumentRequest` 인터페이스에 `generateToc` 추가

**Build (1개)**
- `package.json`
  - `rehype-slug` 의존성 추가

#### 5.5 데이터베이스 스키마 (4개 수정)

**Migration Scripts**
- `backend/src/main/resources/migration_20251219_wiki_tables_mysql.sql`
- `backend/src/main/resources/migration_20251219_wiki_tables_h2.sql`
- `backend/src/main/resources/migration_20251219_wiki_tables_postgresql.sql`
- `backend/src/main/resources/migration_20251219_wiki_tables_cubrid.sql`

**변경 내용**
- 설명: "Phase 1" → "Phase 1 + Phase 2 PDF 변환"
- `wiki_file` 테이블에 컬럼 4개 추가
- 인덱스 1개 추가

---

### 6. 테스트 결과

#### 6.1 PDF 변환 테스트

**시나리오**
1. 5페이지 PDF 업로드 (이미지 3개 포함)
2. 자동 변환 실행
3. 결과 확인

**결과**
- ✅ 텍스트 추출 성공
- ✅ 이미지 3개 추출 성공
- ✅ 페이지별 이미지 배치 성공
- ✅ 버전 1 자동 생성 성공
- ✅ 변환 상태: COMPLETED
- ✅ convertedAt 타임스탬프 기록

**생성된 마크다운**
```markdown
# 문서 제목

페이지 1의 내용...

### 📷 이미지

![이미지 1](http://localhost:8080/api/wiki/files/123)

---

## Page 2

페이지 2의 내용...

### 📷 이미지

![이미지 2](http://localhost:8080/api/wiki/files/124)

![이미지 3](http://localhost:8080/api/wiki/files/125)
```

#### 6.2 목차 자동 생성 테스트

**시나리오**
1. 마크다운 문서 작성 (H2, H3 제목 포함)
2. "목차 자동 생성" 체크박스 선택
3. 저장
4. 렌더링 확인

**결과**
- ✅ 목차 생성 성공
- ✅ H1 제외, H2~H6만 목차에 포함
- ✅ 들여쓰기 정확 (H2는 들여쓰기 없음, H3부터 2칸씩)
- ✅ 앵커 링크 클릭 시 스크롤 이동 성공
- ✅ 외부 링크는 새 탭에서 열림

**생성된 목차**
```markdown
<!-- TOC -->
## 📑 목차

- [소개](#소개)
  - [배경](#배경)
  - [목적](#목적)
- [설치 방법](#설치-방법)
- [사용법](#사용법)

<!-- /TOC -->
```

#### 6.3 드래그 앤 드롭 테스트

**시나리오**
1. PDF 파일을 업로드 영역으로 드래그
2. 파일 드롭

**결과**
- ✅ Chrome PDF 뷰어가 열리지 않음
- ✅ 파일 선택 성공
- ✅ 파일명 표시 정확
- ✅ 업로드 진행

#### 6.4 빌드 및 실행 테스트

**Backend**
```
[INFO] Compiling 88 source files
[INFO] BUILD SUCCESS
```
- 총 88개 Java 파일 (+2개: PdfConversionService, MarkdownTocGenerator)
- 컴파일 에러: 0
- 경고: PDFBox/Tika 관련 로깅 경고 (무시 가능)

**Frontend**
```
✓ 689 modules transformed
✓ built in 2.61s
dist/assets/index-CX7nzPLC.js   1,637.28 kB │ gzip: 512.13 kB
```
- 빌드 성공
- +2개 컴포넌트 (PdfUploadModal, 수정된 WikiViewer)
- 경고: Chunk size (기존과 동일, 기능 정상)

**통합 서버**
```
Server started successfully!
PID: 52143
Backend API: http://localhost:8080
Frontend: http://localhost:8080
```
- 서버 시작 성공
- H2 데이터베이스 초기화 완료 (wiki_file 테이블에 신규 컬럼 자동 생성)

---

### 7. 주요 기술 스택

#### 7.1 Backend

**PDF 처리**
- Apache PDFBox 2.0.30: 이미지 추출, 페이지 분리
- Apache Tika 2.9.1: 텍스트 추출, 메타데이터 파싱

**문서 처리**
- Java Regex: 마크다운 제목 추출
- String Manipulation: 앵커 링크 생성

#### 7.2 Frontend

**Markdown 렌더링**
- react-markdown: 마크다운 파싱 및 렌더링
- rehype-slug: 제목에 ID 자동 부여
- rehype-highlight: 코드 하이라이팅
- rehype-raw: HTML 태그 지원
- remark-gfm: GitHub Flavored Markdown

**파일 업로드**
- HTML5 Drag & Drop API
- React Event Handlers

---

### 8. 알려진 제한사항 및 향후 개선사항

#### 현재 제한사항

1. **PDF 변환 품질**
   - 복잡한 레이아웃(다단, 표) 처리 제한적
   - OCR 미지원 (스캔 PDF는 변환 불가)
   - TODO: PDF 레이아웃 분석 라이브러리 추가 고려

2. **이미지 배치**
   - 페이지 끝에만 배치 (텍스트 중간 위치 미지원)
   - TODO: PDF 좌표 기반 정밀 위치 지정

3. **목차 생성**
   - H1~H6만 지원 (커스텀 제목 미지원)
   - 중복 제목 시 앵커 충돌 가능 (rehype-slug는 자동 번호 추가)

4. **업로드 진행률**
   - 프론트엔드에서 진행률 표시 UI만 존재
   - TODO: 백엔드 WebSocket/SSE로 실시간 진행률 전송

#### Phase 3 계획 (PB 문서 참고)

**AI 검색 기능**
- RAG (Retrieval-Augmented Generation) 기반 자연어 검색
- Spring AI + Ollama 통합
- JdbcVectorStore (H2 기반) 벡터 저장
- Cosine Similarity 유사도 검색
- LLM 기반 답변 생성

---

### 9. 참고 문서

- **기획 문서**: `docs/PB_AI-Powered_Wiki.md` (Phase 2 완료, Phase 3 준비)
- **Phase 1 변경 이력**: `docs/HISTORY_20251219_WIKI_PHASE1.md`
- **데이터베이스 마이그레이션**: `backend/src/main/resources/migration_20251219_wiki_tables_*.sql`

---

## 작업자
- Claude Code (AI Assistant)

## 작업 일시
- 2025-12-19

## 검토자
- 검토 필요

---

## 체크리스트

### PDF 변환 기능
- [x] PdfConversionService 구현 완료
- [x] WikiFileService 페이지별 이미지 배치 완료
- [x] 버전 1 자동 생성 완료
- [x] ConversionStatus ENUM 추가 완료
- [x] PdfUploadModal 컴포넌트 완료
- [x] 드래그 앤 드롭 버그 수정 완료
- [x] 카테고리 선택 기능 추가 완료
- [x] PDFBox/Tika 의존성 추가 완료
- [x] Backend 컴파일 테스트 통과
- [x] Frontend 빌드 테스트 통과
- [x] PDF 변환 기능 테스트 성공

### 목차 자동 생성 기능
- [x] MarkdownTocGenerator 유틸리티 구현 완료
- [x] WikiDocumentService 목차 생성 통합 완료
- [x] WikiDocumentRequest DTO 수정 완료
- [x] WikiViewer rehype-slug 통합 완료
- [x] 앵커 링크 호환성 확보 완료
- [x] 목차 체크박스 UI 추가 완료
- [x] 목차 생성 테스트 성공
- [x] 앵커 링크 스크롤 이동 테스트 성공

### 데이터베이스
- [x] wiki_file 테이블 컬럼 추가 (4개 DB)
- [x] 인덱스 추가 (conversion_status)
- [x] 마이그레이션 스크립트 업데이트 완료

### 문서화
- [x] Phase 2 변경 이력 작성 완료
- [ ] PB 문서 업데이트 (진행 중)
- [ ] API 문서 업데이트 (TODO)
- [ ] 사용자 가이드 업데이트 (TODO)

---

## 비고

Phase 2 작업으로 Wiki 시스템에 PDF 자동 변환 및 목차 생성 기능이 추가되었습니다.
사용자는 이제 PDF 문서를 업로드하면 자동으로 마크다운 문서로 변환되며,
이미지는 원본 페이지 위치에 배치됩니다. 또한 마크다운 문서 작성 시 체크박스 하나로
GitHub 스타일 목차를 자동 생성할 수 있습니다.

Phase 3에서는 AI 기반 자연어 검색 기능을 추가하여
사용자가 질문을 입력하면 관련 문서를 찾아 LLM이 답변을 생성하는
완전한 지식 관리 시스템으로 발전시킬 예정입니다.
