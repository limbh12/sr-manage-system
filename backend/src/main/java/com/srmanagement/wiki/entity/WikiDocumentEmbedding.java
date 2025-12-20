package com.srmanagement.wiki.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Wiki 문서 임베딩 저장 엔티티
 * - 문서 내용을 임베딩 벡터로 변환하여 저장
 * - RAG 기반 유사도 검색에 사용
 */
@Entity
@Table(name = "wiki_document_embedding",
       indexes = @Index(name = "idx_document_id", columnList = "documentId"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WikiDocumentEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 연관된 Wiki 문서 ID
     */
    @Column(nullable = false)
    private Long documentId;

    /**
     * 임베딩 대상 텍스트 (원본 콘텐츠 또는 청크)
     * TEXT 타입으로 큰 텍스트 저장
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 임베딩 벡터 (JSON 배열 형태로 저장)
     * Snowflake Arctic Embed 모델: 768차원
     * 예: [0.123, -0.456, 0.789, ...]
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String embeddingVector;

    /**
     * 문서 청크 순서 (긴 문서를 여러 청크로 나눈 경우)
     * 0부터 시작
     */
    @Column(nullable = false)
    private Integer chunkIndex;

    /**
     * 문서 제목 (검색 결과 표시용)
     */
    @Column(length = 200, nullable = false)
    private String documentTitle;

    /**
     * 카테고리 ID (검색 필터링용)
     */
    private Long categoryId;

    /**
     * 카테고리 이름 (검색 필터링용)
     */
    @Column(length = 100)
    private String categoryName;

    /**
     * 임베딩 생성 시 참조한 문서의 updatedAt 시점
     * (타이밍 이슈 없이 정확한 최신 여부 비교용)
     */
    private LocalDateTime sourceDocumentUpdatedAt;

    /**
     * 임베딩 생성 일시
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 임베딩 업데이트 일시 (문서 수정 시)
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
