# Wiki Phase 3: AI 검색 기능 개발 이력

**작성일**: 2025-12-20
**상태**: 완료
**개발자**: Claude AI Assistant

---

## 1. 개요

### 1.1 목표
- RAG(Retrieval-Augmented Generation) 기반 자연어 검색 기능 구현
- 폐쇄망 환경에서 Ollama 서버를 활용한 AI 검색
- 문서 임베딩 자동 생성 및 실시간 진행률 표시

### 1.2 주요 기능
- Spring AI + Ollama 통합
- 문서 청킹 및 임베딩 생성
- 코사인 유사도 기반 문서 검색
- 비동기 임베딩 생성 및 진행률 폴링
- AI 답변 생성 및 참고 문서 표시

---

## 2. 구현 상세

### 2.1 Backend 구현

#### 신규 파일
| 파일 | 설명 |
|------|------|
| `AiSearchService.java` | RAG 기반 AI 검색 서비스 |
| `WikiSearchController.java` | AI 검색 API 엔드포인트 |
| `AiSearchRequest.java` | 검색 요청 DTO |
| `AiSearchResponse.java` | 검색 응답 DTO (답변 + 참고 문서) |
| `WikiDocumentEmbedding.java` | 임베딩 엔티티 |
| `WikiDocumentEmbeddingRepository.java` | 임베딩 Repository |
| `EmbeddingStatusResponse.java` | 임베딩 상태 응답 DTO |
| `EmbeddingProgressEvent.java` | 임베딩 진행률 이벤트 DTO |
| `EmbeddingProgressService.java` | 진행률 관리 서비스 (SSE/폴링) |
| `AsyncConfig.java` | 비동기 처리 Thread Pool 설정 |

#### 수정 파일
| 파일 | 변경 내용 |
|------|-----------|
| `WikiDocumentService.java` | 문서 저장 시 자동 임베딩 생성 호출 |
| `WikiDocumentRepository.java` | 조회수 증가 쿼리 추가 (updatedAt 미변경) |
| `application.yml` | Spring AI Ollama 설정 추가 |
| `pom.xml` | Spring AI 의존성 추가 |

### 2.2 Frontend 구현

#### 신규 파일
| 파일 | 설명 |
|------|------|
| `AiSearchBox.tsx` | AI 검색 컴포넌트 |
| `aiSearchService.ts` | AI 검색 API 클라이언트 |
| `aiSearch.ts` | AI 검색 타입 정의 |

#### 수정 파일
| 파일 | 변경 내용 |
|------|-----------|
| `WikiPage.tsx` | 임베딩 상태/진행률 UI, 임베딩 생성 버튼 |
| `WikiPage.css` | 스피너 애니메이션 추가 |

---

## 3. 기술 아키텍처

### 3.1 RAG 처리 흐름

```
[사용자 질문] "API 호출 방법은?"
       ↓
[1] 질문 임베딩 생성 (Ollama snowflake-arctic-embed:110m)
       ↓
[2] 코사인 유사도 검색 (H2 Database)
    - 모든 문서 청크 임베딩과 비교
    - 유사도 Top-K 추출 (기본 3개)
    - 유사도 임계값 필터링 (기본 0.3)
       ↓
[3] 컨텍스트 생성
    - 검색된 문서 청크 결합
    - 프롬프트 템플릿 적용
       ↓
[4] LLM 답변 생성 (Ollama gpt-oss:20b)
       ↓
[5] 응답 반환 (AI 답변 + 참고 문서 목록)
```

### 3.2 문서 청킹 전략

```java
// 청킹 파라미터
MAX_CHUNK_LENGTH = 2000  // 청크 최대 길이 (문자)
OVERLAP_LENGTH = 200     // 청크 간 겹치는 영역

// 청킹 로직
1. 문서를 MAX_CHUNK_LENGTH 단위로 분할
2. 문장 중간 끊김 방지 (줄바꿈/마침표 위치 조정)
3. 청크 간 OVERLAP_LENGTH 만큼 겹침 (문맥 유지)
```

### 3.3 임베딩 저장 구조

```sql
CREATE TABLE wiki_document_embedding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,           -- 원본 문서 ID
    content TEXT NOT NULL,                  -- 청크 텍스트
    embedding_vector TEXT NOT NULL,         -- JSON 배열 형태 벡터
    chunk_index INT NOT NULL,               -- 청크 순서
    document_title VARCHAR(200),            -- 검색 결과 표시용
    category_id BIGINT,                     -- 카테고리 필터링용
    category_name VARCHAR(100),
    source_document_updated_at TIMESTAMP,   -- 임베딩 최신 여부 비교용
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

---

## 4. API 엔드포인트

### 4.1 AI 검색

```
POST /api/wiki/search/ai
Request:
{
  "question": "API 호출 방법은?",
  "topK": 3,
  "categoryId": null,
  "similarityThreshold": 0.3
}

Response:
{
  "answer": "API 호출 방법은...",
  "sources": [
    {
      "documentId": 1,
      "title": "API 가이드",
      "categoryName": "기술문서",
      "snippet": "...관련 내용...",
      "relevanceScore": 0.85
    }
  ],
  "processingTimeMs": 2500
}
```

### 4.2 임베딩 관리

```
# 임베딩 상태 조회
GET /api/wiki/search/embeddings/status/{documentId}
Response:
{
  "documentId": 1,
  "hasEmbedding": true,
  "chunkCount": 5,
  "lastEmbeddingDate": "2025-12-20T15:00:00",
  "documentUpdatedAt": "2025-12-20T14:30:00",
  "isUpToDate": true
}

