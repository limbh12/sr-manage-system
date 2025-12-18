package com.srmanagement.wiki.dto;

import com.srmanagement.wiki.entity.WikiVersion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WikiVersionResponse {

    private Long id;
    private Long documentId;
    private Integer version;
    private String content;
    private String changeSummary;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;

    public static WikiVersionResponse fromEntity(WikiVersion version) {
        return WikiVersionResponse.builder()
                .id(version.getId())
                .documentId(version.getDocument().getId())
                .version(version.getVersion())
                .content(version.getContent())
                .changeSummary(version.getChangeSummary())
                .createdById(version.getCreatedBy().getId())
                .createdByName(version.getCreatedBy().getName())
                .createdAt(version.getCreatedAt())
                .build();
    }
}
