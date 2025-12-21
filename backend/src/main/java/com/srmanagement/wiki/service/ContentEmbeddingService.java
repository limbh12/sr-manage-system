package com.srmanagement.wiki.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srmanagement.entity.OpenApiSurvey;
import com.srmanagement.entity.Sr;
import com.srmanagement.repository.OpenApiSurveyRepository;
import com.srmanagement.repository.SrRepository;
import com.srmanagement.wiki.entity.ContentEmbedding;
import com.srmanagement.wiki.entity.ContentEmbedding.ResourceType;
import com.srmanagement.wiki.entity.WikiDocument;
import com.srmanagement.wiki.repository.ContentEmbeddingRepository;
import com.srmanagement.wiki.repository.WikiDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 통합 콘텐츠 임베딩 서비스
 * - Wiki 문서, SR, OPEN API 현황조사 임베딩 생성
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContentEmbeddingService {

    private final ContentEmbeddingRepository embeddingRepository;
    private final WikiDocumentRepository wikiDocumentRepository;
    private final SrRepository srRepository;
    private final OpenApiSurveyRepository surveyRepository;
    private final OllamaEmbeddingModel embeddingModel;
    private final ObjectMapper objectMapper;

    // Self-injection for @Transactional to work in async bulk methods
    @Autowired
    @Lazy
    private ContentEmbeddingService self;

    private static final int MAX_CHUNK_LENGTH = 2000;
    private static final int OVERLAP_LENGTH = 200;

    /**
     * Wiki 문서 임베딩 생성
     */
    @Transactional
    public void generateWikiEmbedding(Long documentId) {
        WikiDocument document = wikiDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다: " + documentId));

        // 기존 임베딩 삭제
        embeddingRepository.deleteByResourceTypeAndResourceId(ResourceType.WIKI, documentId);

        // 임베딩할 텍스트 생성: 제목 + 내용
        String fullContent = document.getTitle() + "\n\n" +
                (document.getContent() != null ? document.getContent() : "");

        generateEmbeddings(
                ResourceType.WIKI,
                documentId,
                document.getTitle(),
                document.getTitle(),
                fullContent,
                document.getCategory() != null ? document.getCategory().getName() : null,
                null,
                document.getUpdatedAt()
        );

        log.info("✅ Wiki 문서 임베딩 완료: {} (ID: {})", document.getTitle(), documentId);
    }

    /**
     * SR 임베딩 생성
     */
    @Transactional
    public void generateSrEmbedding(Long srId) {
        Sr sr = srRepository.findById(srId)
                .orElseThrow(() -> new RuntimeException("SR을 찾을 수 없습니다: " + srId));

        // 삭제된 SR은 임베딩 제거
        if (Boolean.TRUE.equals(sr.getDeleted())) {
            embeddingRepository.deleteByResourceTypeAndResourceId(ResourceType.SR, srId);
            log.info("🗑️ 삭제된 SR 임베딩 제거: {}", sr.getSrId());
            return;
        }

        // 기존 임베딩 삭제
        embeddingRepository.deleteByResourceTypeAndResourceId(ResourceType.SR, srId);

        // 임베딩할 텍스트 생성
        StringBuilder content = new StringBuilder();
        content.append("SR ID: ").append(sr.getSrId()).append("\n");
        content.append("제목: ").append(sr.getTitle()).append("\n");

        if (sr.getDescription() != null && !sr.getDescription().isEmpty()) {
            content.append("\n요청사항:\n").append(sr.getDescription()).append("\n");
        }

        if (sr.getProcessingDetails() != null && !sr.getProcessingDetails().isEmpty()) {
            content.append("\n처리내용:\n").append(sr.getProcessingDetails()).append("\n");
        }

        if (sr.getCategory() != null) {
            content.append("\n분류: ").append(sr.getCategory());
        }

        if (sr.getRequestType() != null) {
            content.append("\n요청구분: ").append(sr.getRequestType());
        }

        generateEmbeddings(
                ResourceType.SR,
                srId,
                sr.getSrId(),
                sr.getSrId() + " - " + sr.getTitle(),
                content.toString(),
                sr.getCategory(),
                sr.getStatus() != null ? sr.getStatus().name() : null,
                sr.getUpdatedAt()
        );

        log.info("✅ SR 임베딩 완료: {} (ID: {})", sr.getSrId(), srId);
    }

    /**
     * OPEN API 현황조사 임베딩 생성
     */
    @Transactional
    public void generateSurveyEmbedding(Long surveyId) {
        OpenApiSurvey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new RuntimeException("현황조사를 찾을 수 없습니다: " + surveyId));

        // 기존 임베딩 삭제
        embeddingRepository.deleteByResourceTypeAndResourceId(ResourceType.SURVEY, surveyId);

        // 임베딩할 텍스트 생성
        StringBuilder content = new StringBuilder();
        content.append("시스템명: ").append(survey.getSystemName()).append("\n");
        content.append("기관: ").append(survey.getOrganization() != null ? survey.getOrganization().getName() : "").append("\n");
        content.append("부서: ").append(survey.getDepartment()).append("\n");

        // 운영 환경 정보
        content.append("\n[운영 환경]\n");
        content.append("- 운영환경: ").append(survey.getOperationEnv()).append("\n");
        content.append("- 현재방식: ").append(survey.getCurrentMethod()).append("\n");
        content.append("- 희망방식: ").append(survey.getDesiredMethod()).append("\n");

        // 서버 정보
        if (survey.getWebServerType() != null) {
            content.append("- 웹서버: ").append(survey.getWebServerType());
            if (survey.getWebServerVersion() != null) {
                content.append(" ").append(survey.getWebServerVersion());
            }
            content.append("\n");
        }

        if (survey.getWasServerType() != null) {
            content.append("- WAS: ").append(survey.getWasServerType());
            if (survey.getWasServerVersion() != null) {
                content.append(" ").append(survey.getWasServerVersion());
            }
            content.append("\n");
        }

        if (survey.getDbServerType() != null) {
            content.append("- DB: ").append(survey.getDbServerType());
            if (survey.getDbServerVersion() != null) {
                content.append(" ").append(survey.getDbServerVersion());
            }
            content.append("\n");
        }

        // 개발 환경
        if (survey.getDevLanguage() != null) {
            content.append("- 개발언어: ").append(survey.getDevLanguage());
            if (survey.getDevLanguageVersion() != null) {
                content.append(" ").append(survey.getDevLanguageVersion());
            }
            content.append("\n");
        }

        if (survey.getDevFramework() != null) {
            content.append("- 프레임워크: ").append(survey.getDevFramework());
            if (survey.getDevFrameworkVersion() != null) {
                content.append(" ").append(survey.getDevFrameworkVersion());
            }
            content.append("\n");
        }

        // 기타 요청사항
        if (survey.getOtherRequests() != null && !survey.getOtherRequests().isEmpty()) {
            content.append("\n기타 요청사항:\n").append(survey.getOtherRequests()).append("\n");
        }

        if (survey.getNote() != null && !survey.getNote().isEmpty()) {
            content.append("\n비고:\n").append(survey.getNote()).append("\n");
        }

        String orgName = survey.getOrganization() != null ? survey.getOrganization().getName() : "";
        String title = survey.getSystemName() + " (" + orgName + ")";

        generateEmbeddings(
                ResourceType.SURVEY,
                surveyId,
                "SURVEY-" + surveyId,
                title,
                content.toString(),
                orgName,
                survey.getStatus() != null ? survey.getStatus().name() : null,
                survey.getUpdatedAt()
        );

        log.info("✅ 현황조사 임베딩 완료: {} (ID: {})", survey.getSystemName(), surveyId);
    }

    /**
     * 비동기 Wiki 문서 임베딩 생성
     */
    @Async("embeddingTaskExecutor")
    @Transactional
    public void generateWikiEmbeddingAsync(Long documentId) {
        try {
            Thread.sleep(500); // 트랜잭션 커밋 대기
            generateWikiEmbedding(documentId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Wiki 임베딩 생성 대기 중 인터럽트 발생", e);
        } catch (Exception e) {
            log.error("Wiki 임베딩 생성 실패: documentId={}", documentId, e);
        }
    }

    /**
     * 비동기 SR 임베딩 생성
     */
    @Async("embeddingTaskExecutor")
    @Transactional
    public void generateSrEmbeddingAsync(Long srId) {
        try {
            Thread.sleep(500); // 트랜잭션 커밋 대기
            generateSrEmbedding(srId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("SR 임베딩 생성 대기 중 인터럽트 발생", e);
        } catch (Exception e) {
            log.error("SR 임베딩 생성 실패: srId={}", srId, e);
        }
    }

    /**
     * 비동기 현황조사 임베딩 생성
     */
    @Async("embeddingTaskExecutor")
    @Transactional
    public void generateSurveyEmbeddingAsync(Long surveyId) {
        try {
            Thread.sleep(500); // 트랜잭션 커밋 대기
            generateSurveyEmbedding(surveyId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("현황조사 임베딩 생성 대기 중 인터럽트 발생", e);
        } catch (Exception e) {
            log.error("현황조사 임베딩 생성 실패: surveyId={}", surveyId, e);
        }
    }

    /**
     * 전체 Wiki 문서 수 조회
     */
    public int getWikiDocumentCount() {
        return (int) wikiDocumentRepository.count();
    }

    /**
     * 전체 SR 수 조회 (삭제되지 않은 것)
     */
    public int getSrCount() {
        return srRepository.findByDeletedFalse().size();
    }

    /**
     * 전체 현황조사 수 조회
     */
    public int getSurveyCount() {
        return (int) surveyRepository.count();
    }

    /**
     * 전체 Wiki 문서 임베딩 생성 (일괄 - 동기)
     */
    @Transactional
    public int generateAllWikiEmbeddings() {
        List<WikiDocument> allDocuments = wikiDocumentRepository.findAll();
        int count = 0;

        for (WikiDocument doc : allDocuments) {
            try {
                generateWikiEmbedding(doc.getId());
                count++;
            } catch (Exception e) {
                log.error("Wiki 임베딩 생성 실패: {}", doc.getTitle(), e);
            }
        }

        log.info("✅ 전체 Wiki 문서 임베딩 완료: {}건", count);
        return count;
    }

    /**
     * 전체 Wiki 문서 임베딩 생성 (일괄 - 비동기, 진행률 추적)
     */
    @Async("embeddingTaskExecutor")
    public void generateAllWikiEmbeddingsAsync(BulkEmbeddingProgressService progressService) {
        List<WikiDocument> allDocuments = wikiDocumentRepository.findAll();
        int totalCount = allDocuments.size();
        int successCount = 0;
        int failureCount = 0;

        progressService.startProgress("WIKI", totalCount);

        for (int i = 0; i < allDocuments.size(); i++) {
            WikiDocument doc = allDocuments.get(i);
            try {
                self.generateWikiEmbedding(doc.getId());
                successCount++;
                progressService.updateProgress("WIKI", i + 1, doc.getTitle(), successCount, failureCount);
            } catch (Exception e) {
                log.error("Wiki 임베딩 생성 실패: {}", doc.getTitle(), e);
                failureCount++;
                progressService.updateProgress("WIKI", i + 1, doc.getTitle(), successCount, failureCount);
            }
        }

        progressService.completeProgress("WIKI", successCount, failureCount);
    }

    /**
     * 전체 SR 임베딩 생성 (일괄 - 동기)
     */
    @Transactional
    public int generateAllSrEmbeddings() {
        List<Sr> allSrs = srRepository.findByDeletedFalse();
        int count = 0;

        for (Sr sr : allSrs) {
            try {
                generateSrEmbedding(sr.getId());
                count++;
            } catch (Exception e) {
                log.error("SR 임베딩 생성 실패: {}", sr.getSrId(), e);
            }
        }

        log.info("✅ 전체 SR 임베딩 완료: {}건", count);
        return count;
    }

    /**
     * 전체 SR 임베딩 생성 (일괄 - 비동기, 진행률 추적)
     */
    @Async("embeddingTaskExecutor")
    public void generateAllSrEmbeddingsAsync(BulkEmbeddingProgressService progressService) {
        // ID만 가져와서 LazyInitializationException 방지
        List<Long> srIds = srRepository.findByDeletedFalse().stream()
                .map(Sr::getId)
                .toList();
        int totalCount = srIds.size();
        int successCount = 0;
        int failureCount = 0;

        progressService.startProgress("SR", totalCount);

        for (int i = 0; i < srIds.size(); i++) {
            Long srId = srIds.get(i);
            String title = "SR-" + srId;
            try {
                self.generateSrEmbedding(srId);
                // 성공 후 제목 업데이트를 위해 다시 조회
                Sr sr = srRepository.findById(srId).orElse(null);
                if (sr != null) {
                    title = sr.getSrId() + " - " + sr.getTitle();
                }
                successCount++;
                progressService.updateProgress("SR", i + 1, title, successCount, failureCount);
            } catch (Exception e) {
                log.error("SR 임베딩 생성 실패: srId={}", srId, e);
                failureCount++;
                progressService.updateProgress("SR", i + 1, title, successCount, failureCount);
            }
        }

        progressService.completeProgress("SR", successCount, failureCount);
    }

    /**
     * 전체 현황조사 임베딩 생성 (일괄 - 동기)
     */
    @Transactional
    public int generateAllSurveyEmbeddings() {
        List<OpenApiSurvey> allSurveys = surveyRepository.findAll();
        int count = 0;

        for (OpenApiSurvey survey : allSurveys) {
            try {
                generateSurveyEmbedding(survey.getId());
                count++;
            } catch (Exception e) {
                log.error("현황조사 임베딩 생성 실패: {}", survey.getSystemName(), e);
            }
        }

        log.info("✅ 전체 현황조사 임베딩 완료: {}건", count);
        return count;
    }

    /**
     * 전체 현황조사 임베딩 생성 (일괄 - 비동기, 진행률 추적)
     */
    @Async("embeddingTaskExecutor")
    public void generateAllSurveyEmbeddingsAsync(BulkEmbeddingProgressService progressService) {
        // ID만 가져와서 LazyInitializationException 방지
        List<Long> surveyIds = surveyRepository.findAll().stream()
                .map(OpenApiSurvey::getId)
                .toList();
        int totalCount = surveyIds.size();
        int successCount = 0;
        int failureCount = 0;

        progressService.startProgress("SURVEY", totalCount);

        for (int i = 0; i < surveyIds.size(); i++) {
            Long surveyId = surveyIds.get(i);
            String title = "Survey-" + surveyId;
            try {
                // 트랜잭션 내에서 다시 조회하여 임베딩 생성
                self.generateSurveyEmbedding(surveyId);
                // 성공 후 제목 업데이트를 위해 다시 조회
                OpenApiSurvey survey = surveyRepository.findById(surveyId).orElse(null);
                if (survey != null) {
                    title = survey.getSystemName() + " (" +
                            (survey.getOrganization() != null ? survey.getOrganization().getName() : "") + ")";
                }
                successCount++;
                progressService.updateProgress("SURVEY", i + 1, title, successCount, failureCount);
            } catch (Exception e) {
                log.error("현황조사 임베딩 생성 실패: surveyId={}", surveyId, e);
                failureCount++;
                progressService.updateProgress("SURVEY", i + 1, title, successCount, failureCount);
            }
        }

        progressService.completeProgress("SURVEY", successCount, failureCount);
    }

    /**
     * 임베딩 통계 조회
     */
    @Transactional(readOnly = true)
    public EmbeddingStats getEmbeddingStats() {
        long wikiCount = embeddingRepository.countDistinctResourcesByType(ResourceType.WIKI);
        long srCount = embeddingRepository.countDistinctResourcesByType(ResourceType.SR);
        long surveyCount = embeddingRepository.countDistinctResourcesByType(ResourceType.SURVEY);

        return new EmbeddingStats(wikiCount, srCount, surveyCount);
    }

    /**
     * 특정 리소스 타입의 임베딩 전체 삭제
     */
    @Transactional
    public int deleteAllByResourceType(String resourceTypeStr) {
        ResourceType resourceType = ResourceType.valueOf(resourceTypeStr);
        List<ContentEmbedding> embeddings = embeddingRepository.findByResourceType(resourceType);
        int count = embeddings.size();
        embeddingRepository.deleteAll(embeddings);
        log.info("🗑️ {} 타입 임베딩 전체 삭제: {}개", resourceType, count);
        return count;
    }

    /**
     * 공통 임베딩 생성 로직
     */
    private void generateEmbeddings(
            ResourceType resourceType,
            Long resourceId,
            String resourceIdentifier,
            String title,
            String content,
            String category,
            String status,
            java.time.LocalDateTime sourceUpdatedAt) {

        List<String> chunks = splitIntoChunks(content);

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);

            try {
                // 임베딩 벡터 생성
                EmbeddingResponse response = embeddingModel.embedForResponse(List.of(chunk));
                float[] embeddingArray = response.getResults().get(0).getOutput();

                List<Double> embedding = new ArrayList<>();
                for (float value : embeddingArray) {
                    embedding.add((double) value);
                }

                String embeddingJson = objectMapper.writeValueAsString(embedding);

                ContentEmbedding embeddingEntity = ContentEmbedding.builder()
                        .resourceType(resourceType)
                        .resourceId(resourceId)
                        .resourceIdentifier(resourceIdentifier)
                        .title(title)
                        .content(chunk)
                        .embeddingVector(embeddingJson)
                        .chunkIndex(i)
                        .category(category)
                        .status(status)
                        .sourceUpdatedAt(sourceUpdatedAt)
                        .build();

                embeddingRepository.save(embeddingEntity);

            } catch (JsonProcessingException e) {
                log.error("임베딩 벡터 직렬화 실패", e);
                throw new RuntimeException("임베딩 생성 실패", e);
            }
        }
    }

    /**
     * 텍스트를 청크로 분할
     */
    private List<String> splitIntoChunks(String content) {
        List<String> chunks = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return chunks;
        }

        if (content.length() <= MAX_CHUNK_LENGTH) {
            chunks.add(content.trim());
            return chunks;
        }

        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + MAX_CHUNK_LENGTH, content.length());

            if (end < content.length()) {
                int lastNewline = content.lastIndexOf('\n', end);
                int lastPeriod = content.lastIndexOf('.', end);
                int breakPoint = Math.max(lastNewline, lastPeriod);

                if (breakPoint > start + OVERLAP_LENGTH) {
                    end = breakPoint + 1;
                }
            }

            String chunk = content.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            if (end >= content.length()) {
                break;
            }

            start = end - OVERLAP_LENGTH;
            if (start < 0) start = 0;
        }

        return chunks;
    }

    /**
     * SR 임베딩 상태 조회
     */
    @Transactional(readOnly = true)
    public SrEmbeddingStatus getSrEmbeddingStatus(Long srId) {
        Sr sr = srRepository.findById(srId)
                .orElseThrow(() -> new RuntimeException("SR을 찾을 수 없습니다: " + srId));

        List<ContentEmbedding> embeddings = embeddingRepository.findByResourceTypeAndResourceId(ResourceType.SR, srId);

        boolean hasEmbedding = !embeddings.isEmpty();
        int chunkCount = embeddings.size();
        java.time.LocalDateTime lastEmbeddingDate = embeddings.stream()
                .map(ContentEmbedding::getCreatedAt)
                .max(java.time.LocalDateTime::compareTo)
                .orElse(null);
        java.time.LocalDateTime sourceUpdatedAt = sr.getUpdatedAt();

        // 임베딩이 최신인지 확인 (source 업데이트 시간과 비교)
        boolean isUpToDate = hasEmbedding && lastEmbeddingDate != null &&
                !lastEmbeddingDate.isBefore(sourceUpdatedAt);

        return new SrEmbeddingStatus(
                srId,
                sr.getSrId(),
                sr.getTitle(),
                hasEmbedding,
                chunkCount,
                lastEmbeddingDate,
                sourceUpdatedAt,
                isUpToDate
        );
    }

    /**
     * Survey 임베딩 상태 조회
     */
    @Transactional(readOnly = true)
    public SurveyEmbeddingStatus getSurveyEmbeddingStatus(Long surveyId) {
        OpenApiSurvey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new RuntimeException("현황조사를 찾을 수 없습니다: " + surveyId));

        List<ContentEmbedding> embeddings = embeddingRepository.findByResourceTypeAndResourceId(ResourceType.SURVEY, surveyId);

        boolean hasEmbedding = !embeddings.isEmpty();
        int chunkCount = embeddings.size();
        java.time.LocalDateTime lastEmbeddingDate = embeddings.stream()
                .map(ContentEmbedding::getCreatedAt)
                .max(java.time.LocalDateTime::compareTo)
                .orElse(null);
        java.time.LocalDateTime sourceUpdatedAt = survey.getUpdatedAt();

        // 임베딩이 최신인지 확인
        boolean isUpToDate = hasEmbedding && lastEmbeddingDate != null &&
                !lastEmbeddingDate.isBefore(sourceUpdatedAt);

        String orgName = survey.getOrganization() != null ? survey.getOrganization().getName() : "";

        return new SurveyEmbeddingStatus(
                surveyId,
                survey.getSystemName(),
                orgName,
                hasEmbedding,
                chunkCount,
                lastEmbeddingDate,
                sourceUpdatedAt,
                isUpToDate
        );
    }

    /**
     * 임베딩 통계 DTO
     */
    public record EmbeddingStats(long wikiCount, long srCount, long surveyCount) {
        public long getTotal() {
            return wikiCount + srCount + surveyCount;
        }
    }

    /**
     * SR 임베딩 상태 DTO
     */
    public record SrEmbeddingStatus(
            Long id,
            String srId,
            String title,
            boolean hasEmbedding,
            int chunkCount,
            java.time.LocalDateTime lastEmbeddingDate,
            java.time.LocalDateTime sourceUpdatedAt,
            boolean isUpToDate
    ) {}

    /**
     * Survey 임베딩 상태 DTO
     */
    public record SurveyEmbeddingStatus(
            Long id,
            String systemName,
            String organizationName,
            boolean hasEmbedding,
            int chunkCount,
            java.time.LocalDateTime lastEmbeddingDate,
            java.time.LocalDateTime sourceUpdatedAt,
            boolean isUpToDate
    ) {}
}
