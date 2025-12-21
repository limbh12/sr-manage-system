package com.srmanagement.wiki.entity;

import com.srmanagement.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * AI 검색 이력 엔티티
 */
@Entity
@Table(name = "ai_search_history", indexes = {
        @Index(name = "idx_search_history_user", columnList = "user_id"),
        @Index(name = "idx_search_history_created", columnList = "created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiSearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 검색한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 검색 질문
     */
    @Column(nullable = false, length = 1000)
    private String question;

    /**
     * AI 답변 (요약 저장, 너무 길면 잘라서 저장)
     */
    @Column(columnDefinition = "TEXT")
    private String answer;

    /**
     * 참고 자료 개수
     */
    @Column(name = "source_count")
    private Integer sourceCount;

    /**
     * 검색한 리소스 타입들 (콤마 구분)
     * 예: "WIKI,SR,SURVEY"
     */
    @Column(name = "resource_types", length = 100)
    private String resourceTypes;

    /**
     * 처리 시간 (밀리초)
     */
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    /**
     * 검색 시간
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
