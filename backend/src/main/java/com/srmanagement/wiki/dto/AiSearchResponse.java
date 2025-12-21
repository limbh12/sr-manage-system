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
         * 리소스 타입 (WIKI, SR, SURVEY)
         */
        private String resourceType;

        /**
         * 리소스 ID (Wiki 문서 ID, SR PK, Survey PK)
         */
        private Long resourceId;

        /**
         * 리소스 식별자 (예: SR-2512-0001, SURVEY-123)
         */
        private String resourceIdentifier;

        /**
         * 문서 ID (기존 호환용 - Wiki일 경우 resourceId와 동일)
         */
        private Long documentId;

        /**
         * 문서 제목
         */
        private String title;

        /**
         * 카테고리/분류 이름
         */
        private String categoryName;

        /**
         * 상태 (SR: OPEN/CLOSED, Survey: PENDING/COMPLETED 등)
         */
        private String status;

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
