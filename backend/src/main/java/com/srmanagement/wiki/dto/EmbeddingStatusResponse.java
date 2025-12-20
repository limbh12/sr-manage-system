package com.srmanagement.wiki.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 임베딩 상태 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmbeddingStatusResponse {

    /**
     * 문서 ID
     */
    private Long documentId;

    /**
     * 임베딩 존재 여부
     */
    private boolean hasEmbedding;

    /**
     * 임베딩 청크 개수
     */
    private Long chunkCount;

    /**
     * 마지막 임베딩 생성 일시
     */
    private LocalDateTime lastEmbeddingDate;

    /**
     * 문서 마지막 수정 일시
     */
    private LocalDateTime documentUpdatedAt;

    /**
     * 임베딩 최신 여부 (문서 수정 이후 임베딩 생성 여부)
     */
    @JsonProperty("isUpToDate")
    private boolean isUpToDate;
}
