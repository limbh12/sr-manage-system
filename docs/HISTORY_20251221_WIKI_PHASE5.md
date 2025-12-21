# Wiki Phase 5: 통합 임베딩 시스템 개발 이력

**작성일**: 2025-12-21
**상태**: 완료
**개발자**: Claude AI Assistant

---

## 1. 개요

### 1.1 목표
- SR/Survey/Wiki 통합 임베딩 시스템 구축
- 리소스 타입별 개별 및 일괄 임베딩 관리
- 임베딩 상태 모니터링 및 관리 기능 제공
- 일괄 등록 시 성능 최적화

### 1.2 주요 기능
- 통합 `ContentEmbedding` 엔티티로 모든 리소스 타입 관리
- SR/Survey 개별 문서 임베딩 상태 표시 및 생성
- 리소스 타입별 일괄 임베딩 생성 (진행률 표시)
- 임베딩 통계 조회 API
- 리소스 타입별 임베딩 삭제 API

---

## 2. 구현 상세

### 2.1 Backend 구현

#### 신규 파일
| 파일 | 설명 |
|------|------|
| `ContentEmbedding.java` | 통합 임베딩 엔티티 (Wiki/SR/Survey 공통) |
| `ContentEmbeddingRepository.java` | 통합 임베딩 Repository |
| `ContentEmbeddingService.java` | 통합 임베딩 서비스 (개별/일괄 생성) |
| `BulkEmbeddingProgressService.java` | 일괄 임베딩 진행률 관리 서비스 |

#### 수정 파일
| 파일 | 변경 내용 |
|------|-----------|
| `WikiSearchController.java` | 통합 임베딩 API 추가 (상태/생성/삭제/통계) |
| `AiSearchService.java` | 통합 임베딩 기반 검색으로 변경 |
| `SrService.java` | 임베딩 생성 여부 플래그 추가 |
| `OpenApiSurveyService.java` | 일괄등록 시 임베딩 스킵 옵션 전파 |
| `application.yml` | Ollama 타임아웃 설정 추가 |
| `pom.xml` | 의존성 버전 업데이트 |

### 2.2 Frontend 구현

#### 수정 파일
| 파일 | 변경 내용 |
|------|-----------|
| `WikiPage.tsx` | AI 검색 패널에 통합 임베딩 관리 UI 추가 |
| `SurveyDetailModal.tsx` | Survey 개별 임베딩 상태/생성 버튼 |
| `SrDetailModal.tsx` | SR 개별 임베딩 상태/생성 버튼 |
| `aiSearchService.ts` | 통합 임베딩 API 클라이언트 추가 |
| `aiSearch.ts` | 통합 임베딩 관련 타입 정의 |

---

## 3. 기술 상세

### 3.1 통합 임베딩 엔티티

```java
@Entity
@Table(name = "content_embedding")
public class ContentEmbedding {

    public enum ResourceType {
        WIKI, SR, SURVEY
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType resourceType;  // WIKI, SR, SURVEY

    @Column(nullable = false)
    private Long resourceId;  // Wiki ID, SR ID, Survey ID

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;  // 청크 텍스트

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String embeddingVector;  // JSON 배열 형태 벡터

    @Column(nullable = false)
    private Integer chunkIndex;  // 청크 순서

    private String resourceTitle;  // 검색 결과 표시용
    private String categoryName;  // 카테고리 필터링용

    private LocalDateTime sourceUpdatedAt;  // 원본 문서 수정 시점

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

### 3.2 리소스별 콘텐츠 추출

```java
// ContentEmbeddingService.java
private String extractContent(Object resource, ResourceType resourceType) {
    switch (resourceType) {
        case WIKI:
            WikiDocument doc = (WikiDocument) resource;
            return String.format("제목: %s\n카테고리: %s\n내용:\n%s",
                    doc.getTitle(),
                    doc.getCategory() != null ? doc.getCategory().getName() : "미분류",
                    doc.getContent());
        case SR:
            SR sr = (SR) resource;
            return String.format("SR 제목: %s\n상태: %s\n우선순위: %s\n요청내용:\n%s\n처리내용:\n%s",
                    sr.getTitle(),
                    sr.getStatus().name(),
                    sr.getPriority().name(),
                    sr.getDescription() != null ? sr.getDescription() : "",
                    sr.getResolution() != null ? sr.getResolution() : "");
        case SURVEY:
            OpenApiSurvey survey = (OpenApiSurvey) resource;
            return String.format("기관: %s\n시스템: %s\n서비스: %s\n담당자: %s\n호출건수: %s\n비고: %s",
                    survey.getOrganization() != null ? survey.getOrganization().getName() : "",
                    survey.getSystemName() != null ? survey.getSystemName() : "",
                    survey.getServiceName() != null ? survey.getServiceName() : "",
                    survey.getManagerName() != null ? survey.getManagerName() : "",
                    survey.getCallCount() != null ? survey.getCallCount() : "",
                    survey.getNotes() != null ? survey.getNotes() : "");
        default:
            return "";
    }
}
```

### 3.3 일괄 임베딩 생성

#### 비동기 처리 흐름

```
[일괄 임베딩 요청]
       ↓
