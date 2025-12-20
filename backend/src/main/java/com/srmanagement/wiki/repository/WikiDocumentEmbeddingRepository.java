package com.srmanagement.wiki.repository;

import com.srmanagement.wiki.entity.WikiDocumentEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Wiki 문서 임베딩 Repository
 */
@Repository
public interface WikiDocumentEmbeddingRepository extends JpaRepository<WikiDocumentEmbedding, Long> {

    /**
     * 문서 ID로 모든 임베딩 조회 (청크 순서대로)
     */
    List<WikiDocumentEmbedding> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    /**
     * 문서 ID로 임베딩 개수 조회
     */
    long countByDocumentId(Long documentId);

    /**
     * 문서 ID로 임베딩 삭제
     */
    @Modifying
    @Query("DELETE FROM WikiDocumentEmbedding we WHERE we.documentId = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);

    /**
     * 카테고리 ID로 임베딩 조회
     */
    List<WikiDocumentEmbedding> findByCategoryId(Long categoryId);

    /**
     * 모든 임베딩 조회 (검색용)
     */
    @Query("SELECT we FROM WikiDocumentEmbedding we ORDER BY we.documentId, we.chunkIndex")
    List<WikiDocumentEmbedding> findAllForSearch();
}
