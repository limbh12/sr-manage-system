package com.srmanagement.wiki.repository;

import com.srmanagement.wiki.entity.WikiCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WikiCategoryRepository extends JpaRepository<WikiCategory, Long> {

    // 최상위 카테고리 조회 (parent가 null인 카테고리)
    List<WikiCategory> findByParentIsNullOrderBySortOrderAsc();

    // 특정 부모 카테고리의 자식 카테고리 조회
    List<WikiCategory> findByParentIdOrderBySortOrderAsc(Long parentId);

    // 카테고리 이름으로 검색
    List<WikiCategory> findByNameContainingIgnoreCase(String name);

    // 카테고리 상세 조회 (자식 카테고리 포함)
    @Query("SELECT c FROM WikiCategory c LEFT JOIN FETCH c.children WHERE c.id = :id")
    Optional<WikiCategory> findByIdWithChildren(@Param("id") Long id);

    // 전체 카테고리 트리 조회 (계층 구조)
    @Query("SELECT c FROM WikiCategory c LEFT JOIN FETCH c.parent ORDER BY c.sortOrder ASC")
    List<WikiCategory> findAllWithParent();
}
