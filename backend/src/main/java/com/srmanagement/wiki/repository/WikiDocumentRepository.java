package com.srmanagement.wiki.repository;

import com.srmanagement.wiki.entity.WikiDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WikiDocumentRepository extends JpaRepository<WikiDocument, Long> {

    // 카테고리별 문서 조회
    Page<WikiDocument> findByCategoryId(Long categoryId, Pageable pageable);

    // SR 연계 문서 조회 (다대다 관계)
    @Query("SELECT wd FROM WikiDocument wd JOIN wd.srs s WHERE s.id = :srId")
    List<WikiDocument> findBySrsId(@Param("srId") Long srId);

    // 제목으로 검색
    @Query("SELECT wd FROM WikiDocument wd WHERE LOWER(wd.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<WikiDocument> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    // 제목 또는 내용으로 검색
    @Query("SELECT wd FROM WikiDocument wd WHERE LOWER(wd.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(wd.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<WikiDocument> searchByTitleOrContent(@Param("keyword") String keyword, Pageable pageable);

    // 작성자별 문서 조회
    Page<WikiDocument> findByCreatedById(Long userId, Pageable pageable);

    // 최근 수정된 문서 조회
    @Query("SELECT wd FROM WikiDocument wd ORDER BY wd.updatedAt DESC")
    Page<WikiDocument> findRecentlyUpdated(Pageable pageable);

    // 인기 문서 조회 (조회수 높은 순)
    @Query("SELECT wd FROM WikiDocument wd ORDER BY wd.viewCount DESC")
    Page<WikiDocument> findPopular(Pageable pageable);

    // ID로 문서 조회 (연관 엔티티 페치 조인)
    // Note: 여러 컬렉션을 한번에 fetch할 수 없으므로 files만 fetch
    @Query("SELECT DISTINCT wd FROM WikiDocument wd " +
           "LEFT JOIN FETCH wd.category " +
           "LEFT JOIN FETCH wd.createdBy " +
           "LEFT JOIN FETCH wd.updatedBy " +
           "LEFT JOIN FETCH wd.files " +
           "WHERE wd.id = :id")
    Optional<WikiDocument> findByIdWithDetails(@Param("id") Long id);

    // SR 목록 별도 조회
    @Query("SELECT DISTINCT wd FROM WikiDocument wd " +
           "LEFT JOIN FETCH wd.srs " +
           "WHERE wd.id = :id")
    Optional<WikiDocument> findByIdWithSrs(@Param("id") Long id);

    // 버전 목록 별도 조회
    @Query("SELECT DISTINCT wd FROM WikiDocument wd " +
           "LEFT JOIN FETCH wd.versions " +
           "WHERE wd.id = :id")
    Optional<WikiDocument> findByIdWithVersions(@Param("id") Long id);
}
