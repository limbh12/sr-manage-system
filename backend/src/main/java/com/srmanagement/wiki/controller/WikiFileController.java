package com.srmanagement.wiki.controller;

import com.srmanagement.entity.User;
import com.srmanagement.exception.CustomException;
import com.srmanagement.repository.UserRepository;
import com.srmanagement.wiki.dto.WikiDocumentResponse;
import com.srmanagement.wiki.dto.WikiFileResponse;
import com.srmanagement.wiki.entity.WikiDocument;
import com.srmanagement.wiki.service.WikiFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/wiki/files")
@RequiredArgsConstructor
public class WikiFileController {

    private final WikiFileService wikiFileService;
    private final UserRepository userRepository;

    @PostMapping("/upload")
    public ResponseEntity<WikiFileResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentId", required = false) Long documentId,
            Authentication authentication) throws IOException {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
        WikiFileResponse response = wikiFileService.uploadFile(file, documentId, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) throws IOException {
        Resource resource = wikiFileService.downloadFile(fileId);
        WikiFileResponse fileInfo = wikiFileService.getFile(fileId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileInfo.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + fileInfo.getOriginalFileName() + "\"")
                .body(resource);
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> forceDownloadFile(@PathVariable Long fileId) throws IOException {
        Resource resource = wikiFileService.downloadFile(fileId);
        WikiFileResponse fileInfo = wikiFileService.getFile(fileId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileInfo.getOriginalFileName() + "\"")
                .body(resource);
    }

    @GetMapping("/{fileId}/info")
    public ResponseEntity<WikiFileResponse> getFileInfo(@PathVariable Long fileId) {
        WikiFileResponse response = wikiFileService.getFile(fileId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<WikiFileResponse>> getFilesByDocument(@PathVariable Long documentId) {
        List<WikiFileResponse> files = wikiFileService.getFilesByDocument(documentId);
        return ResponseEntity.ok(files);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long fileId) throws IOException {
        wikiFileService.deleteFile(fileId);
        return ResponseEntity.noContent().build();
    }

    /**
     * PDF 업로드 및 자동 마크다운 변환
     */
    @PostMapping("/upload-pdf")
    public ResponseEntity<WikiDocumentResponse> uploadAndConvertPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            Authentication authentication) throws IOException {

        // PDF 파일인지 확인
        if (!"application/pdf".equals(file.getContentType())) {
            return ResponseEntity.badRequest().build();
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        WikiDocument document = wikiFileService.uploadAndConvertPdf(file, categoryId, user.getId());
        WikiDocumentResponse response = WikiDocumentResponse.fromEntity(document);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 파일 ID로 PDF를 마크다운으로 변환
     */
    @PostMapping("/{fileId}/convert")
    public ResponseEntity<WikiDocumentResponse> convertPdfToMarkdown(
            @PathVariable Long fileId,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            Authentication authentication) {

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        WikiDocument document = wikiFileService.convertPdfToWikiDocument(fileId, user.getId(), categoryId);
        WikiDocumentResponse response = WikiDocumentResponse.fromEntity(document);

        return ResponseEntity.ok(response);
    }

    /**
     * 대기 중인 PDF 변환 작업 처리 (관리자 전용)
     */
    @PostMapping("/process-pending")
    public ResponseEntity<Void> processPendingConversions(Authentication authentication) {
        // TODO: 관리자 권한 체크
        wikiFileService.processPendingConversions();
        return ResponseEntity.ok().build();
    }
}
