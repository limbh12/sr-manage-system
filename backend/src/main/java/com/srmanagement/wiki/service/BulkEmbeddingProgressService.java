package com.srmanagement.wiki.service;

import com.srmanagement.wiki.dto.BulkEmbeddingProgressEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 일괄 임베딩 진행 상태 관리 서비스
 */
@Service
@Slf4j
public class BulkEmbeddingProgressService {

    // 리소스 타입별 진행 상태 저장
    private final Map<String, BulkEmbeddingProgressEvent> progressMap = new ConcurrentHashMap<>();

    // 리소스 타입별 시작 시간
    private final Map<String, Long> startTimeMap = new ConcurrentHashMap<>();

    /**
     * 진행 중인지 확인
     */
    public boolean isInProgress(String resourceType) {
        BulkEmbeddingProgressEvent progress = progressMap.get(resourceType);
        return progress != null &&
               ("STARTED".equals(progress.getStatus()) || "IN_PROGRESS".equals(progress.getStatus()));
    }

    /**
     * 진행 시작
     */
    public void startProgress(String resourceType, int totalCount) {
        long startTime = System.currentTimeMillis();
        startTimeMap.put(resourceType, startTime);

        BulkEmbeddingProgressEvent event = BulkEmbeddingProgressEvent.builder()
                .resourceType(resourceType)
                .status("STARTED")
                .currentIndex(0)
                .totalCount(totalCount)
                .successCount(0)
                .failureCount(0)
                .progressPercent(0)
                .currentTitle("")
                .elapsedTimeMs(0L)
                .estimatedRemainingMs(null)
                .message(getResourceTypeName(resourceType) + " 임베딩 생성 시작")
                .build();

        progressMap.put(resourceType, event);
        log.info("일괄 임베딩 시작: {} (총 {}건)", resourceType, totalCount);
    }

    /**
     * 진행 상태 업데이트
     */
    public void updateProgress(String resourceType, int currentIndex, String currentTitle,
                               int successCount, int failureCount) {
        BulkEmbeddingProgressEvent current = progressMap.get(resourceType);
        if (current == null) return;

        Long startTime = startTimeMap.get(resourceType);
        long elapsedMs = startTime != null ? System.currentTimeMillis() - startTime : 0;

        int totalCount = current.getTotalCount();
        int progressPercent = totalCount > 0 ? (int) ((currentIndex * 100.0) / totalCount) : 0;

        // 예상 남은 시간 계산
        Long estimatedRemainingMs = null;
        if (currentIndex > 0 && elapsedMs > 0) {
            long avgTimePerItem = elapsedMs / currentIndex;
            int remainingItems = totalCount - currentIndex;
            estimatedRemainingMs = avgTimePerItem * remainingItems;
        }

        BulkEmbeddingProgressEvent event = BulkEmbeddingProgressEvent.builder()
                .resourceType(resourceType)
                .status("IN_PROGRESS")
                .currentIndex(currentIndex)
                .totalCount(totalCount)
                .successCount(successCount)
                .failureCount(failureCount)
                .progressPercent(progressPercent)
                .currentTitle(currentTitle)
                .elapsedTimeMs(elapsedMs)
                .estimatedRemainingMs(estimatedRemainingMs)
                .message(String.format("%s 임베딩 생성 중: %d/%d",
                        getResourceTypeName(resourceType), currentIndex, totalCount))
                .build();

        progressMap.put(resourceType, event);
    }

    /**
     * 진행 완료
     */
    public void completeProgress(String resourceType, int successCount, int failureCount) {
        Long startTime = startTimeMap.get(resourceType);
        long elapsedMs = startTime != null ? System.currentTimeMillis() - startTime : 0;

        BulkEmbeddingProgressEvent current = progressMap.get(resourceType);
        int totalCount = current != null ? current.getTotalCount() : successCount + failureCount;

        BulkEmbeddingProgressEvent event = BulkEmbeddingProgressEvent.builder()
                .resourceType(resourceType)
                .status("COMPLETED")
                .currentIndex(totalCount)
                .totalCount(totalCount)
                .successCount(successCount)
                .failureCount(failureCount)
                .progressPercent(100)
                .currentTitle("")
                .elapsedTimeMs(elapsedMs)
                .estimatedRemainingMs(0L)
                .message(String.format("%s 임베딩 생성 완료: 성공 %d건, 실패 %d건",
                        getResourceTypeName(resourceType), successCount, failureCount))
                .build();

        progressMap.put(resourceType, event);
        log.info("일괄 임베딩 완료: {} (성공: {}, 실패: {}, 소요시간: {}ms)",
                resourceType, successCount, failureCount, elapsedMs);
    }

    /**
     * 진행 실패
     */
    public void failProgress(String resourceType, String errorMessage) {
        Long startTime = startTimeMap.get(resourceType);
        long elapsedMs = startTime != null ? System.currentTimeMillis() - startTime : 0;

        BulkEmbeddingProgressEvent current = progressMap.get(resourceType);

        BulkEmbeddingProgressEvent event = BulkEmbeddingProgressEvent.builder()
                .resourceType(resourceType)
                .status("FAILED")
                .currentIndex(current != null ? current.getCurrentIndex() : 0)
                .totalCount(current != null ? current.getTotalCount() : 0)
                .successCount(current != null ? current.getSuccessCount() : 0)
                .failureCount(current != null ? current.getFailureCount() : 0)
                .progressPercent(current != null ? current.getProgressPercent() : 0)
                .currentTitle("")
                .elapsedTimeMs(elapsedMs)
                .estimatedRemainingMs(null)
                .message(errorMessage)
                .build();

        progressMap.put(resourceType, event);
        log.error("일괄 임베딩 실패: {} - {}", resourceType, errorMessage);
    }

    /**
     * 현재 진행 상태 조회
     */
    public BulkEmbeddingProgressEvent getCurrentProgress(String resourceType) {
        return progressMap.get(resourceType);
    }

    /**
     * 진행 상태 초기화 (완료 후 일정 시간 뒤 호출)
     */
    public void clearProgress(String resourceType) {
        progressMap.remove(resourceType);
        startTimeMap.remove(resourceType);
    }

    /**
     * 리소스 타입 한글명
     */
    private String getResourceTypeName(String resourceType) {
        switch (resourceType) {
            case "WIKI": return "Wiki 문서";
            case "SR": return "SR";
            case "SURVEY": return "현황조사";
            default: return resourceType;
        }
    }
}
