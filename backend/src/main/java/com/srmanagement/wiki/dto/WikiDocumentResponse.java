package com.srmanagement.wiki.dto;

import com.srmanagement.wiki.entity.WikiDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WikiDocumentResponse {

    private Long id;
    private String title;
    private String content;
    private Long categoryId;
    private String categoryName;
    private List<SrInfo> srs;
    private Long createdById;
    private String createdByName;
    private Long updatedById;
    private String updatedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer viewCount;
    private Integer currentVersion;
    private List<WikiFileResponse> files;
    private String aiSummary;
    private LocalDateTime summaryGeneratedAt;
    private Boolean summaryUpToDate;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SrInfo {
        private Long id;
        private String title;
        private String status;
    }

    public static WikiDocumentResponse fromEntity(WikiDocument document) {
        WikiDocumentResponseBuilder builder = WikiDocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .content(document.getContent())
                .createdById(document.getCreatedBy().getId())
                .createdByName(document.getCreatedBy().getName())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .viewCount(document.getViewCount());

        if (document.getCategory() != null) {
            builder.categoryId(document.getCategory().getId())
                   .categoryName(document.getCategory().getName());
        }

        // SR 목록 매핑
        if (document.getSrs() != null && !document.getSrs().isEmpty()) {
            List<SrInfo> srInfos = document.getSrs().stream()
                    .map(sr -> SrInfo.builder()
                            .id(sr.getId())
                            .title(sr.getTitle())
                            .status(sr.getStatus().name())
                            .build())
                    .collect(Collectors.toList());
            builder.srs(srInfos);
        } else {
            builder.srs(new ArrayList<>());
        }

        if (document.getUpdatedBy() != null) {
            builder.updatedById(document.getUpdatedBy().getId())
                   .updatedByName(document.getUpdatedBy().getName());
        }

        // 현재 버전 번호 (버전 목록이 있으면 최대값)
        if (!document.getVersions().isEmpty()) {
            builder.currentVersion(document.getVersions().stream()
                    .mapToInt(v -> v.getVersion())
                    .max()
                    .orElse(0));
        }

        // 파일 목록 매핑
        if (document.getFiles() != null && !document.getFiles().isEmpty()) {
            List<WikiFileResponse> fileResponses = document.getFiles().stream()
                    .map(WikiFileResponse::fromEntity)
                    .collect(Collectors.toList());
            builder.files(fileResponses);
        } else {
            builder.files(new ArrayList<>());
        }

        // AI 요약 정보 매핑
        builder.aiSummary(document.getAiSummary())
               .summaryGeneratedAt(document.getSummaryGeneratedAt());

        // 요약이 최신인지 확인 (문서 수정 후 요약이 생성되었는지)
        if (document.getSummaryGeneratedAt() != null && document.getUpdatedAt() != null) {
            builder.summaryUpToDate(
                document.getSummaryGeneratedAt().isAfter(document.getUpdatedAt()) ||
                document.getSummaryGeneratedAt().isEqual(document.getUpdatedAt())
            );
        } else {
            builder.summaryUpToDate(false);
        }

        return builder.build();
    }
}
