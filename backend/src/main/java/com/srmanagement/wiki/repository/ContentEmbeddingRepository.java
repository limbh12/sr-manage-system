package com.srmanagement.wiki.repository;

import com.srmanagement.wiki.entity.ContentEmbedding;
import com.srmanagement.wiki.entity.ContentEmbedding.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 통합 콘텐츠 임베딩 Repository
 */
@Repository
public interface ContentEmbeddingRepository extends JpaRepository<ContentEmbedding, Long> {

    /**
     * 특정 리소스의 임베딩 조회 (정렬 순서 지정)
     */
    List<ContentEmbedding> findByResourceTypeAndResourceIdOrderByChunkIndexAsc(
            ResourceType resourceType, Long resourceId);

    /**
     * 특정 리소스의 임베딩 조회
     */
    List<ContentEmbedding> findByResourceTypeAndResourceId(
            ResourceType resourceType, Long resourceId);

    /**
     * 특정 리소스의 임베딩 개수
     */
    long countByResourceTypeAndResourceId(ResourceType resourceType, Long resourceId);

    /**
     * 특정 리소스의 임베딩 삭제
     */
    @Modifying
    @Query("DELETE FROM ContentEmbedding e WHERE e.resourceType = :resourceType AND e.resourceId = :resourceId")
    void deleteByResourceTypeAndResourceId(
            @Param("resourceType") ResourceType resourceType,
            @Param("resourceId") Long resourceId);

    /**
     * 모든 임베딩 조회 (검색용)
     */
    @Query("SELECT e FROM ContentEmbedding e")
    List<ContentEmbedding> findAllForSearch();

    /**
     * 특정 리소스 타입의 임베딩 조회
     */
    @Query("SELECT e FROM ContentEmbedding e WHERE e.resourceType = :resourceType")
    List<ContentEmbedding> findByResourceType(@Param("resourceType") ResourceType resourceType);

    /**
     * 여러 리소스 타입의 임베딩 조회
     */
    @Query("SELECT e FROM ContentEmbedding e WHERE e.resourceType IN :resourceTypes")
    List<ContentEmbedding> findByResourceTypeIn(@Param("resourceTypes") List<ResourceType> resourceTypes);

    /**
     * 리소스 타입별 임베딩 개수
     */
    @Query("SELECT e.resourceType, COUNT(DISTINCT e.resourceId) FROM ContentEmbedding e GROUP BY e.resourceType")
    List<Object[]> countByResourceTypeGrouped();

    /**
     * 전체 임베딩된 리소스 개수 (중복 제거)
     */
    @Query("SELECT COUNT(DISTINCT CONCAT(e.resourceType, '-', e.resourceId)) FROM ContentEmbedding e")
    long countDistinctResources();

    /**
     * 특정 리소스 타입의 임베딩된 리소스 개수
     */
    @Query("SELECT COUNT(DISTINCT e.resourceId) FROM ContentEmbedding e WHERE e.resourceType = :resourceType")
    long countDistinctResourcesByType(@Param("resourceType") ResourceType resourceType);
}
