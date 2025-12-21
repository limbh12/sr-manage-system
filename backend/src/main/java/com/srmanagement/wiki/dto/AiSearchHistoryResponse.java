package com.srmanagement.wiki.dto;

import com.srmanagement.wiki.entity.AiSearchHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * AI 검색 이력 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiSearchHistoryResponse {

    private Long id;
    private String question;
    private String answerPreview;
    private Integer sourceCount;
    private List<String> resourceTypes;
    private Long processingTimeMs;
    private LocalDateTime createdAt;
    private String username;

    public static AiSearchHistoryResponse from(AiSearchHistory history) {
        String answerPreview = history.getAnswer();
        if (answerPreview != null && answerPreview.length() > 200) {
            answerPreview = answerPreview.substring(0, 200) + "...";
        }

        List<String> types = null;
        if (history.getResourceTypes() != null && !history.getResourceTypes().isEmpty()) {
            types = Arrays.asList(history.getResourceTypes().split(","));
        }

        return AiSearchHistoryResponse.builder()
                .id(history.getId())
                .question(history.getQuestion())
                .answerPreview(answerPreview)
                .sourceCount(history.getSourceCount())
                .resourceTypes(types)
                .processingTimeMs(history.getProcessingTimeMs())
                .createdAt(history.getCreatedAt())
                .username(history.getUser() != null ? history.getUser().getUsername() : null)
                .build();
    }
}
