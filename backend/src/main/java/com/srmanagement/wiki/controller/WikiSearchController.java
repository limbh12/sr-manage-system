package com.srmanagement.wiki.controller;

import com.srmanagement.wiki.dto.AiSearchHistoryResponse;
import com.srmanagement.wiki.dto.AiSearchRequest;
import com.srmanagement.wiki.dto.AiSearchResponse;
import com.srmanagement.wiki.dto.BulkEmbeddingProgressEvent;
import com.srmanagement.wiki.dto.EmbeddingProgressEvent;
import com.srmanagement.wiki.dto.EmbeddingStatusResponse;
import com.srmanagement.wiki.dto.SummaryResponse;
import com.srmanagement.wiki.service.AiSearchHistoryService;
import com.srmanagement.wiki.service.AiSearchService;
import com.srmanagement.wiki.service.BulkEmbeddingProgressService;
import com.srmanagement.wiki.service.ContentEmbeddingService;
import com.srmanagement.wiki.service.EmbeddingProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * Wiki AI 검색 컨트롤러
 */
@RestController
@RequestMapping("/api/wiki/search")
@RequiredArgsConstructor
@Slf4j
public class WikiSearchController {

    private final AiSearchService aiSearchService;
    private final AiSearchHistoryService historyService;
    private final ContentEmbeddingService contentEmbeddingService;
    private final EmbeddingProgressService progressService;
    private final BulkEmbeddingProgressService bulkProgressService;

