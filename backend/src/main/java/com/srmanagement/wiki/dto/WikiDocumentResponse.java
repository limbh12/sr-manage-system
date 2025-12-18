package com.srmanagement.wiki.dto;

import com.srmanagement.wiki.entity.WikiDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private Long srId;
    private String srTitle;
    private Long createdById;
    private String createdByName;
    private Long updatedById;
    private String updatedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer viewCount;
    private Integer currentVersion;

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

        if (document.getSr() != null) {
            builder.srId(document.getSr().getId())
                   .srTitle(document.getSr().getTitle());
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

        return builder.build();
    }
}
