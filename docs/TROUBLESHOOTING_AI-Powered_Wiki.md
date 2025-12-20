# AI-Powered Wiki Troubleshooting Guide

AI 기반 지능형 위키 시스템 개발 중 발생한 주요 이슈와 해결 방법을 정리한 문서입니다.

---

## 목차

1. [Wiki Phase 2: PDF 뷰어 관련](#wiki-phase-2-pdf-뷰어-관련)
   - [TS-P2-1: MultipleBagFetchException](#ts-p2-1-multiplebagfetchexception)
   - [TS-P2-2: 폐쇄망 PDF.js Worker 로딩 실패](#ts-p2-2-폐쇄망-pdfjs-worker-로딩-실패)
   - [TS-P2-3: react-pdf와 pdfjs-dist 버전 불일치](#ts-p2-3-react-pdf와-pdfjs-dist-버전-불일치)
   - [TS-P2-4: Spring Boot .mjs 파일 MIME 타입 문제](#ts-p2-4-spring-boot-mjs-파일-mime-타입-문제)
   - [TS-P2-5: 드래그 앤 드롭 시 Chrome PDF 뷰어 열림](#ts-p2-5-드래그-앤-드롭-시-chrome-pdf-뷰어-열림)
   - [TS-P2-6: 목차 링크 클릭 시 401 오류](#ts-p2-6-목차-링크-클릭-시-401-오류)

2. [Wiki Phase 3: AI 검색 관련](#wiki-phase-3-ai-검색-관련)
   - [TS-P3-1: 임베딩 상태 불일치 - isUpToDate 항상 false](#ts-p3-1-임베딩-상태-불일치---isuptodate-항상-false)
   - [TS-P3-2: JSON 필드명 불일치 (upToDate vs isUpToDate)](#ts-p3-2-json-필드명-불일치-uptodate-vs-isuptodate)
   - [TS-P3-3: SSE 인증 문제로 폴링 전환](#ts-p3-3-sse-인증-문제로-폴링-전환)

---

## Wiki Phase 2: PDF 뷰어 관련

### TS-P2-1: MultipleBagFetchException

**발생일**: 2025-12-19
**심각도**: HIGH
**영향 범위**: Wiki 문서 조회 API

#### 증상

```
org.hibernate.loader.MultipleBagFetchException: cannot simultaneously fetch multiple bags
```

- Wiki 문서 조회 시 서버 500 오류 발생

#### 원인

- JPA에서 여러 List 컬렉션(files, srs, versions)을 한 쿼리에서 fetch join 불가
- `WikiDocument` 엔티티에 `@OneToMany` 관계가 3개 존재

```java
@OneToMany
private List<WikiFile> files;

@ManyToMany
private List<SR> srs;

@OneToMany
private List<WikiVersion> versions;
```

#### 해결책

쿼리를 3개로 분리하여 각각 fetch join

```java
// WikiDocumentRepository.java

// 메인 쿼리 - files만 fetch (PDF 뷰어용)
@Query("SELECT DISTINCT wd FROM WikiDocument wd " +
       "LEFT JOIN FETCH wd.category " +
       "LEFT JOIN FETCH wd.createdBy " +
       "LEFT JOIN FETCH wd.updatedBy " +
       "LEFT JOIN FETCH wd.files " +
       "WHERE wd.id = :id")
Optional<WikiDocument> findByIdWithDetails(@Param("id") Long id);

// SR 목록 별도 조회
@Query("SELECT DISTINCT wd FROM WikiDocument wd " +
       "LEFT JOIN FETCH wd.srs " +
       "WHERE wd.id = :id")
Optional<WikiDocument> findByIdWithSrs(@Param("id") Long id);

// 버전 목록 별도 조회
@Query("SELECT DISTINCT wd FROM WikiDocument wd " +
       "LEFT JOIN FETCH wd.versions " +
       "WHERE wd.id = :id")
Optional<WikiDocument> findByIdWithVersions(@Param("id") Long id);
```

#### 교훈

1. **JPA List 컬렉션 제한**: 여러 `@OneToMany` List를 동시에 fetch join 불가
2. **해결 방법들**:
   - 쿼리 분리 (권장)
   - `Set`으로 변경 (순서가 필요 없는 경우)
   - `@BatchSize` 사용 (N+1 최적화)

#### 관련 파일

- [WikiDocumentRepository.java](../backend/src/main/java/com/srmanagement/wiki/repository/WikiDocumentRepository.java)
- [WikiDocumentService.java](../backend/src/main/java/com/srmanagement/wiki/service/WikiDocumentService.java)

---

### TS-P2-2: 폐쇄망 PDF.js Worker 로딩 실패

**발생일**: 2025-12-19
**심각도**: HIGH
**영향 범위**: PDF 뷰어 기능 전체

#### 증상

- PDF 렌더링 실패
- 콘솔에 worker 파일 로딩 오류

#### 원인

- react-pdf 기본 설정이 unpkg CDN에서 worker 로드
- 폐쇄망에서 외부 CDN 접근 불가

#### 시도한 방법들

| 방법 | 결과 |
|------|------|
| CDN URL 사용 | ❌ 폐쇄망 접근 불가 |
| public 폴더에 worker 복사 | ❌ Spring Boot .mjs 서빙 실패 |
| Vite import.meta.url 패턴 | ✅ 성공 |

#### 해결책

Vite의 `new URL(..., import.meta.url)` 패턴 사용

```typescript
// PdfViewer.tsx
import * as pdfjs from 'pdfjs-dist';

pdfjs.GlobalWorkerOptions.workerSrc = new URL(
  'pdfjs-dist/build/pdf.worker.min.mjs',
  import.meta.url
).toString();
```

**효과**:
- Worker 파일이 빌드 시 번들에 포함됨
- 빌드 결과: `dist/assets/pdf.worker.min-qwK7q_zL.mjs` (1.04MB)
- 외부 네트워크 없이 완전히 자체 포함

#### 교훈

1. **폐쇄망 환경 고려**: 외부 CDN 의존성 제거 필수
2. **Vite 정적 자산 처리**: `import.meta.url` 패턴으로 번들 포함 가능
3. **번들 크기 증가**: PDF.js worker (~1MB) 포함으로 초기 로딩 시간 증가

#### 관련 파일

- [PdfViewer.tsx](../frontend/src/components/wiki/PdfViewer.tsx)

---

### TS-P2-3: react-pdf와 pdfjs-dist 버전 불일치

**발생일**: 2025-12-19
**심각도**: MEDIUM
**영향 범위**: PDF 렌더링

#### 증상

```
The API version "5.4.296" does not match the Worker version "5.4.449"
```

- PDF가 렌더링되지 않음

#### 원인

- react-pdf 10.2.0이 특정 pdfjs-dist 버전을 요구
- npm이 최신 버전(5.4.449) 자동 설치
- API와 Worker 버전 불일치

#### 해결책

pdfjs-dist 버전을 명시적으로 고정

```json
// package.json
{
  "dependencies": {
    "react-pdf": "^10.2.0",
    "pdfjs-dist": "5.4.296"  // 캐럿(^) 제거하여 정확한 버전 고정
  }
}
```

```bash
# 의존성 재설치
rm -rf node_modules package-lock.json
npm install --legacy-peer-deps
```

#### 교훈

1. **버전 호환성**: 라이브러리 간 버전 호환성 확인 필수
2. **정확한 버전 고정**: 호환성이 중요한 경우 캐럿(^) 제거
3. **의존성 재설치**: 버전 변경 시 node_modules 완전 삭제 후 재설치

#### 관련 파일

- [package.json](../frontend/package.json)

---

### TS-P2-4: Spring Boot .mjs 파일 MIME 타입 문제

**발생일**: 2025-12-19
**심각도**: MEDIUM
**영향 범위**: PDF.js worker 로딩

#### 증상

- .mjs 파일이 올바른 MIME 타입으로 서빙되지 않음
- 브라우저에서 JavaScript로 인식 안 함

#### 원인

- Spring Boot가 기본적으로 .mjs 확장자의 MIME 타입 미설정
- SecurityConfig에서 .mjs 파일 접근 차단

#### 해결책

**1. SecurityConfig에서 .mjs 파일 접근 허용**

```java
// SecurityConfig.java
.requestMatchers("/static/**", "/assets/**").permitAll()
.requestMatchers("/*.js", "/*.mjs", "/*.css", "/*.png", "/*.svg", "/*.ico").permitAll()
```

**2. WebConfig에서 MIME 타입 매핑**

```java
// WebConfig.java (신규 생성)
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureContentNegotiation(@NonNull ContentNegotiationConfigurer configurer) {
        configurer
                .favorParameter(false)
                .ignoreAcceptHeader(false)
                .defaultContentType(MediaType.APPLICATION_JSON)
                .mediaType("mjs", MediaType.valueOf("application/javascript"))
                .mediaType("js", MediaType.valueOf("application/javascript"));
    }
}
```

#### 교훈

1. **새로운 확장자 지원**: Spring Boot에 명시적 MIME 타입 설정 필요
2. **Security 설정 확인**: 정적 리소스 접근 허용 패턴 확인
3. **ES Module 지원**: .mjs 파일도 JavaScript로 서빙되어야 함

#### 관련 파일

- [SecurityConfig.java](../backend/src/main/java/com/srmanagement/config/SecurityConfig.java)
- [WebConfig.java](../backend/src/main/java/com/srmanagement/config/WebConfig.java)

---

### TS-P2-5: 드래그 앤 드롭 시 Chrome PDF 뷰어 열림

**발생일**: 2025-12-19
**심각도**: LOW
**영향 범위**: PDF 업로드 UX

#### 증상

- PDF 파일을 드래그하면 Chrome이 PDF 뷰어로 열어버림
- 파일 업로드 동작이 중단됨

#### 원인

- 브라우저 기본 동작으로 PDF 파일 열기 시도
- 이벤트 버블링으로 상위 요소까지 전파

#### 해결책

```tsx
// PdfUploadModal.tsx
const handleDrop = (e: React.DragEvent) => {
  e.preventDefault();      // 브라우저 기본 동작 방지
  e.stopPropagation();     // 이벤트 버블링 방지

  setIsDragging(false);
  const files = e.dataTransfer.files;
  if (files.length > 0) {
    handleFileSelect(files[0]);
  }
};

// 모든 드래그 관련 이벤트에 적용
const handleDragEnter = (e: React.DragEvent) => {
  e.preventDefault();
  e.stopPropagation();
  setIsDragging(true);
};

const handleDragLeave = (e: React.DragEvent) => {
  e.preventDefault();
  e.stopPropagation();
  setIsDragging(false);
};

const handleDragOver = (e: React.DragEvent) => {
  e.preventDefault();
  e.stopPropagation();
};
```

#### 교훈

1. **브라우저 기본 동작**: 파일 드래그 시 브라우저가 직접 열기 시도
2. **이벤트 핸들링**: `preventDefault()` + `stopPropagation()` 조합 필요
3. **모든 드래그 이벤트**: Enter, Leave, Over, Drop 모두 처리해야 함

#### 관련 파일

- [PdfUploadModal.tsx](../frontend/src/components/wiki/PdfUploadModal.tsx)

---

### TS-P2-6: 목차 링크 클릭 시 401 오류

**발생일**: 2025-12-19
**심각도**: MEDIUM
**영향 범위**: 목차 네비게이션

#### 증상

- 목차 링크 클릭 시 새 탭으로 열리며 401 Unauthorized 오류
- 앵커 링크(#)도 외부 링크처럼 동작

#### 원인

- ReactMarkdown에서 모든 링크에 `target="_blank"` 적용
- 앵커 링크(#)도 새 탭에서 열려 JWT 토큰 없이 요청

```tsx
// 문제가 된 코드
components={{
  a: ({ href, children }) => (
    <a href={href} target="_blank" rel="noopener noreferrer">
      {children}
    </a>
  ),
}}
```

#### 해결책

앵커 링크와 외부 링크 구분 처리

```tsx
// WikiViewer.tsx
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

#### 교훈

1. **링크 유형 구분**: 앵커/상대/외부 링크별 다른 처리 필요
2. **SPA 네비게이션**: 같은 페이지 내 이동은 `target="_blank"` 제외
3. **rehype-slug 연동**: 서버에서 생성한 앵커와 클라이언트 ID 일치 확인

#### 관련 파일

- [WikiViewer.tsx](../frontend/src/components/wiki/WikiViewer.tsx)

---

## Wiki Phase 3: AI 검색 관련

### TS-P3-1: 임베딩 상태 불일치 - `isUpToDate` 항상 false

**발생일**: 2025-12-20
**심각도**: HIGH
**영향 범위**: Wiki 문서 임베딩 상태 표시

#### 증상

- 임베딩 생성 완료 후에도 "임베딩 재생성 필요" 상태 표시
- 다른 문서로 이동 후 돌아오면 상태가 바뀜
- 문서 조회만 해도 상태가 변경됨

#### 근본 원인

**문제 1: 조회수 증가 시 updatedAt 변경**

```java
// WikiDocument 엔티티
@UpdateTimestamp
private LocalDateTime updatedAt;  // 모든 save() 호출 시 자동 갱신
```

- `WikiDocument` 엔티티의 `updatedAt`에 `@UpdateTimestamp` 어노테이션 적용
- 문서 조회 시 `viewCount` 증가를 위해 `save()` 호출
- **조회할 때마다 `updatedAt`이 현재 시간으로 갱신됨**
- 임베딩의 `sourceDocumentUpdatedAt`과 불일치 발생

**문제 2: 비동기 트랜잭션 타이밍**

- 문서 저장 트랜잭션이 커밋되기 전에 비동기 임베딩 스레드 시작
- 비동기 스레드에서 문서 조회 시 이전 버전의 `updatedAt` 읽음

#### 해결책

**해결 1: 조회수 증가를 네이티브 쿼리로 변경**

```java
// WikiDocumentRepository.java
@Modifying
@Query("UPDATE WikiDocument wd SET wd.viewCount = wd.viewCount + 1 WHERE wd.id = :id")
void incrementViewCount(@Param("id") Long id);

// WikiDocumentService.java
public WikiDocumentResponse getDocumentAndIncrementViewCount(Long id) {
    WikiDocument document = wikiDocumentRepository.findByIdWithDetails(id)...;
    // 네이티브 쿼리로 조회수 증가 (updatedAt 미변경)
    wikiDocumentRepository.incrementViewCount(id);
    return WikiDocumentResponse.fromEntity(document);
}
```

**해결 2: 비동기 임베딩 시작 전 대기**

```java
@Async("embeddingTaskExecutor")
@Transactional
public void generateEmbeddingsAsync(Long documentId) {
    // 호출자의 트랜잭션 커밋 대기 (500ms)
    try {
        Thread.sleep(500);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
    // 이후 문서 조회 및 임베딩 생성...
}
```

**해결 3: sourceDocumentUpdatedAt 필드 추가**

```java
// WikiDocumentEmbedding.java
/**
 * 임베딩 생성 시 참조한 문서의 updatedAt 시점
 * (타이밍 이슈 없이 정확한 최신 여부 비교용)
 */
private LocalDateTime sourceDocumentUpdatedAt;

// AiSearchService.java - 임베딩 저장 시
.sourceDocumentUpdatedAt(document.getUpdatedAt())

// AiSearchService.java - 상태 비교 시
if (sourceDocumentUpdatedAt != null) {
    isUpToDate = sourceDocumentUpdatedAt.isEqual(document.getUpdatedAt()) ||
                sourceDocumentUpdatedAt.isAfter(document.getUpdatedAt());
}
```

#### 교훈

1. **@UpdateTimestamp 사용 시 주의**: 모든 `save()` 호출에서 갱신됨. 조회수 같은 필드 업데이트 시 네이티브 쿼리 사용 권장
2. **비동기 메서드와 트랜잭션**: `@Async` 메서드는 호출자 트랜잭션 커밋 전에 실행될 수 있음
3. **타임스탬프 비교 대신 명시적 버전 관리**: 정확한 버전 추적을 위해 별도 필드 사용

#### 관련 파일

- [WikiDocumentRepository.java](../backend/src/main/java/com/srmanagement/wiki/repository/WikiDocumentRepository.java)
- [WikiDocumentService.java](../backend/src/main/java/com/srmanagement/wiki/service/WikiDocumentService.java)
- [AiSearchService.java](../backend/src/main/java/com/srmanagement/wiki/service/AiSearchService.java)
- [WikiDocumentEmbedding.java](../backend/src/main/java/com/srmanagement/wiki/entity/WikiDocumentEmbedding.java)

---

### TS-P3-2: JSON 필드명 불일치 (`upToDate` vs `isUpToDate`)

**발생일**: 2025-12-20
**심각도**: MEDIUM
**영향 범위**: 프론트엔드 임베딩 상태 표시

#### 증상

- 프론트엔드에서 `isUpToDate`가 항상 `undefined`
- API 응답에서 필드명이 `upToDate`로 반환됨

#### 원인

- Java의 `boolean isUpToDate` 필드
- Lombok `@Data`가 생성하는 getter: `isUpToDate()`
- Jackson이 JavaBean 규약에 따라 `is` 접두사 제거 → JSON에서 `upToDate`로 직렬화

```java
// EmbeddingStatusResponse.java
@Data
public class EmbeddingStatusResponse {
    private boolean isUpToDate;  // Jackson이 "upToDate"로 직렬화
}
```

#### 해결책

```java
// EmbeddingStatusResponse.java
@JsonProperty("isUpToDate")
private boolean isUpToDate;
```

#### 교훈

- Boolean 필드의 `is` 접두사는 Jackson 직렬화 시 제거됨
- `@JsonProperty`로 명시적 필드명 지정 권장
- 프론트엔드와 백엔드 간 필드명 일치 확인 필요

#### 관련 파일

- [EmbeddingStatusResponse.java](../backend/src/main/java/com/srmanagement/wiki/dto/EmbeddingStatusResponse.java)

---

### TS-P3-3: SSE 인증 문제로 폴링 전환

**발생일**: 2025-12-20
**심각도**: MEDIUM
**영향 범위**: 임베딩 진행률 실시간 표시

#### 증상

- SSE(Server-Sent Events) 연결 시 401 Unauthorized 오류
- JWT 토큰이 SSE 요청에 포함되지 않음

#### 원인

- EventSource API는 커스텀 헤더 (Authorization) 지원 안 함
- 브라우저 기본 동작으로 쿠키만 전송
- JWT는 Authorization 헤더로 전송되므로 SSE에서 인증 불가

```javascript
// EventSource는 커스텀 헤더 불가
const eventSource = new EventSource('/api/wiki/search/embeddings/progress');
// Authorization 헤더를 추가할 방법 없음
```

#### 해결책

SSE 대신 폴링 방식으로 전환

```typescript
// aiSearchService.ts
subscribeProgress(
  documentId: number,
  onProgress: (event: EmbeddingProgressEvent) => void,
  onComplete?: () => void,
  onError?: (error: Error) => void
): () => void {
  let intervalId = setInterval(async () => {
    try {
      const progress = await this.getCurrentProgress(documentId);
      if (progress) {
        onProgress(progress);
        if (progress.status === 'COMPLETED' || progress.status === 'FAILED') {
          clearInterval(intervalId);
          if (onComplete) onComplete();
        }
      }
    } catch (error) {
      clearInterval(intervalId);
      if (onError) onError(error as Error);
    }
  }, 1000);  // 1초 간격

  return () => clearInterval(intervalId);  // cleanup 함수 반환
}
```

#### 대안 검토

| 방식 | 장점 | 단점 |
|------|------|------|
| SSE | 서버 푸시, 실시간 | 인증 헤더 불가 |
| 폴링 | 인증 쉬움, 구현 간단 | 서버 부하 |
| WebSocket | 양방향, 인증 가능 | 구현 복잡도 |

#### 교훈

- SSE는 인증이 필요한 API에서 제한적
- 폴링 방식이 더 안정적 (인터벌 조절 가능)
- WebSocket도 대안이지만 구현 복잡도 증가
- 향후 WebSocket 전환 시 [Spring WebSocket + STOMP](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket) 고려

#### 관련 파일

- [aiSearchService.ts](../frontend/src/services/aiSearchService.ts)
- [WikiPage.tsx](../frontend/src/pages/WikiPage.tsx)

---

## 문서 관리

### 이 문서에 새 이슈 추가하기

1. 목차에 링크 추가
2. 아래 템플릿 사용:

```markdown
### TS-PN-N: [이슈 제목]

**발생일**: YYYY-MM-DD
**심각도**: HIGH/MEDIUM/LOW
**영향 범위**: [영향받는 기능]

#### 증상
- [증상 1]
- [증상 2]

#### 원인
[원인 설명]

#### 해결책
[코드 예시 포함]

#### 교훈
[배운 점]

#### 관련 파일
- [파일명](파일경로)
```

### 이슈 ID 규칙

- `TS-P1-N`: Phase 1 (코어 위키) 관련
- `TS-P2-N`: Phase 2 (PDF 변환/뷰어) 관련
- `TS-P3-N`: Phase 3 (AI 검색) 관련
- `TS-P4-N`: Phase 4 (고급 기능) 관련

---

## 참고 자료

- [HISTORY_20251219.md](HISTORY_20251219.md) - Phase 2 PDF 뷰어 개발 이력
- [HISTORY_20251219_WIKI_PHASE2.md](HISTORY_20251219_WIKI_PHASE2.md) - Phase 2 PDF 변환 개발 이력
- [HISTORY_20251220_WIKI_PHASE3.md](HISTORY_20251220_WIKI_PHASE3.md) - Phase 3 AI 검색 개발 이력
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Ollama API Reference](https://github.com/ollama/ollama/blob/main/docs/api.md)
- [react-pdf Documentation](https://github.com/wojtekmaj/react-pdf)