    /**
     * AI 기반 자연어 검색 (RAG)
     *
     * @param request 검색 요청 (질문, topK, 카테고리 필터 등)
     * @param authentication 인증 정보
     * @return AI 답변 및 참고 문서 목록
     */
    @PostMapping("/ai")
    public ResponseEntity<AiSearchResponse> aiSearch(
            @Valid @RequestBody AiSearchRequest request,
            Authentication authentication) {
        log.info("AI 검색 요청: {}", request.getQuestion());
        AiSearchResponse response = aiSearchService.search(request);
        log.info("AI 답변 생성 완료: {} sources, {}ms",
                response.getSources().size(),
                response.getProcessingTimeMs());

        // 검색 이력 비동기 저장
        if (authentication != null) {
            String username = authentication.getName();
            historyService.saveSearchHistoryAsync(username, request.getQuestion(), response, request.getResourceTypes());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 문서 임베딩 생성 (수동 트리거용)
     *
     * @param documentId Wiki 문서 ID
     * @return 성공 메시지
     */
    @PostMapping("/embeddings/{documentId}")
    public ResponseEntity<String> generateEmbeddings(@PathVariable Long documentId) {
        log.info("문서 임베딩 생성 요청: documentId={}", documentId);
        aiSearchService.generateEmbeddings(documentId);
        log.info("문서 임베딩 생성 완료: documentId={}", documentId);
        return ResponseEntity.ok("임베딩 생성 완료");
    }

    /**
     * 문서 임베딩 상태 조회
     *
     * @param documentId Wiki 문서 ID
     * @return 임베딩 상태 정보 (존재 여부, 청크 개수, 최신 여부 등)
     */
    @GetMapping("/embeddings/status/{documentId}")
    public ResponseEntity<EmbeddingStatusResponse> getEmbeddingStatus(@PathVariable Long documentId) {
        log.debug("문서 임베딩 상태 조회: documentId={}", documentId);
        EmbeddingStatusResponse status = aiSearchService.getEmbeddingStatus(documentId);
        return ResponseEntity.ok(status);
    }

    /**
     * 비동기 임베딩 생성 (진행률 SSE 전송)
     *
     * @param documentId Wiki 문서 ID
     * @return 비동기 작업 시작 확인 메시지
     */
    @PostMapping("/embeddings/async/{documentId}")
    public ResponseEntity<String> generateEmbeddingsAsync(@PathVariable Long documentId) {
        // 이미 진행 중인지 확인
        if (progressService.isInProgress(documentId)) {
            log.warn("이미 임베딩 생성 중: documentId={}", documentId);
            return ResponseEntity.badRequest().body("이미 임베딩 생성이 진행 중입니다");
        }

        log.info("비동기 임베딩 생성 요청: documentId={}", documentId);
        aiSearchService.generateEmbeddingsAsync(documentId);
        return ResponseEntity.ok("임베딩 생성이 시작되었습니다");
    }

    /**
     * 임베딩 진행률 SSE 스트림 구독
     *
     * @param documentId Wiki 문서 ID
     * @return SSE Emitter (실시간 진행률 전송)
     */
    @GetMapping(value = "/embeddings/progress/{documentId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeProgress(@PathVariable Long documentId) {
        log.info("임베딩 진행률 SSE 구독: documentId={}", documentId);
        return progressService.subscribe(documentId);
    }

    /**
     * 현재 임베딩 진행 상태 조회 (폴링용)
     *
     * @param documentId Wiki 문서 ID
     * @return 현재 진행 상태 (진행 중이 아니면 null)
     */
    @GetMapping("/embeddings/progress/current/{documentId}")
    public ResponseEntity<EmbeddingProgressEvent> getCurrentProgress(@PathVariable Long documentId) {
        EmbeddingProgressEvent progress = progressService.getCurrentProgress(documentId);
        if (progress == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(progress);
    }

    /**
     * 문서 AI 요약 생성
     * - 캐시된 요약이 최신이면 바로 반환
     * - 아니면 새로 생성
     *
     * @param documentId Wiki 문서 ID
     * @param forceRegenerate 강제 재생성 여부 (기본값: false)
     * @return AI 요약 응답
     */
    @PostMapping("/summary/{documentId}")
    public ResponseEntity<SummaryResponse> generateSummary(
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "false") boolean forceRegenerate) {
        log.info("문서 요약 생성 요청: documentId={}, forceRegenerate={}", documentId, forceRegenerate);
        SummaryResponse response = aiSearchService.generateSummary(documentId, forceRegenerate);
        return ResponseEntity.ok(response);
    }

    /**
     * 문서 AI 요약 상태 조회 (폴링용)
     * - 생성 중이면 GENERATING 상태 반환
     * - 캐시가 있으면 CACHED 상태와 요약 반환
     * - 없으면 NEEDS_UPDATE 상태 반환
     *
     * @param documentId Wiki 문서 ID
     * @return AI 요약 상태 및 요약 (있으면)
     */
    @GetMapping("/summary/{documentId}")
    public ResponseEntity<SummaryResponse> getSummaryStatus(@PathVariable Long documentId) {
        log.debug("문서 요약 상태 조회: documentId={}", documentId);
        SummaryResponse response = aiSearchService.getSummaryStatus(documentId);
        return ResponseEntity.ok(response);
    }

    // ==================== 통합 임베딩 API (비동기) ====================

    /**
     * 전체 Wiki 문서 임베딩 생성 (비동기)
     *
     * @return 시작 메시지
     */
    @PostMapping("/embeddings/wiki/all")
    public ResponseEntity<Map<String, Object>> generateAllWikiEmbeddings() {
        // 이미 진행 중인지 확인
        if (bulkProgressService.isInProgress("WIKI")) {
            log.warn("이미 Wiki 임베딩 생성 중");
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "이미 Wiki 임베딩 생성이 진행 중입니다",
                    "status", "IN_PROGRESS"
            ));
        }

        log.info("전체 Wiki 문서 임베딩 생성 시작 (비동기)");
        int totalCount = contentEmbeddingService.getWikiDocumentCount();
        contentEmbeddingService.generateAllWikiEmbeddingsAsync(bulkProgressService);
        return ResponseEntity.ok(Map.of(
                "message", "Wiki 문서 임베딩 생성이 시작되었습니다",
                "totalCount", totalCount,
                "status", "STARTED"
        ));
    }

