package com.srmanagement.wiki.repository;

import com.srmanagement.wiki.entity.WikiFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WikiFileRepository extends JpaRepository<WikiFile, Long> {

    // 특정 문서의 모든 파일 조회
    List<WikiFile> findByDocumentId(Long documentId);

    // 특정 문서의 특정 타입 파일 조회
    List<WikiFile> findByDocumentIdAndType(Long documentId, WikiFile.FileType type);

    // 파일명으로 검색
    List<WikiFile> findByOriginalFileNameContainingIgnoreCase(String fileName);

    // 저장된 파일명으로 조회
    Optional<WikiFile> findByStoredFileName(String storedFileName);

    // 특정 사용자가 업로드한 파일 조회
    List<WikiFile> findByUploadedById(Long userId);

    // 파일 경로로 조회
    Optional<WikiFile> findByFilePath(String filePath);

    // 문서별 이미지 파일만 조회
    @Query("SELECT f FROM WikiFile f WHERE f.document.id = :documentId AND f.type = 'IMAGE' ORDER BY f.uploadedAt ASC")
    List<WikiFile> findImagesByDocumentId(@Param("documentId") Long documentId);

    // 변환 대기 중인 PDF 파일 조회
    @Query("SELECT f FROM WikiFile f WHERE f.type = 'DOCUMENT' AND f.conversionStatus = 'PENDING' ORDER BY f.uploadedAt ASC")
    List<WikiFile> findPendingConversions();

    // 변환 상태별 파일 조회
    List<WikiFile> findByConversionStatus(WikiFile.ConversionStatus status);

    // 특정 문서의 PDF 파일 조회
    @Query("SELECT f FROM WikiFile f WHERE f.document.id = :documentId AND f.type = 'DOCUMENT' AND f.mimeType = 'application/pdf'")
    Optional<WikiFile> findPdfByDocumentId(@Param("documentId") Long documentId);
}
