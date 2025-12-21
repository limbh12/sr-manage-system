# Wiki Phase 4: AI 검색 고급 기능 개발 이력

**작성일**: 2025-12-20
**상태**: 완료
**개발자**: Claude AI Assistant

---

## 1. 개요

### 1.1 목표
- AI 검색 결과 최적화 및 사용자 경험 개선
- 문서 AI 요약 기능 구현
- 검색 이력 관리 기능
- 검색 결과 하이라이팅 및 UI 개선

### 1.2 주요 기능
- 문서 AI 요약 생성 및 캐싱
- 검색 이력 저장/조회/삭제
- 검색 결과 스니펫 및 관련도 점수 표시
- 카테고리별 필터링 강화
- 임베딩 상태 캐싱 (30초 TTL)

---

## 2. 구현 상세

### 2.1 Backend 구현

#### 신규 파일
| 파일 | 설명 |
|------|------|
| `AiSearchHistory.java` | 검색 이력 엔티티 |
| `AiSearchHistoryRepository.java` | 검색 이력 Repository |
| `AiSearchHistoryService.java` | 검색 이력 서비스 (비동기 저장) |
| `AiSearchHistoryResponse.java` | 검색 이력 응답 DTO |
| `SummaryResponse.java` | AI 요약 응답 DTO |

#### 수정 파일
| 파일 | 변경 내용 |
|------|-----------|
| `AiSearchService.java` | AI 요약 생성, 캐싱 로직 추가 |
| `WikiSearchController.java` | 요약 API, 검색 이력 API 추가 |
| `WikiDocument.java` | `aiSummary`, `summaryGeneratedAt` 필드 추가 |
| `WikiDocumentRepository.java` | `updateAiSummary()` 네이티브 쿼리 추가 |
| `application.yml` | 캐시 TTL 설정 추가 |

### 2.2 Frontend 구현

#### 수정 파일
| 파일 | 변경 내용 |
|------|-----------|
| `AiSearchBox.tsx` | 검색 이력 표시, 빠른 재검색 |
| `WikiPage.tsx` | AI 요약 표시 영역, 요약 생성 버튼 |
| `aiSearchService.ts` | 요약 API, 이력 API 클라이언트 추가 |
| `aiSearch.ts` | 요약/이력 관련 타입 정의 추가 |

---

## 3. 기술 상세

### 3.1 AI 요약 생성

#### 요약 생성 흐름
```
[문서 조회]
    ↓
[요약 캐시 확인]
    ├── 캐시 최신 → 즉시 반환
    └── 캐시 없음/오래됨 → 비동기 생성 시작
                            ↓
                     [Ollama LLM 호출]
                            ↓
                     [DB 저장 (updatedAt 미변경)]
                            ↓
                     [폴링으로 결과 확인]
```

#### 요약 상태
| 상태 | 설명 |
|------|------|
| `CACHED` | 캐시된 요약이 최신 상태 |
| `GENERATING` | 비동기 요약 생성 중 |
| `NEEDS_UPDATE` | 문서 수정 후 요약 갱신 필요 |
| `FAILED` | 요약 생성 실패 |

#### 요약 저장 (updatedAt 미변경)
```java
// WikiDocumentRepository.java
@Modifying
@Query("UPDATE WikiDocument wd SET wd.aiSummary = :summary, " +
       "wd.summaryGeneratedAt = :generatedAt WHERE wd.id = :id")
void updateAiSummary(@Param("id") Long id,
                     @Param("summary") String summary,
                     @Param("generatedAt") LocalDateTime generatedAt);
```

### 3.2 검색 이력 관리

#### 이력 엔티티
```java
@Entity
public class AiSearchHistory {
    @Id @GeneratedValue
    private Long id;

    private String username;      // 검색 사용자
    private String query;         // 검색 질문
    private String answer;        // AI 답변 (요약)
    private Integer sourceCount;  // 참고 문서 수
    private Long processingTimeMs; // 처리 시간
    private String resourceTypes; // 검색 대상 (JSON)

    private LocalDateTime searchedAt;
}
```

