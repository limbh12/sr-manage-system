package com.srmanagement.wiki.repository;

import com.srmanagement.wiki.entity.AiSearchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 검색 이력 Repository
 */
@Repository
public interface AiSearchHistoryRepository extends JpaRepository<AiSearchHistory, Long> {

    /**
     * 특정 사용자의 검색 이력 페이징 조회
     */
    Page<AiSearchHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 전체 최근 검색 이력 조회 (관리자용)
     */
    Page<AiSearchHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 특정 사용자의 최근 N개 검색 이력 조회
     */
    @Query("SELECT h FROM AiSearchHistory h WHERE h.user.id = :userId ORDER BY h.createdAt DESC")
    List<AiSearchHistory> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 특정 기간 이전의 검색 이력 삭제
     */
    @Modifying
    @Query("DELETE FROM AiSearchHistory h WHERE h.createdAt < :before")
    int deleteByCreatedAtBefore(@Param("before") LocalDateTime before);

    /**
     * 특정 사용자의 검색 이력 개수
     */
    long countByUserId(Long userId);

    /**
     * 질문으로 검색 (부분 일치)
     */
    @Query("SELECT h FROM AiSearchHistory h WHERE h.user.id = :userId AND LOWER(h.question) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY h.createdAt DESC")
    List<AiSearchHistory> searchByQuestion(@Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);
}
