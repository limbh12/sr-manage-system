package com.srmanagement.wiki.controller;

import com.srmanagement.wiki.dto.AiSearchRequest;
import com.srmanagement.wiki.dto.AiSearchResponse;
import com.srmanagement.wiki.dto.EmbeddingProgressEvent;
import com.srmanagement.wiki.dto.EmbeddingStatusResponse;
import com.srmanagement.wiki.service.AiSearchService;
import com.srmanagement.wiki.service.EmbeddingProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Wiki AI 검색 컨트롤러
 */
@RestController
@RequestMapping("/api/wiki/search")
@RequiredArgsConstructor
@Slf4j
public class WikiSearchController {

    private final AiSearchService aiSearchService;
    private final EmbeddingProgressService progressService;

    /**
     * AI 기반 자연어 검색 (RAG)
     *
     * @param request 검색 요청 (질문, topK, 카테고리 필터 등)
     * @return AI 답변 및 참고 문서 목록
     */
    @PostMapping("/ai")
    public ResponseEntity<AiSearchResponse> aiSearch(@Valid @RequestBody AiSearchRequest request) {
        log.info("AI 검색 요청: {}", request.getQuestion());
        AiSearchResponse response = aiSearchService.search(request);
        log.info("AI 답변 생성 완료: {} sources, {}ms",
                response.getSources().size(),
                response.getProcessingTimeMs());
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
}
