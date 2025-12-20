package com.srmanagement.wiki.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srmanagement.wiki.dto.AiSearchRequest;
import com.srmanagement.wiki.dto.AiSearchResponse;
import com.srmanagement.wiki.dto.EmbeddingProgressEvent;
import com.srmanagement.wiki.entity.WikiDocument;
import com.srmanagement.wiki.entity.WikiDocumentEmbedding;
import com.srmanagement.wiki.repository.WikiDocumentEmbeddingRepository;
import com.srmanagement.wiki.repository.WikiDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 검색 서비스
 * - Wiki 문서 임베딩 생성
 * - RAG 기반 자연어 검색
 * - 코사인 유사도 기반 문서 검색
 *
 * Spring AI (Ollama) 사용
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AiSearchService {

    private final WikiDocumentRepository documentRepository;
    private final WikiDocumentEmbeddingRepository embeddingRepository;
    private final OllamaChatModel chatModel;
    private final OllamaEmbeddingModel embeddingModel;
    private final ObjectMapper objectMapper;
    private final EmbeddingProgressService progressService;

    private static final int MAX_CHUNK_LENGTH = 2000; // 청크 최대 길이 (문자 수)
    private static final int OVERLAP_LENGTH = 200; // 청크 간 겹치는 영역 (문자 수)

    /**
     * Wiki 문서 임베딩 생성
     * - 긴 문서는 청크로 분할하여 각각 임베딩 생성
     *
     * @param documentId Wiki 문서 ID
     */
    @Transactional
    public void generateEmbeddings(Long documentId) {
        long startTime = System.currentTimeMillis();

        // 1. 기존 임베딩 삭제
        embeddingRepository.deleteByDocumentId(documentId);

        // 2. 문서 조회
        WikiDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다: " + documentId));
        log.info("🔍 [문서 {}] 문서 조회 완료 - 제목: {}, 내용 길이: {}자",
                documentId, document.getTitle(),
                document.getContent() != null ? document.getContent().length() : 0);

        // 3. 문서 내용을 청크로 분할
        List<String> chunks = splitIntoChunks(document.getContent());
        int totalChunks = chunks.size();
        log.info("📊 [문서 {}] 임베딩 생성 시작 - 총 {}개 청크로 분할됨", documentId, totalChunks);

        // 4. 각 청크에 대해 임베딩 생성
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            int currentChunk = i + 1;

            try {
                long chunkStartTime = System.currentTimeMillis();

                // 임베딩 벡터 생성 (Spring AI - Ollama Embedding API 호출)
                EmbeddingResponse response = embeddingModel.embedForResponse(List.of(chunk));
                float[] embeddingArray = response.getResults().get(0).getOutput();

                // float[] -> List<Double> 변환
                List<Double> embedding = new ArrayList<>();
                for (float value : embeddingArray) {
                    embedding.add((double) value);
                }

                // JSON 배열로 직렬화
                String embeddingJson = objectMapper.writeValueAsString(embedding);

                // DB에 저장 (sourceDocumentUpdatedAt에 현재 문서의 updatedAt 저장)
                WikiDocumentEmbedding embeddingEntity = WikiDocumentEmbedding.builder()
                        .documentId(documentId)
                        .content(chunk)
                        .embeddingVector(embeddingJson)
                        .chunkIndex(i)
                        .documentTitle(document.getTitle())
                        .categoryId(document.getCategory() != null ? document.getCategory().getId() : null)
                        .categoryName(document.getCategory() != null ? document.getCategory().getName() : null)
                        .sourceDocumentUpdatedAt(document.getUpdatedAt())
                        .build();

                embeddingRepository.save(embeddingEntity);

                long chunkElapsedTime = System.currentTimeMillis() - chunkStartTime;
                int progressPercent = (currentChunk * 100) / totalChunks;
                log.info("⏳ [문서 {}] 진행중 {}/{} ({}%) - 청크 처리 시간: {}ms",
                        documentId, currentChunk, totalChunks, progressPercent, chunkElapsedTime);

            } catch (JsonProcessingException e) {
                log.error("임베딩 벡터 직렬화 실패", e);
                throw new RuntimeException("임베딩 생성 실패", e);
            }
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        double avgTimePerChunk = totalChunks > 0 ? (double) elapsedTime / totalChunks : 0;
        log.info("✅ [문서 {}] 임베딩 생성 완료 - 총 {}개 청크, 전체 소요시간: {}ms, 청크당 평균: {}ms",
                documentId, totalChunks, elapsedTime, String.format("%.1f", avgTimePerChunk));
    }

    /**
     * 비동기 임베딩 생성 (문서 저장 시 자동 호출)
     * - 진행률을 SSE로 실시간 전송
     *
     * @param documentId Wiki 문서 ID
     */
    @Async("embeddingTaskExecutor")
    @Transactional
    public void generateEmbeddingsAsync(Long documentId) {
        long startTime = System.currentTimeMillis();

        try {
            // 호출자의 트랜잭션이 커밋될 때까지 대기 (문서 저장 후 updatedAt 반영)
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("임베딩 생성 대기 중 인터럽트 발생", e);
            }

            // 1. 기존 임베딩 삭제
            embeddingRepository.deleteByDocumentId(documentId);

            // 2. 문서 조회
            WikiDocument document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다: " + documentId));

            String docTitle = document.getTitle();
            log.info("🔍 [문서 {}] 비동기 임베딩 시작 - 제목: {}, 내용 길이: {}자",
                    documentId, docTitle,
                    document.getContent() != null ? document.getContent().length() : 0);

            // 3. 문서 내용을 청크로 분할
            List<String> chunks = splitIntoChunks(document.getContent());
            int totalChunks = chunks.size();

            // 시작 이벤트 전송
            progressService.sendProgress(EmbeddingProgressEvent.builder()
                    .documentId(documentId)
                    .documentTitle(docTitle)
                    .status("STARTED")
                    .currentChunk(0)
                    .totalChunks(totalChunks)
                    .progressPercent(0)
                    .elapsedTimeMs(0)
                    .message("임베딩 생성을 시작합니다")
                    .build());

            log.info("📊 [문서 {}] 임베딩 생성 시작 - 총 {}개 청크로 분할됨", documentId, totalChunks);

            // 4. 각 청크에 대해 임베딩 생성
            long totalChunkTime = 0;
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                int currentChunk = i + 1;

                try {
                    long chunkStartTime = System.currentTimeMillis();

                    // 임베딩 벡터 생성 (Spring AI - Ollama Embedding API 호출)
                    EmbeddingResponse response = embeddingModel.embedForResponse(List.of(chunk));
                    float[] embeddingArray = response.getResults().get(0).getOutput();

                    // float[] -> List<Double> 변환
                    List<Double> embedding = new ArrayList<>();
                    for (float value : embeddingArray) {
                        embedding.add((double) value);
                    }

                    // JSON 배열로 직렬화
                    String embeddingJson = objectMapper.writeValueAsString(embedding);

                    // DB에 저장 (sourceDocumentUpdatedAt에 현재 문서의 updatedAt 저장)
                    WikiDocumentEmbedding embeddingEntity = WikiDocumentEmbedding.builder()
                            .documentId(documentId)
                            .content(chunk)
                            .embeddingVector(embeddingJson)
                            .chunkIndex(i)
                            .documentTitle(docTitle)
                            .categoryId(document.getCategory() != null ? document.getCategory().getId() : null)
                            .categoryName(document.getCategory() != null ? document.getCategory().getName() : null)
                            .sourceDocumentUpdatedAt(document.getUpdatedAt())
                            .build();

                    embeddingRepository.save(embeddingEntity);

                    long chunkElapsedTime = System.currentTimeMillis() - chunkStartTime;
                    totalChunkTime += chunkElapsedTime;
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    int progressPercent = (currentChunk * 100) / totalChunks;

                    // 예상 남은 시간 계산
                    double avgTime = (double) totalChunkTime / currentChunk;
                    long estimatedRemaining = (long) (avgTime * (totalChunks - currentChunk));

                    // 진행률 이벤트 전송
                    progressService.sendProgress(EmbeddingProgressEvent.builder()
                            .documentId(documentId)
                            .documentTitle(docTitle)
                            .status("IN_PROGRESS")
                            .currentChunk(currentChunk)
                            .totalChunks(totalChunks)
                            .progressPercent(progressPercent)
                            .chunkProcessingTimeMs(chunkElapsedTime)
                            .elapsedTimeMs(elapsedTime)
                            .estimatedRemainingMs(estimatedRemaining)
                            .message(String.format("청크 %d/%d 처리 완료", currentChunk, totalChunks))
                            .build());

                    log.info("⏳ [문서 {}] 진행중 {}/{} ({}%) - 청크 처리 시간: {}ms",
                            documentId, currentChunk, totalChunks, progressPercent, chunkElapsedTime);

                } catch (JsonProcessingException e) {
                    log.error("임베딩 벡터 직렬화 실패", e);
                    progressService.sendProgress(EmbeddingProgressEvent.builder()
                            .documentId(documentId)
                            .documentTitle(docTitle)
                            .status("FAILED")
                            .currentChunk(currentChunk)
                            .totalChunks(totalChunks)
                            .message("임베딩 생성 실패: " + e.getMessage())
                            .build());
                    throw new RuntimeException("임베딩 생성 실패", e);
                }
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            double avgTimePerChunk = totalChunks > 0 ? (double) elapsedTime / totalChunks : 0;

            // 완료 이벤트 전송
            progressService.sendProgress(EmbeddingProgressEvent.builder()
                    .documentId(documentId)
                    .documentTitle(docTitle)
                    .status("COMPLETED")
                    .currentChunk(totalChunks)
                    .totalChunks(totalChunks)
                    .progressPercent(100)
                    .elapsedTimeMs(elapsedTime)
                    .estimatedRemainingMs(0)
                    .message(String.format("임베딩 생성 완료 (총 %d개 청크, %.1fms/청크)", totalChunks, avgTimePerChunk))
                    .build());

            log.info("✅ [문서 {}] 비동기 임베딩 완료 - 총 {}개 청크, 전체 소요시간: {}ms, 청크당 평균: {}ms",
                    documentId, totalChunks, elapsedTime, String.format("%.1f", avgTimePerChunk));

        } catch (Exception e) {
            log.error("비동기 임베딩 생성 실패: documentId={}", documentId, e);
            progressService.sendProgress(EmbeddingProgressEvent.builder()
                    .documentId(documentId)
                    .status("FAILED")
                    .message("임베딩 생성 실패: " + e.getMessage())
                    .build());
        }
    }

    /**
     * 문서의 임베딩 상태 조회
     *
     * @param documentId Wiki 문서 ID
     * @return 임베딩 상태 정보
     */
    @Transactional(readOnly = true)
    public com.srmanagement.wiki.dto.EmbeddingStatusResponse getEmbeddingStatus(Long documentId) {
        // 1. 문서 조회
        WikiDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다: " + documentId));

        // 2. 임베딩 개수 조회
        long chunkCount = embeddingRepository.countByDocumentId(documentId);

        // 3. 마지막 임베딩 정보 조회
        java.time.LocalDateTime lastEmbeddingDate = null;
        java.time.LocalDateTime sourceDocumentUpdatedAt = null;
        if (chunkCount > 0) {
            List<WikiDocumentEmbedding> embeddings =
                embeddingRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
            if (!embeddings.isEmpty()) {
                WikiDocumentEmbedding firstEmbedding = embeddings.get(0);
                lastEmbeddingDate = firstEmbedding.getUpdatedAt();
                sourceDocumentUpdatedAt = firstEmbedding.getSourceDocumentUpdatedAt();
            }
        }

        // 4. 임베딩이 최신인지 확인
        // sourceDocumentUpdatedAt이 있으면 이를 사용 (정확한 비교)
        // 없으면 타이밍 허용 오차를 적용 (레거시 데이터 호환)
        boolean isUpToDate = false;
        if (chunkCount > 0 && document.getUpdatedAt() != null) {
            if (sourceDocumentUpdatedAt != null) {
                // 임베딩 생성 시 저장한 문서 버전과 현재 문서 버전 비교
                isUpToDate = sourceDocumentUpdatedAt.isEqual(document.getUpdatedAt()) ||
                            sourceDocumentUpdatedAt.isAfter(document.getUpdatedAt());
            } else if (lastEmbeddingDate != null) {
                // 레거시 데이터: 5초 허용 오차 적용
                java.time.Duration timeDiff = java.time.Duration.between(lastEmbeddingDate, document.getUpdatedAt());
                boolean withinTolerance = Math.abs(timeDiff.toSeconds()) <= 5;
                isUpToDate = lastEmbeddingDate.isAfter(document.getUpdatedAt()) ||
                            lastEmbeddingDate.isEqual(document.getUpdatedAt()) ||
                            withinTolerance;
            }
        }

        return com.srmanagement.wiki.dto.EmbeddingStatusResponse.builder()
                .documentId(documentId)
                .hasEmbedding(chunkCount > 0)
                .chunkCount(chunkCount)
                .lastEmbeddingDate(lastEmbeddingDate)
                .documentUpdatedAt(document.getUpdatedAt())
                .isUpToDate(isUpToDate)
                .build();
    }

    /**
     * RAG 기반 자연어 검색
     *
     * @param request 검색 요청
     * @return AI 답변 및 참고 문서
     */
    @Transactional(readOnly = true)
    public AiSearchResponse search(AiSearchRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 사용자 질문 임베딩 생성 (Spring AI)
            EmbeddingResponse queryEmbeddingResponse = embeddingModel.embedForResponse(List.of(request.getQuestion()));
            float[] queryEmbeddingArray = queryEmbeddingResponse.getResults().get(0).getOutput();

            // float[] -> List<Double> 변환
            List<Double> queryEmbedding = new ArrayList<>();
            for (float value : queryEmbeddingArray) {
                queryEmbedding.add((double) value);
            }

            log.debug("질문 임베딩 생성 완료: {}차원", queryEmbedding.size());

            // 2. 모든 문서 임베딩 조회
            List<WikiDocumentEmbedding> allEmbeddings = embeddingRepository.findAllForSearch();
            log.debug("전체 임베딩 개수: {}", allEmbeddings.size());

            // 카테고리 필터링 (선택적)
            if (request.getCategoryId() != null) {
                allEmbeddings = allEmbeddings.stream()
                        .filter(e -> request.getCategoryId().equals(e.getCategoryId()))
                        .collect(Collectors.toList());
                log.debug("카테고리 필터 적용 후: {} 개", allEmbeddings.size());
            }

            // 3. 코사인 유사도 계산 및 정렬
            List<ScoredEmbedding> scoredEmbeddings = allEmbeddings.stream()
                    .map(embedding -> {
                        try {
                            List<Double> docEmbedding = objectMapper.readValue(
                                    embedding.getEmbeddingVector(),
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, Double.class)
                            );
                            double similarity = cosineSimilarity(queryEmbedding, docEmbedding);
                            return new ScoredEmbedding(embedding, similarity);
                        } catch (JsonProcessingException e) {
                            log.error("임베딩 역직렬화 실패", e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .filter(se -> se.score >= request.getSimilarityThreshold())
                    .sorted(Comparator.comparingDouble(ScoredEmbedding::getScore).reversed())
                    .limit(request.getTopK())
                    .collect(Collectors.toList());

            log.info("유사도 Top-{}: {}", request.getTopK(), scoredEmbeddings.stream()
                    .map(se -> String.format("%.3f", se.score))
                    .collect(Collectors.joining(", ")));

            // 4. 참고 문서 컨텍스트 생성
            StringBuilder contextBuilder = new StringBuilder();
            List<AiSearchResponse.SourceDocument> sources = new ArrayList<>();

            for (ScoredEmbedding scored : scoredEmbeddings) {
                WikiDocumentEmbedding embedding = scored.embedding;
                contextBuilder.append("## ").append(embedding.getDocumentTitle()).append("\n");
                contextBuilder.append(embedding.getContent()).append("\n\n");

                // 중복 제거 (같은 문서의 다른 청크)
                boolean exists = sources.stream()
                        .anyMatch(s -> s.getDocumentId().equals(embedding.getDocumentId()));

                if (!exists) {
                    sources.add(AiSearchResponse.SourceDocument.builder()
                            .documentId(embedding.getDocumentId())
                            .title(embedding.getDocumentTitle())
                            .categoryName(embedding.getCategoryName())
                            .snippet(truncate(embedding.getContent(), 200))
                            .relevanceScore(scored.score)
                            .build());
                }
            }

            // 5. LLM에 프롬프트 전송 (Spring AI)
            String promptText = buildPrompt(request.getQuestion(), contextBuilder.toString());
            Prompt prompt = new Prompt(promptText);
            ChatResponse chatResponse = chatModel.call(prompt);
            String answer = chatResponse.getResult().getOutput().getContent();

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("검색 완료: {}ms, {} sources", elapsedTime, sources.size());

            return AiSearchResponse.builder()
                    .answer(answer)
                    .sources(sources)
                    .processingTimeMs(elapsedTime)
                    .build();

        } catch (Exception e) {
            log.error("AI 검색 실패", e);
            throw new RuntimeException("AI 검색 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * Prompt Template 생성
     */
    private String buildPrompt(String question, String context) {
        return String.format("""
                당신은 SR 관리 시스템의 기술 문서 도우미입니다.
                아래 문서들을 참고하여 사용자의 질문에 정확하고 친절하게 답변해주세요.

                **참고 문서:**
                %s

                **사용자 질문:**
                %s

                **답변 작성 지침:**
                1. 참고 문서의 내용을 기반으로 답변하세요
                2. 문서에 없는 내용은 추측하지 마세요
                3. 마크다운 형식으로 답변하세요 (제목, 리스트, 코드 블록 등 활용)
                4. 답변은 한국어로 작성하세요
                5. 가능한 구체적으로 답변하세요

                답변:
                """, context, question);
    }

    /**
     * 문서를 청크로 분할
     */
    private List<String> splitIntoChunks(String content) {
        List<String> chunks = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return chunks;
        }

        // 문서가 청크 크기보다 작으면 전체를 하나의 청크로
        if (content.length() <= MAX_CHUNK_LENGTH) {
            chunks.add(content.trim());
            return chunks;
        }

        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + MAX_CHUNK_LENGTH, content.length());

            // 청크가 문장 중간에서 끊기지 않도록 조정
            if (end < content.length()) {
                // 마지막 줄바꿈이나 마침표 위치 찾기
                int lastNewline = content.lastIndexOf('\n', end);
                int lastPeriod = content.lastIndexOf('.', end);
                int breakPoint = Math.max(lastNewline, lastPeriod);

                if (breakPoint > start + OVERLAP_LENGTH) {
                    end = breakPoint + 1; // 줄바꿈/마침표 포함
                }
            }

            String chunk = content.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // 마지막 청크면 종료
            if (end >= content.length()) {
                break;
            }

            start = end - OVERLAP_LENGTH; // 겹치는 영역 추가
            if (start < 0) start = 0;
        }

        return chunks;
    }

    /**
     * 코사인 유사도 계산
     */
    private double cosineSimilarity(List<Double> vec1, List<Double> vec2) {
        if (vec1.size() != vec2.size()) {
            throw new IllegalArgumentException("벡터 차원이 일치하지 않습니다");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.size(); i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            norm1 += vec1.get(i) * vec1.get(i);
            norm2 += vec2.get(i) * vec2.get(i);
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 텍스트 자르기 (미리보기용)
     */
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    /**
     * 임베딩과 유사도 점수를 함께 저장하는 내부 클래스
     */
    private static class ScoredEmbedding {
        WikiDocumentEmbedding embedding;
        double score;

        ScoredEmbedding(WikiDocumentEmbedding embedding, double score) {
            this.embedding = embedding;
            this.score = score;
        }

        public double getScore() {
            return score;
        }
    }
}
