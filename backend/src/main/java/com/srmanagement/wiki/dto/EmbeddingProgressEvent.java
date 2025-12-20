package com.srmanagement.wiki.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 임베딩 진행률 이벤트 DTO
 * - SSE로 클라이언트에 전송되는 진행 상태 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingProgressEvent {

    private Long documentId;
    private String documentTitle;

    /**
     * 상태: STARTED, IN_PROGRESS, COMPLETED, FAILED
     */
    private String status;

    /**
     * 현재 처리 중인 청크 번호 (1부터 시작)
     */
    private int currentChunk;

    /**
     * 전체 청크 수
     */
    private int totalChunks;

    /**
     * 진행률 (0-100)
     */
    private int progressPercent;

    /**
     * 현재 청크 처리 시간 (ms)
     */
    private long chunkProcessingTimeMs;

    /**
     * 전체 경과 시간 (ms)
     */
    private long elapsedTimeMs;

    /**
     * 예상 남은 시간 (ms)
     */
    private long estimatedRemainingMs;

    /**
     * 메시지 (오류 시 오류 메시지)
     */
    private String message;
}
