package com.srmanagement.wiki.entity;

import com.srmanagement.entity.Sr;
import com.srmanagement.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wiki_document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WikiDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private WikiCategory category;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "wiki_document_sr",
        joinColumns = @JoinColumn(name = "document_id"),
        inverseJoinColumns = @JoinColumn(name = "sr_id")
    )
    @Builder.Default
    private List<Sr> srs = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WikiVersion> versions = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WikiFile> files = new ArrayList<>();

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "summary_generated_at")
    private LocalDateTime summaryGeneratedAt;

    // 조회수 증가
    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0 : this.viewCount) + 1;
    }

    // 버전 저장 헬퍼 메소드
    public void addVersion(WikiVersion version) {
        versions.add(version);
        version.setDocument(this);
    }

    // 파일 추가 헬퍼 메소드
    public void addFile(WikiFile file) {
        files.add(file);
        file.setDocument(this);
    }
}
