package com.srmanagement.wiki.entity;

import com.srmanagement.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "wiki_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WikiFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private WikiDocument document;

    @Column(nullable = false, length = 200)
    private String originalFileName;

    @Column(nullable = false, length = 200)
    private String storedFileName;

    @Column(nullable = false, length = 500)
    private String filePath;

    @Column(nullable = false)
    private Long fileSize;

    @Column(length = 50)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FileType type = FileType.ATTACHMENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ConversionStatus conversionStatus = ConversionStatus.NOT_APPLICABLE;

    @Column(length = 1000)
    private String conversionErrorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column
    private LocalDateTime convertedAt;

    public enum FileType {
        IMAGE,      // 이미지 (PNG, JPG, GIF)
        DOCUMENT,   // 문서 (PDF, DOCX)
        ATTACHMENT  // 기타 첨부파일
    }

    public enum ConversionStatus {
        NOT_APPLICABLE, // 변환 불필요 (이미지, 일반 첨부파일)
        PENDING,        // 변환 대기
        PROCESSING,     // 변환 중
        COMPLETED,      // 변환 완료
        FAILED          // 변환 실패
    }
}