[1] ID 목록만 먼저 조회 (Lazy Loading 방지)
       ↓
[2] 진행률 초기화 (BulkEmbeddingProgressService)
       ↓
[3] 각 리소스별 개별 트랜잭션에서 처리
    └── self.generateXxxEmbedding(id)  // Self-injection
       ↓
[4] 진행률 업데이트 (폴링으로 조회)
       ↓
[5] 완료 또는 실패 상태 설정
```

#### Self-Injection 패턴

```java
@Service
public class ContentEmbeddingService {

    // Spring AOP 프록시를 통한 호출을 위한 self-injection
    @Autowired
    @Lazy
    private ContentEmbeddingService self;

    @Async("embeddingTaskExecutor")
    public void generateAllSurveyEmbeddingsAsync(BulkEmbeddingProgressService progressService) {
        // 1. ID만 가져오기 (Lazy Loading 방지)
        List<Long> surveyIds = surveyRepository.findAll().stream()
                .map(OpenApiSurvey::getId)
                .toList();

        progressService.startProgress("SURVEY", surveyIds.size());

        for (int i = 0; i < surveyIds.size(); i++) {
            Long surveyId = surveyIds.get(i);
            try {
                // 2. self-injection을 통해 새 트랜잭션에서 처리
                self.generateSurveyEmbedding(surveyId);
                progressService.updateProgress("SURVEY", i + 1, surveyIds.size());
            } catch (Exception e) {
                log.error("Survey 임베딩 생성 실패 (ID: {}): {}", surveyId, e.getMessage());
            }
        }
        progressService.completeProgress("SURVEY");
    }
}
```

### 3.4 일괄등록 시 임베딩 스킵

#### 문제점
- Survey 일괄등록 시 연결된 SR이 자동 생성됨
- SR 생성 시 임베딩도 자동 생성 → Ollama 서버 과부하
- Survey의 `generateEmbedding=false` 플래그가 SR까지 전달되지 않음

#### 해결: 플래그 전파 체인

```java
// SrService.java - 오버로드 메서드 추가
@Transactional
public SrResponse createSr(SrCreateRequest request, String username) {
    return createSr(request, username, true);  // 기본값 true
}

@Transactional
public SrResponse createSr(SrCreateRequest request, String username,
        boolean generateEmbedding) {
    // ... SR 생성 로직 ...

    // 임베딩 생성 여부에 따라 분기
    if (generateEmbedding && contentEmbeddingService != null) {
        final Long srId = savedSr.getId();
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    contentEmbeddingService.generateSrEmbeddingAsync(srId);
                }
            });
    }
    return SrResponse.from(savedSr);
}

// OpenApiSurveyService.java
private void createSrForNewSurvey(OpenApiSurvey survey, User requester,
        boolean generateEmbedding) {
    // ...
    // generateEmbedding 플래그를 SR 생성에 전달
    SrResponse createdSr = srService.createSr(srRequest, requester.getUsername(),
            generateEmbedding);
}
```

---

## 4. API 명세

### 4.1 임베딩 통계 조회

```
GET /api/wiki/search/embeddings/stats

Response:
{
  "total": 25,
  "wiki": 10,
  "sr": 10,
  "survey": 5
}
```

### 4.2 개별 리소스 임베딩 상태

```
GET /api/wiki/search/embeddings/status/{resourceType}/{resourceId}
예: GET /api/wiki/search/embeddings/status/SR/123

Response:
{
  "resourceType": "SR",
  "resourceId": 123,
  "hasEmbedding": true,
  "chunkCount": 3,
  "lastEmbeddingDate": "2025-12-21T10:00:00",
  "sourceUpdatedAt": "2025-12-21T09:30:00",
  "isUpToDate": true
}
```

### 4.3 개별 리소스 임베딩 생성

```
POST /api/wiki/search/embeddings/{resourceType}/{resourceId}
예: POST /api/wiki/search/embeddings/SURVEY/456

Response:
{
  "resourceType": "SURVEY",
  "resourceId": 456,
  "chunkCount": 2,
  "message": "임베딩 생성 완료"
}
```

### 4.4 일괄 임베딩 생성

```
POST /api/wiki/search/embeddings/bulk/{resourceType}
예: POST /api/wiki/search/embeddings/bulk/SR

