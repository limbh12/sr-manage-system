package com.srmanagement.wiki.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 통합 콘텐츠 임베딩 저장 엔티티
 * - Wiki 문서, SR, OPEN API 현황조사 등 다양한 리소스 임베딩 저장
 * - RAG 기반 통합 AI 검색에 사용
 */
@Entity
@Table(name = "content_embedding",
       indexes = {
           @Index(name = "idx_content_embedding_resource", columnList = "resourceType, resourceId"),
           @Index(name = "idx_content_embedding_type", columnList = "resourceType")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 리소스 유형 (WIKI, SR, SURVEY)
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ResourceType resourceType;

    /**
     * 리소스 ID (Wiki 문서 ID, SR ID, Survey ID)
     */
    @Column(nullable = false)
    private Long resourceId;

    /**
     * 리소스 식별자 (예: SR-2512-0001, Wiki 문서 제목 등)
     * 검색 결과 표시용
     */
    @Column(length = 200, nullable = false)
    private String resourceIdentifier;

    /**
     * 리소스 제목/요약
     */
    @Column(length = 500, nullable = false)
    private String title;

    /**
     * 임베딩 대상 텍스트 (원본 콘텐츠 또는 청크)
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 임베딩 벡터 (JSON 배열 형태로 저장)
     * Snowflake Arctic Embed 모델: 768차원
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String embeddingVector;

    /**
     * 문서 청크 순서 (긴 문서를 여러 청크로 나눈 경우)
     */
    @Column(nullable = false)
    private Integer chunkIndex;

    /**
     * 카테고리/분류 정보 (선택적)
     * - Wiki: 카테고리명
     * - SR: 분류(category)
     * - Survey: 기관명
     */
    @Column(length = 100)
    private String category;

    /**
     * 상태 정보 (선택적)
     * - SR: OPEN, IN_PROGRESS, RESOLVED, CLOSED
     * - Survey: PENDING, IN_PROGRESS, COMPLETED
     */
    @Column(length = 30)
    private String status;

    /**
     * 원본 리소스의 수정 시간
     * (임베딩 최신 여부 판단용)
     */
    private LocalDateTime sourceUpdatedAt;

    /**
     * 임베딩 생성 일시
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 임베딩 업데이트 일시
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

    /**
     * 리소스 유형 Enum
     */
    public enum ResourceType {
        WIKI,   // Wiki 문서
        SR,     // SR (Service Request)
        SURVEY  // OPEN API 현황조사
    }
}