    /**
     * 전체 SR 임베딩 생성 (비동기)
     *
     * @return 시작 메시지
     */
    @PostMapping("/embeddings/sr/all")
    public ResponseEntity<Map<String, Object>> generateAllSrEmbeddings() {
        // 이미 진행 중인지 확인
        if (bulkProgressService.isInProgress("SR")) {
            log.warn("이미 SR 임베딩 생성 중");
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "이미 SR 임베딩 생성이 진행 중입니다",
                    "status", "IN_PROGRESS"
            ));
        }

        log.info("전체 SR 임베딩 생성 시작 (비동기)");
        int totalCount = contentEmbeddingService.getSrCount();
        contentEmbeddingService.generateAllSrEmbeddingsAsync(bulkProgressService);
        return ResponseEntity.ok(Map.of(
                "message", "SR 임베딩 생성이 시작되었습니다",
                "totalCount", totalCount,
                "status", "STARTED"
        ));
    }

    /**
     * 전체 현황조사 임베딩 생성 (비동기)
     *
     * @return 시작 메시지
     */
    @PostMapping("/embeddings/survey/all")
    public ResponseEntity<Map<String, Object>> generateAllSurveyEmbeddings() {
        // 이미 진행 중인지 확인
        if (bulkProgressService.isInProgress("SURVEY")) {
            log.warn("이미 현황조사 임베딩 생성 중");
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "이미 현황조사 임베딩 생성이 진행 중입니다",
                    "status", "IN_PROGRESS"
            ));
        }

        log.info("전체 현황조사 임베딩 생성 시작 (비동기)");
        int totalCount = contentEmbeddingService.getSurveyCount();
        contentEmbeddingService.generateAllSurveyEmbeddingsAsync(bulkProgressService);
        return ResponseEntity.ok(Map.of(
                "message", "현황조사 임베딩 생성이 시작되었습니다",
                "totalCount", totalCount,
                "status", "STARTED"
        ));
    }

    /**
     * 일괄 임베딩 진행 상태 조회 (폴링용)
     *
     * @param resourceType 리소스 타입 (WIKI, SR, SURVEY)
     * @return 현재 진행 상태
     */
    @GetMapping("/embeddings/bulk/progress/{resourceType}")
    public ResponseEntity<BulkEmbeddingProgressEvent> getBulkProgress(@PathVariable String resourceType) {
        BulkEmbeddingProgressEvent progress = bulkProgressService.getCurrentProgress(resourceType.toUpperCase());
        if (progress == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(progress);
    }

    /**
     * 개별 SR 임베딩 생성
     *
     * @param srId SR ID
     * @return 성공 메시지
     */
    @PostMapping("/embeddings/sr/{srId}")
    public ResponseEntity<String> generateSrEmbedding(@PathVariable Long srId) {
        log.info("SR 임베딩 생성 요청: srId={}", srId);
        contentEmbeddingService.generateSrEmbedding(srId);
        return ResponseEntity.ok("SR 임베딩 생성 완료");
    }

    /**
     * SR 임베딩 상태 조회
     *
     * @param srId SR ID
     * @return 임베딩 상태 정보
     */
    @GetMapping("/embeddings/sr/status/{srId}")
    public ResponseEntity<ContentEmbeddingService.SrEmbeddingStatus> getSrEmbeddingStatus(@PathVariable Long srId) {
        log.debug("SR 임베딩 상태 조회: srId={}", srId);
        ContentEmbeddingService.SrEmbeddingStatus status = contentEmbeddingService.getSrEmbeddingStatus(srId);
        return ResponseEntity.ok(status);
    }

    /**
     * 개별 현황조사 임베딩 생성
     *
     * @param surveyId 현황조사 ID
     * @return 성공 메시지
     */
    @PostMapping("/embeddings/survey/{surveyId}")
    public ResponseEntity<String> generateSurveyEmbedding(@PathVariable Long surveyId) {
        log.info("현황조사 임베딩 생성 요청: surveyId={}", surveyId);
        contentEmbeddingService.generateSurveyEmbedding(surveyId);
        return ResponseEntity.ok("현황조사 임베딩 생성 완료");
    }

    /**
     * 현황조사 임베딩 상태 조회
     *
     * @param surveyId 현황조사 ID
     * @return 임베딩 상태 정보
     */
    @GetMapping("/embeddings/survey/status/{surveyId}")
    public ResponseEntity<ContentEmbeddingService.SurveyEmbeddingStatus> getSurveyEmbeddingStatus(@PathVariable Long surveyId) {
        log.debug("현황조사 임베딩 상태 조회: surveyId={}", surveyId);
        ContentEmbeddingService.SurveyEmbeddingStatus status = contentEmbeddingService.getSurveyEmbeddingStatus(surveyId);
        return ResponseEntity.ok(status);
    }

    /**
     * 통합 임베딩 통계 조회
     *
     * @return 리소스 타입별 임베딩 개수
     */
    @GetMapping("/embeddings/stats")
    public ResponseEntity<Map<String, Object>> getEmbeddingStats() {
        log.debug("통합 임베딩 통계 조회");
        ContentEmbeddingService.EmbeddingStats stats = contentEmbeddingService.getEmbeddingStats();
        return ResponseEntity.ok(Map.of(
                "wiki", stats.wikiCount(),
                "sr", stats.srCount(),
                "survey", stats.surveyCount(),
                "total", stats.getTotal()
        ));
    }

    /**
     * 특정 리소스 타입의 임베딩 전체 삭제
     *
     * @param resourceType 리소스 타입 (WIKI, SR, SURVEY)
     * @return 삭제된 개수
     */
    @DeleteMapping("/embeddings/{resourceType}/all")
    public ResponseEntity<Map<String, Object>> deleteAllEmbeddingsByType(@PathVariable String resourceType) {
        log.info("리소스 타입별 임베딩 전체 삭제 요청: {}", resourceType);
        int deletedCount = contentEmbeddingService.deleteAllByResourceType(resourceType.toUpperCase());
        return ResponseEntity.ok(Map.of(
                "message", resourceType + " 임베딩이 삭제되었습니다",
                "deletedCount", deletedCount
        ));
    }

    // ==================== 검색 이력 API ====================

    /**
     * 내 최근 검색 이력 조회
     *
     * @param limit 조회 개수 (기본값: 10)
     * @param authentication 인증 정보
     * @return 최근 검색 이력 목록
     */
    @GetMapping("/history/recent")
    public ResponseEntity<List<AiSearchHistoryResponse>> getRecentHistory(
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        String username = authentication.getName();
        log.debug("최근 검색 이력 조회: username={}, limit={}", username, limit);
        List<AiSearchHistoryResponse> histories = historyService.getRecentHistory(username, limit);
        return ResponseEntity.ok(histories);
    }

    /**
     * 내 검색 이력 페이징 조회
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @param authentication 인증 정보
     * @return 검색 이력 페이지
     */
    @GetMapping("/history")
    public ResponseEntity<Page<AiSearchHistoryResponse>> getHistoryPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String username = authentication.getName();
        log.debug("검색 이력 페이징 조회: username={}, page={}, size={}", username, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<AiSearchHistoryResponse> histories = historyService.getHistoryPage(username, pageable);
        return ResponseEntity.ok(histories);
    }

    /**
     * 검색 이력 키워드 검색
     *
     * @param keyword 검색 키워드
     * @param limit 조회 개수 (기본값: 10)
     * @param authentication 인증 정보
     * @return 검색된 이력 목록
     */
    @GetMapping("/history/search")
    public ResponseEntity<List<AiSearchHistoryResponse>> searchHistory(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        String username = authentication.getName();
        log.debug("검색 이력 검색: username={}, keyword={}", username, keyword);
        List<AiSearchHistoryResponse> histories = historyService.searchHistory(username, keyword, limit);
        return ResponseEntity.ok(histories);
    }

    /**
     * 검색 이력 삭제
     *
     * @param historyId 이력 ID
     * @param authentication 인증 정보
     * @return 삭제 결과
     */
    @DeleteMapping("/history/{historyId}")
    public ResponseEntity<Map<String, String>> deleteHistory(
            @PathVariable Long historyId,
            Authentication authentication) {
        String username = authentication.getName();
        log.info("검색 이력 삭제 요청: historyId={}, username={}", historyId, username);
        historyService.deleteHistory(historyId, username);
        return ResponseEntity.ok(Map.of("message", "검색 이력이 삭제되었습니다"));
    }
}
