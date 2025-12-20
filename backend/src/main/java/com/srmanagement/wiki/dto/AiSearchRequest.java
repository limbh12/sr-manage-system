package com.srmanagement.wiki.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 검색 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiSearchRequest {

    /**
     * 사용자 질문
     */
    @NotBlank(message = "질문을 입력해주세요")
    private String question;

    /**
     * 검색할 상위 K개 문서 (기본값: 3)
     */
    @Builder.Default
    private Integer topK = 3;

    /**
     * 카테고리 ID 필터 (선택적)
     */
    private Long categoryId;

    /**
     * 유사도 임계값 (0~1, 기본값: 0.7)
     */
    @Builder.Default
    private Double similarityThreshold = 0.7;
}
