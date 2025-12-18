package com.srmanagement.wiki.repository;

import com.srmanagement.wiki.entity.WikiVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WikiVersionRepository extends JpaRepository<WikiVersion, Long> {

    // 특정 문서의 모든 버전 조회 (최신순)
    List<WikiVersion> findByDocumentIdOrderByVersionDesc(Long documentId);

    // 특정 문서의 버전 이력 (페이징)
    Page<WikiVersion> findByDocumentIdOrderByVersionDesc(Long documentId, Pageable pageable);

    // 특정 문서의 특정 버전 조회
    Optional<WikiVersion> findByDocumentIdAndVersion(Long documentId, Integer version);

    // 특정 문서의 최신 버전 번호 조회
    @Query("SELECT MAX(v.version) FROM WikiVersion v WHERE v.document.id = :documentId")
    Optional<Integer> findLatestVersionNumber(@Param("documentId") Long documentId);

    // 특정 문서의 최신 버전 조회
    @Query("SELECT v FROM WikiVersion v WHERE v.document.id = :documentId ORDER BY v.version DESC LIMIT 1")
    Optional<WikiVersion> findLatestVersion(@Param("documentId") Long documentId);
}
