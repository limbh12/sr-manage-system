package com.srmanagement.wiki.entity;

import com.srmanagement.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "wiki_version")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WikiVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private WikiDocument document;

    @Column(nullable = false)
    private Integer version;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 200)
    private String changeSummary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