Response:
{
  "message": "SR 전체 임베딩 생성이 시작되었습니다",
  "resourceType": "SR"
}
```

### 4.5 일괄 임베딩 진행률 조회

```
GET /api/wiki/search/embeddings/bulk/progress/{resourceType}
예: GET /api/wiki/search/embeddings/bulk/progress/SR

Response:
{
  "resourceType": "SR",
  "status": "IN_PROGRESS",
  "current": 5,
  "total": 14,
  "progressPercent": 35,
  "startedAt": "2025-12-21T10:00:00"
}
```

### 4.6 리소스 타입별 임베딩 삭제

```
DELETE /api/wiki/search/embeddings/{resourceType}/all
예: DELETE /api/wiki/search/embeddings/SR/all

Response:
{
  "message": "SR 임베딩이 삭제되었습니다",
  "deletedCount": 14
}
```

---

## 5. 성능 최적화

### 5.1 적용된 최적화

| 항목 | 방법 | 효과 |
|------|------|------|
| Lazy Loading 방지 | ID-first 패턴 | LazyInitializationException 해결 |
| 트랜잭션 격리 | Self-injection | 개별 실패 격리, 전체 롤백 방지 |
| 일괄등록 최적화 | 임베딩 생성 스킵 | Ollama 서버 부하 감소 |
| 비동기 처리 | @Async + Thread Pool | 메인 스레드 블로킹 방지 |
| 진행률 추적 | 인메모리 Map | 실시간 상태 조회 |

### 5.2 Ollama 타임아웃 설정

```yaml
# application.yml
spring:
  ai:
    ollama:
      init:
        timeout: 120s
        pull-model-strategy: never
```

---

## 6. 트러블슈팅

Phase 5에서 발생한 주요 이슈는 [TROUBLESHOOTING_AI-Powered_Wiki.md](TROUBLESHOOTING_AI-Powered_Wiki.md)에 상세히 기록되어 있습니다.

### 6.1 주요 이슈 요약

| 이슈 ID | 제목 | 심각도 | 상태 |
|---------|------|--------|------|
| TS-P5-1 | 현황조사 일괄등록 시 LazyInitializationException | HIGH | 해결 |
| TS-P5-2 | 임베딩 통계에 잘못된 데이터 표시 | MEDIUM | 해결 |
| TS-P5-3 | 현황조사 일괄등록 시 SR 임베딩 자동 생성 | HIGH | 해결 |

---

## 7. 테스트

### 7.1 테스트 케이스
- [x] SR 개별 임베딩 생성 및 상태 표시
- [x] Survey 개별 임베딩 생성 및 상태 표시
- [x] Wiki 개별 임베딩 생성 (기존 기능 유지)
- [x] SR 일괄 임베딩 생성 및 진행률 표시
- [x] Survey 일괄 임베딩 생성 및 진행률 표시
- [x] 임베딩 통계 조회
- [x] 리소스 타입별 임베딩 삭제
- [x] 현황조사 일괄등록 시 임베딩 스킵 확인
- [x] AI 검색 시 통합 임베딩 기반 검색

### 7.2 성능 측정

| 작업 | 소요 시간 |
|------|-----------|
| SR 개별 임베딩 생성 | 1-3초 |
| Survey 개별 임베딩 생성 | 1-2초 |
| SR 14건 일괄 임베딩 | 약 30초 |
| Survey 50건 일괄 임베딩 | 약 1분 30초 |

---

## 8. 향후 개선 사항

1. **임베딩 품질 향상**: 리소스별 맞춤 청킹 전략
2. **자동 갱신**: 원본 수정 시 자동 재임베딩 스케줄러
3. **벡터 DB 도입**: PostgreSQL pgvector 또는 별도 벡터 DB
4. **검색 필터 강화**: 리소스 타입, 날짜 범위, 카테고리 필터
5. **임베딩 버전 관리**: 모델 변경 시 일괄 재생성 지원

---

## 9. 참고 자료

- [HISTORY_20251220_WIKI_PHASE3.md](HISTORY_20251220_WIKI_PHASE3.md) - Phase 3 AI 검색 기본 구현
- [HISTORY_20251220_WIKI_PHASE4.md](HISTORY_20251220_WIKI_PHASE4.md) - Phase 4 AI 검색 고급 기능
- [TROUBLESHOOTING_AI-Powered_Wiki.md](TROUBLESHOOTING_AI-Powered_Wiki.md) - 트러블슈팅 가이드
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Ollama API Reference](https://github.com/ollama/ollama/blob/main/docs/api.md)
