package com.srmanagement.wiki.dto;

import com.srmanagement.wiki.entity.WikiFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WikiFileResponse {

    private Long id;
    private Long documentId;
    private String originalFileName;
    private String storedFileName;
    private Long fileSize;
    private String mimeType;
    private String type; // IMAGE, DOCUMENT, ATTACHMENT
    private String conversionStatus; // NOT_APPLICABLE, PENDING, PROCESSING, COMPLETED, FAILED
    private String conversionErrorMessage;
    private Long uploadedById;
    private String uploadedByName;
    private LocalDateTime uploadedAt;
    private LocalDateTime convertedAt;
    private String downloadUrl;

    public static WikiFileResponse fromEntity(WikiFile file) {
        return WikiFileResponse.builder()
                .id(file.getId())
                .documentId(file.getDocument() != null ? file.getDocument().getId() : null)
                .originalFileName(file.getOriginalFileName())
                .storedFileName(file.getStoredFileName())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .type(file.getType().name())
                .conversionStatus(file.getConversionStatus().name())
                .conversionErrorMessage(file.getConversionErrorMessage())
                .uploadedById(file.getUploadedBy().getId())
                .uploadedByName(file.getUploadedBy().getName())
                .uploadedAt(file.getUploadedAt())
                .convertedAt(file.getConvertedAt())
                .downloadUrl("/api/wiki/files/" + file.getId())
                .build();
    }
}
