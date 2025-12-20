package com.srmanagement.wiki.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 검색 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiSearchResponse {

    /**
     * AI가 생성한 답변
     */
    private String answer;

    /**
     * 답변 생성 시 참고한 문서 목록
     */
    @Builder.Default
    private List<SourceDocument> sources = new ArrayList<>();

    /**
     * 답변 생성 소요 시간 (ms)
     */
    private Long processingTimeMs;

    /**
     * 참고 문서 정보 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SourceDocument {
        /**
         * 문서 ID
         */
        private Long documentId;

        /**
         * 문서 제목
         */
        private String title;

        /**
         * 카테고리 이름
         */
        private String categoryName;

        /**
         * 관련 내용 스니펫
         */
        private String snippet;

        /**
         * 유사도 점수 (0~1)
         */
        private Double relevanceScore;
    }
}
