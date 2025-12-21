package com.srmanagement.wiki.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
     * 검색할 상위 K개 문서 (기본값: 5)
     */
    @Builder.Default
    private Integer topK = 5;

    /**
     * 카테고리 ID 필터 (선택적, Wiki만 적용)
     */
    private Long categoryId;

    /**
     * 유사도 임계값 (0~1, 기본값: 0.6)
     */
    @Builder.Default
    private Double similarityThreshold = 0.6;

    /**
     * 검색할 리소스 타입 (null이면 전체 검색)
     * WIKI, SR, SURVEY
     */
    private List<String> resourceTypes;

    /**
     * 통합 검색 사용 여부 (기본값: true)
     * true: ContentEmbedding 테이블 사용 (Wiki, SR, Survey 통합)
     * false: WikiDocumentEmbedding만 사용 (기존 호환)
     */
    @Builder.Default
    private Boolean useUnifiedSearch = true;
}
