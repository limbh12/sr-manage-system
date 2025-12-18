package com.srmanagement.wiki.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wiki_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WikiCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private WikiCategory parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @Builder.Default
    private List<WikiCategory> children = new ArrayList<>();

    @OneToMany(mappedBy = "category")
    @Builder.Default
    private List<WikiDocument> documents = new ArrayList<>();

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 자식 카테고리 추가 헬퍼 메소드
    public void addChild(WikiCategory child) {
        children.add(child);
        child.setParent(this);
    }

    // 문서 개수 계산
    @Transient
    public int getDocumentCount() {
        return documents != null ? documents.size() : 0;
    }
}
