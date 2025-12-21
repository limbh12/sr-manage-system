package com.srmanagement.wiki.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 요약 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummaryResponse {
    private Long documentId;
    private String summary;
    private LocalDateTime generatedAt;
    private Long processingTimeMs;
    private String status; // GENERATED, CACHED, FAILED
    private String message;
}