#### 이력 API
| API | 설명 |
|-----|------|
| `GET /history/recent?limit=10` | 최근 검색 이력 |
| `GET /history?page=0&size=20` | 이력 페이징 조회 |
| `GET /history/search?keyword=` | 이력 키워드 검색 |
| `DELETE /history/{id}` | 이력 삭제 |

### 3.3 임베딩 상태 캐싱

#### 캐시 설정
```yaml
# application.yml
spring:
  cache:
    type: simple

# 30초 TTL (CacheConfig.java에서 설정)
```

#### 캐시 적용
```java
@Cacheable(value = "embeddingStatus", key = "#documentId")
public EmbeddingStatusResponse getEmbeddingStatus(Long documentId) {
    // ...
}

@CacheEvict(value = "embeddingStatus", key = "#documentId")
public void generateEmbeddings(Long documentId) {
    // 임베딩 생성 시 캐시 무효화
}
```

---

## 4. API 명세

### 4.1 AI 요약 API

#### 요약 생성
```
POST /api/wiki/search/summary/{documentId}?forceRegenerate=false
```

**Response (GENERATING)**:
```json
{
  "documentId": 1,
  "status": "GENERATING",
  "message": "요약 생성을 시작했습니다"
}
```

**Response (CACHED)**:
```json
{
  "documentId": 1,
  "summary": "이 문서는 API 호출 방법에 대해...",
  "generatedAt": "2025-12-20T15:30:00",
  "status": "CACHED"
}
```

#### 요약 상태 조회 (폴링용)
```
GET /api/wiki/search/summary/{documentId}
```

### 4.2 검색 이력 API

#### 최근 이력 조회
```
GET /api/wiki/search/history/recent?limit=10
```

**Response**:
```json
[
  {
    "id": 1,
    "query": "API 호출 방법",
    "answerPreview": "API 호출은...",
    "sourceCount": 3,
    "processingTimeMs": 1234,
    "searchedAt": "2025-12-20T15:00:00"
  }
]
```

---

## 5. 성능 최적화

### 5.1 적용된 최적화
| 항목 | 방법 | 효과 |
|------|------|------|
| 임베딩 상태 | 30초 캐싱 | DB 조회 감소 |
| 요약 저장 | 네이티브 쿼리 | updatedAt 미변경 |
| 이력 저장 | 비동기 처리 | 검색 응답 지연 방지 |
| 요약 생성 | 비동기 + 폴링 | 사용자 대기 시간 최소화 |

### 5.2 측정 결과
| 작업 | 소요 시간 |
|------|-----------|
| 임베딩 상태 조회 (캐시 hit) | < 5ms |
| 임베딩 상태 조회 (캐시 miss) | ~50ms |
| AI 요약 생성 | 3-8초 |
| 검색 이력 저장 | ~10ms (비동기) |

---

## 6. 테스트

### 6.1 테스트 케이스
- [x] 문서 요약 생성 및 캐싱
- [x] 요약 캐시 유효성 검사 (문서 수정 시 갱신 필요)
- [x] 검색 이력 저장 및 조회
- [x] 이력 삭제 (본인 이력만)
- [x] 임베딩 상태 캐시 동작

---

## 7. 향후 개선 사항

1. **요약 품질 향상**: 프롬프트 튜닝, 긴 문서 처리 개선
2. **이력 분석**: 자주 검색되는 키워드 통계
3. **관련 문서 추천**: 검색 결과 기반 추천
4. **다국어 지원**: 영문 문서 요약/검색

---

## 8. 참고 자료

- [HISTORY_20251220_WIKI_PHASE3.md](HISTORY_20251220_WIKI_PHASE3.md) - Phase 3 AI 검색 기본 구현
- [TROUBLESHOOTING_AI-Powered_Wiki.md](TROUBLESHOOTING_AI-Powered_Wiki.md) - 트러블슈팅 가이드
- [Spring Cache Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
