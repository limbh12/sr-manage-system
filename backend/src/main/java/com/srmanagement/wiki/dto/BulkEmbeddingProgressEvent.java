package com.srmanagement.wiki.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 일괄 임베딩 진행률 이벤트 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkEmbeddingProgressEvent {

    /**
     * 리소스 타입 (WIKI, SR, SURVEY)
     */
    private String resourceType;

    /**
     * 상태 (STARTED, IN_PROGRESS, COMPLETED, FAILED)
     */
    private String status;

    /**
     * 현재 처리 중인 항목 인덱스
     */
    private int currentIndex;

    /**
     * 전체 항목 수
     */
    private int totalCount;

    /**
     * 성공 개수
     */
    private int successCount;

    /**
     * 실패 개수
     */
    private int failureCount;

    /**
     * 진행률 (0~100)
     */
    private int progressPercent;

    /**
     * 현재 처리 중인 항목 제목
     */
    private String currentTitle;

    /**
     * 경과 시간 (ms)
     */
    private Long elapsedTimeMs;

    /**
     * 예상 남은 시간 (ms)
     */
    private Long estimatedRemainingMs;

    /**
     * 메시지
     */
    private String message;
}