# 수동 임베딩 생성 (동기)
POST /api/wiki/search/embeddings/{documentId}

# 비동기 임베딩 생성
POST /api/wiki/search/embeddings/async/{documentId}

# 진행률 조회 (폴링)
GET /api/wiki/search/embeddings/progress/current/{documentId}
Response:
{
  "documentId": 1,
  "documentTitle": "문서 제목",
  "status": "IN_PROGRESS",
  "currentChunk": 3,
  "totalChunks": 10,
  "progressPercent": 30,
  "elapsedTimeMs": 5000,
  "estimatedRemainingMs": 15000
}
```

---

## 5. 설정

### 5.1 Spring AI 설정 (application.yml)

```yaml
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
        model: snowflake-arctic-embed:110m
        options:
          num-ctx: 512
```

### 5.2 비동기 처리 설정

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "embeddingTaskExecutor")
    public Executor embeddingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("Embedding-");
        executor.initialize();
        return executor;
    }
}
```

---

## 6. 트러블슈팅 (중요)

### TS-1: 임베딩 상태 불일치 - `isUpToDate` 항상 false

#### 증상
- 임베딩 생성 완료 후에도 "임베딩 재생성 필요" 상태 표시
- 다른 문서로 이동 후 돌아오면 상태가 바뀜

#### 근본 원인
**문제 1: 조회수 증가 시 updatedAt 변경**
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
1. **@UpdateTimestamp 사용 시 주의**: 모든 save() 호출에서 갱신됨
2. **비동기 메서드와 트랜잭션**: 호출자 트랜잭션 커밋 전에 실행될 수 있음
3. **타임스탬프 비교 대신 명시적 버전 관리**: 정확한 버전 추적을 위해 별도 필드 사용

---

### TS-2: JSON 필드명 불일치 (`upToDate` vs `isUpToDate`)

#### 증상
- 프론트엔드에서 `isUpToDate`가 항상 `undefined`

#### 원인
- Java의 `boolean isUpToDate` 필드
- Lombok `@Data`가 생성하는 getter: `isUpToDate()`
- Jackson이 JavaBean 규약에 따라 `is` 접두사 제거 → JSON에서 `upToDate`로 직렬화

#### 해결책
```java
// EmbeddingStatusResponse.java
@JsonProperty("isUpToDate")
private boolean isUpToDate;
```

#### 교훈
- Boolean 필드의 `is` 접두사는 Jackson 직렬화에 주의 필요
- `@JsonProperty`로 명시적 필드명 지정 권장

---

### TS-3: SSE 인증 문제로 폴링 전환

#### 증상
- SSE(Server-Sent Events) 연결 시 401 Unauthorized 오류
- JWT 토큰이 SSE 요청에 포함되지 않음

#### 원인
- EventSource API는 커스텀 헤더 (Authorization) 지원 안 함
- 브라우저 기본 동작으로 쿠키만 전송

#### 해결책
- SSE 대신 폴링 방식으로 전환
- 1초 간격으로 진행률 API 호출

```typescript
// aiSearchService.ts
subscribeProgress(
  documentId: number,
  onProgress: (event: EmbeddingProgressEvent) => void,
  onComplete?: () => void,
  onError?: (error: Error) => void
): () => void {
  let intervalId = setInterval(async () => {
    const progress = await this.getCurrentProgress(documentId);
    if (progress) {
      onProgress(progress);
      if (progress.status === 'COMPLETED' || progress.status === 'FAILED') {
        clearInterval(intervalId);
      }
    }
  }, 1000);
  return () => clearInterval(intervalId);
}
```

#### 교훈
- SSE는 인증이 필요한 API에서 제한적
- 폴링 방식이 더 안정적 (인터벌 조절 가능)
- 웹소켓도 대안이지만 구현 복잡도 증가

---

## 7. 테스트

### 7.1 기능 테스트
- [x] 문서 저장 시 자동 임베딩 생성
- [x] 수동 임베딩 생성 버튼
- [x] 임베딩 진행률 실시간 표시
- [x] AI 검색 질의응답
- [x] 참고 문서 링크 표시
- [x] 다른 문서 이동 후 복귀 시 상태 유지

### 7.2 성능 테스트
- 임베딩 생성: 청크당 약 200-500ms (Ollama 서버 상태에 따라 변동)
- AI 검색 응답: 2-5초 (문서 수, 질문 복잡도에 따라 변동)

---

## 8. 향후 개선 사항

### 8.1 단기
- [ ] 벡터 검색 성능 최적화 (인덱싱)
- [ ] 캐싱 적용 (자주 묻는 질문)
- [ ] 에러 핸들링 강화

### 8.2 중장기
- [ ] 배치 임베딩 생성 (전체 문서)
- [ ] 임베딩 갱신 스케줄러
- [ ] 멀티 모델 지원 (Chat/Embedding 분리)

---

## 9. 참고 자료

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Ollama API Reference](https://github.com/ollama/ollama/blob/main/docs/api.md)
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - 상세 트러블슈팅 가이드
