package com.srmanagement.wiki.controller;

import com.srmanagement.entity.User;
import com.srmanagement.exception.CustomException;
import com.srmanagement.repository.UserRepository;
import com.srmanagement.wiki.dto.WikiDocumentResponse;
import com.srmanagement.wiki.dto.WikiFileResponse;
import com.srmanagement.wiki.entity.WikiDocument;
import com.srmanagement.wiki.service.StructureEnhancementService;
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
    private final StructureEnhancementService structureEnhancementService;

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

        // MIME 타입 설정 (null일 경우 기본값 사용)
        String mimeType = fileInfo.getMimeType();
        if (mimeType == null || mimeType.isEmpty()) {
            mimeType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
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

    /**
     * PDF 업로드 및 AI 구조 보정 적용 변환
     * - 표(Table) 인식 및 마크다운 변환
     * - 수식(LaTeX) 인식 및 변환
     * - AI 기반 구조 분석
     */
    @PostMapping("/upload-pdf-enhanced")
    public ResponseEntity<EnhancedPdfConversionResponse> uploadAndConvertPdfWithAiEnhancement(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "enableAiEnhancement", defaultValue = "true") boolean enableAiEnhancement,
            Authentication authentication) throws IOException {

        // PDF 파일인지 확인
        if (!"application/pdf".equals(file.getContentType())) {
            return ResponseEntity.badRequest().build();
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        WikiDocument document = wikiFileService.uploadAndConvertPdfWithAiEnhancement(
                file, categoryId, user.getId(), enableAiEnhancement);

        WikiDocumentResponse documentResponse = WikiDocumentResponse.fromEntity(document);

        EnhancedPdfConversionResponse response = EnhancedPdfConversionResponse.builder()
                .document(documentResponse)
                .aiEnhanced(enableAiEnhancement)
                .message(enableAiEnhancement ? "AI 구조 보정이 적용되었습니다" : "기본 변환이 수행되었습니다")
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Pandoc 상태 확인 (Pandoc 설치 여부)
     */
    @GetMapping("/pandoc-status")
    public ResponseEntity<PandocStatusResponse> checkPandocStatus() {
        boolean available = structureEnhancementService.isPandocAvailable();

        return ResponseEntity.ok(PandocStatusResponse.builder()
                .available(available)
                .message(available ? "Pandoc이 설치되어 있습니다" : "Pandoc이 설치되어 있지 않습니다")
                .build());
    }

    /**
     * 마크다운 텍스트에 AI 구조 보정 적용 (테스트용)
     */
    @PostMapping("/enhance-markdown")
    public ResponseEntity<StructureEnhancementService.EnhancementResult> enhanceMarkdown(
            @RequestBody EnhanceMarkdownRequest request) {

        StructureEnhancementService.EnhancementResult result =
                structureEnhancementService.enhanceMarkdown(
                        request.getMarkdown(),
                        request.getFileName() != null ? request.getFileName() : "test.pdf"
                );

        return ResponseEntity.ok(result);
    }

    /**
     * Vision 모델을 사용하여 이미지에서 복잡한 표 추출
     * - 셀 병합, 중첩 표, 다중 헤더 등 복잡한 구조 지원
     */
    @PostMapping("/extract-table-from-image")
    public ResponseEntity<StructureEnhancementService.VisionTableResult> extractTableFromImage(
            @RequestParam("file") MultipartFile file) throws IOException {

        // 임시 파일로 저장
        String tempPath = System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename();
        java.io.File tempFile = new java.io.File(tempPath);
        file.transferTo(tempFile);

        try {
            StructureEnhancementService.VisionTableResult result =
                    structureEnhancementService.extractTableFromImage(tempPath);
            return ResponseEntity.ok(result);
        } finally {
            // 임시 파일 삭제
            tempFile.delete();
        }
    }

    /**
     * 표 복잡도 분석
     * - 텍스트 기반 처리 가능 여부 판단
     * - Vision 처리 권장 여부 반환
     */
    @PostMapping("/analyze-table-complexity")
    public ResponseEntity<StructureEnhancementService.TableComplexityResult> analyzeTableComplexity(
            @RequestBody AnalyzeTableRequest request) {

        StructureEnhancementService.TableComplexityResult result =
                structureEnhancementService.analyzeTableComplexity(request.getTableText());

        return ResponseEntity.ok(result);
    }

    /**
     * Vision 기능 상태 확인
     */
    @GetMapping("/vision-status")
    public ResponseEntity<VisionStatusResponse> checkVisionStatus() {
        // application.yml에서 vision-enabled 설정 확인
        // 실제로는 @Value로 주입받아야 하지만, 여기서는 서비스 메서드 호출로 확인
        StructureEnhancementService.VisionTableResult testResult =
                structureEnhancementService.extractTableFromImage("__test__");

        boolean enabled = !testResult.getMessage().contains("비활성화");

        return ResponseEntity.ok(VisionStatusResponse.builder()
                .enabled(enabled)
                .message(enabled ? "Vision 기반 표 추출이 활성화되어 있습니다" :
                        "Vision 기반 표 추출이 비활성화되어 있습니다. application.yml에서 wiki.structure-enhancement.vision-enabled=true로 설정하세요.")
                .build());
    }

    // ========== DTO 클래스들 ==========

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EnhancedPdfConversionResponse {
        private WikiDocumentResponse document;
        private boolean aiEnhanced;
        private int tablesFound;
        private int formulasFound;
        private boolean usedPandoc;
        private long processingTimeMs;
        private String message;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PandocStatusResponse {
        private boolean available;
        private String message;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EnhanceMarkdownRequest {
        private String markdown;
        private String fileName;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AnalyzeTableRequest {
        private String tableText;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class VisionStatusResponse {
        private boolean enabled;
        private String message;
    }
}
