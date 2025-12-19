package com.srmanagement.wiki.service;

import com.srmanagement.entity.User;
import com.srmanagement.repository.UserRepository;
import com.srmanagement.wiki.dto.WikiFileResponse;
import com.srmanagement.wiki.entity.WikiDocument;
import com.srmanagement.wiki.entity.WikiFile;
import com.srmanagement.wiki.entity.WikiCategory;
import com.srmanagement.wiki.entity.WikiVersion;
import com.srmanagement.wiki.repository.WikiDocumentRepository;
import com.srmanagement.wiki.repository.WikiFileRepository;
import com.srmanagement.wiki.repository.WikiCategoryRepository;
import com.srmanagement.wiki.repository.WikiVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WikiFileService {

    private final WikiFileRepository wikiFileRepository;
    private final WikiDocumentRepository wikiDocumentRepository;
    private final WikiCategoryRepository wikiCategoryRepository;
    private final UserRepository userRepository;
    private final PdfConversionService pdfConversionService;
    private final WikiVersionRepository wikiVersionRepository;

    @Value("${wiki.upload.base-path:./uploads}")
    private String uploadBasePath;

    @Value("${wiki.upload.max-file-size:20971520}")
    private Long maxFileSize;

    @Transactional
    public WikiFileResponse uploadFile(MultipartFile file, Long documentId, Long userId) throws IOException {
        log.info("Uploading file: {} for document: {}", file.getOriginalFilename(), documentId);

        // 파일 크기 검증
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("파일 크기가 허용된 최대 크기를 초과했습니다");
        }

        // 파일명 및 확장자 검증
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isEmpty()) {
            throw new RuntimeException("파일명이 유효하지 않습니다");
        }

        // 문서 조회
        WikiDocument document = null;
        if (documentId != null) {
            document = wikiDocumentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다"));
        }

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // 저장 경로 생성
        Path uploadPath = Paths.get(uploadBasePath);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 고유 파일명 생성
        String fileExtension = getFileExtension(originalFileName);
        String storedFileName = UUID.randomUUID().toString() + fileExtension;
        Path filePath = uploadPath.resolve(storedFileName);

        // 파일 저장
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // 파일 타입 결정
        WikiFile.FileType fileType = determineFileType(file.getContentType(), fileExtension);

        // 변환 상태 결정
        WikiFile.ConversionStatus conversionStatus = WikiFile.ConversionStatus.NOT_APPLICABLE;
        if (fileType == WikiFile.FileType.DOCUMENT && "application/pdf".equals(file.getContentType())) {
            conversionStatus = WikiFile.ConversionStatus.PENDING;
        }

        // DB에 파일 정보 저장
        WikiFile wikiFile = WikiFile.builder()
                .document(document)
                .originalFileName(originalFileName)
                .storedFileName(storedFileName)
                .filePath(filePath.toString())
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .type(fileType)
                .conversionStatus(conversionStatus)
                .uploadedBy(user)
                .build();

        WikiFile savedFile = wikiFileRepository.save(wikiFile);
        log.info("File uploaded successfully: {}", savedFile.getId());

        return WikiFileResponse.fromEntity(savedFile);
    }

    /**
     * PDF를 마크다운으로 변환하고 Wiki 문서 생성
     */
    @Transactional
    public WikiDocument convertPdfToWikiDocument(Long fileId, Long userId, Long categoryId) {
        log.info("Starting PDF conversion for file: {}, categoryId: {}", fileId, categoryId);

        // 파일 조회
        WikiFile wikiFile = wikiFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다"));

        // PDF 파일인지 확인
        if (!"application/pdf".equals(wikiFile.getMimeType())) {
            throw new RuntimeException("PDF 파일만 변환할 수 있습니다");
        }

        // 변환 상태 업데이트
        wikiFile.setConversionStatus(WikiFile.ConversionStatus.PROCESSING);
        wikiFileRepository.save(wikiFile);

        try {
            // PDF를 마크다운으로 변환 (페이지별 이미지 마커 포함)
            PdfConversionService.PdfConversionResult conversionResult =
                    pdfConversionService.convertPdfToMarkdownWithImages(
                            wikiFile.getFilePath(),
                            wikiFile.getOriginalFileName()
                    );

            String markdown = conversionResult.getMarkdown();

            // PDF에서 이미지 추출
            List<PdfConversionService.ExtractedImage> extractedImages = new ArrayList<>();
            java.util.Map<Integer, List<String>> pageImageLinks = new java.util.HashMap<>();

            try {
                // 이미지 저장 디렉토리 생성
                Path pdfPath = Paths.get(wikiFile.getFilePath());
                String imageDir = pdfPath.getParent().toString() + File.separator + "images_" + wikiFile.getId();

                extractedImages = pdfConversionService.extractImages(wikiFile.getFilePath(), imageDir);
                log.info("PDF에서 {} 개의 이미지 추출됨", extractedImages.size());
            } catch (Exception e) {
                log.warn("PDF 이미지 추출 실패 (계속 진행): {}", e.getMessage());
            }

            // Wiki 문서 생성 또는 업데이트
            WikiDocument document;
            boolean isNewDocument = (wikiFile.getDocument() == null);

            if (!isNewDocument) {
                // 기존 문서 업데이트
                document = wikiFile.getDocument();
                document.setContent(markdown);
                document.setUpdatedBy(wikiFile.getUploadedBy());
            } else {
                // 새 문서 생성
                String title = wikiFile.getOriginalFileName().replaceAll("\\.pdf$", "");

                // 카테고리 조회
                WikiCategory category = null;
                if (categoryId != null) {
                    category = wikiCategoryRepository.findById(categoryId)
                            .orElse(null);
                }

                document = WikiDocument.builder()
                        .title(title)
                        .content(markdown)
                        .category(category)
                        .createdBy(wikiFile.getUploadedBy())
                        .updatedBy(wikiFile.getUploadedBy())
                        .build();
            }

            WikiDocument savedDocument = wikiDocumentRepository.save(document);

            // 추출된 이미지를 WikiFile로 등록하고 페이지별로 그룹화
            for (PdfConversionService.ExtractedImage extractedImage : extractedImages) {
                try {
                    WikiFile imageFile = WikiFile.builder()
                            .document(savedDocument)
                            .originalFileName(extractedImage.getFilename())
                            .storedFileName(extractedImage.getFilename())
                            .filePath(extractedImage.getFilepath())
                            .fileSize(extractedImage.getFileSize())
                            .mimeType("image/png")
                            .type(WikiFile.FileType.IMAGE)
                            .conversionStatus(WikiFile.ConversionStatus.NOT_APPLICABLE)
                            .uploadedBy(wikiFile.getUploadedBy())
                            .build();

                    WikiFile savedImageFile = wikiFileRepository.save(imageFile);
                    log.info("이미지 파일 등록 완료: {}", extractedImage.getFilename());

                    // 페이지별로 이미지 링크 그룹화
                    int pageNum = extractedImage.getPageNumber();
                    pageImageLinks.putIfAbsent(pageNum, new ArrayList<>());

                    String imageMarkdown = String.format("![%s - Page %d](%s)",
                            extractedImage.getFilename(),
                            pageNum,
                            "/api/wiki/files/" + savedImageFile.getId());
                    pageImageLinks.get(pageNum).add(imageMarkdown);

                } catch (Exception e) {
                    log.error("이미지 파일 등록 실패: {}", extractedImage.getFilename(), e);
                }
            }

            // 마커를 실제 이미지 링크로 대체
            for (int pageNum = 1; pageNum <= conversionResult.getTotalPages(); pageNum++) {
                String marker = "{{IMAGES_PAGE_" + pageNum + "}}";
                List<String> images = pageImageLinks.get(pageNum);

                if (images != null && !images.isEmpty()) {
                    String imageSection = "\n\n### 📷 이미지\n\n" + String.join("\n\n", images) + "\n";
                    markdown = markdown.replace(marker, imageSection);
                } else {
                    // 이미지가 없으면 마커 제거
                    markdown = markdown.replace(marker, "");
                }
            }

            // 최종 마크다운으로 문서 업데이트
            savedDocument.setContent(markdown);
            savedDocument = wikiDocumentRepository.save(savedDocument);

            // 새 문서인 경우 버전 1 생성
            if (isNewDocument) {
                WikiVersion firstVersion = WikiVersion.builder()
                        .document(savedDocument)
                        .version(1)
                        .content(markdown)
                        .changeSummary("PDF 변환으로 생성")
                        .createdBy(wikiFile.getUploadedBy())
                        .build();
                wikiVersionRepository.save(firstVersion);
                log.info("버전 1 생성 완료: document={}", savedDocument.getId());
            }

            // 파일과 문서 연결
            wikiFile.setDocument(savedDocument);
            wikiFile.setConversionStatus(WikiFile.ConversionStatus.COMPLETED);
            wikiFile.setConvertedAt(java.time.LocalDateTime.now());
            wikiFileRepository.save(wikiFile);

            log.info("PDF conversion completed successfully: file={}, document={}", fileId, savedDocument.getId());
            return savedDocument;

        } catch (Exception e) {
            log.error("PDF conversion failed for file: {}", fileId, e);

            // 변환 실패 상태 업데이트
            wikiFile.setConversionStatus(WikiFile.ConversionStatus.FAILED);
            wikiFile.setConversionErrorMessage(e.getMessage());
            wikiFileRepository.save(wikiFile);

            throw new RuntimeException("PDF 변환 실패: " + e.getMessage(), e);
        }
    }

    /**
     * PDF 업로드 및 자동 변환
     */
    @Transactional
    public WikiDocument uploadAndConvertPdf(MultipartFile file, Long categoryId, Long userId) throws IOException {
        log.info("Uploading and converting PDF: {}, categoryId: {}", file.getOriginalFilename(), categoryId);

        // 파일 업로드
        WikiFileResponse uploadedFile = uploadFile(file, null, userId);

        // PDF 변환 및 Wiki 문서 생성
        return convertPdfToWikiDocument(uploadedFile.getId(), userId, categoryId);
    }

    /**
     * 대기 중인 PDF 변환 처리
     */
    @Transactional
    public void processPendingConversions() {
        List<WikiFile> pendingFiles = wikiFileRepository.findPendingConversions();
        log.info("Processing {} pending PDF conversions", pendingFiles.size());

        for (WikiFile file : pendingFiles) {
            try {
                // 대기 중인 변환은 카테고리 없이 처리
                convertPdfToWikiDocument(file.getId(), file.getUploadedBy().getId(), null);
            } catch (Exception e) {
                log.error("Failed to convert PDF: {}", file.getId(), e);
            }
        }
    }

    @Transactional(readOnly = true)
    public Resource downloadFile(Long fileId) throws IOException {
        WikiFile wikiFile = wikiFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다"));

        Path filePath = Paths.get(wikiFile.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("파일을 읽을 수 없습니다");
        }

        return resource;
    }

    @Transactional(readOnly = true)
    public WikiFileResponse getFile(Long fileId) {
        WikiFile wikiFile = wikiFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다"));
        return WikiFileResponse.fromEntity(wikiFile);
    }

    @Transactional(readOnly = true)
    public List<WikiFileResponse> getFilesByDocument(Long documentId) {
        return wikiFileRepository.findByDocumentId(documentId).stream()
                .map(WikiFileResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteFile(Long fileId) throws IOException {
        log.info("Deleting file: {}", fileId);

        WikiFile wikiFile = wikiFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다"));

        // 파일 시스템에서 삭제
        Path filePath = Paths.get(wikiFile.getFilePath());
        Files.deleteIfExists(filePath);

        // DB에서 삭제
        wikiFileRepository.deleteById(fileId);
        log.info("File deleted successfully: {}", fileId);
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex);
        }
        return "";
    }

    private WikiFile.FileType determineFileType(String contentType, String extension) {
        if (contentType != null) {
            if (contentType.startsWith("image/")) {
                return WikiFile.FileType.IMAGE;
            } else if (contentType.equals("application/pdf") ||
                       contentType.contains("document") ||
                       contentType.contains("word")) {
                return WikiFile.FileType.DOCUMENT;
            }
        }

        // contentType이 없거나 확실하지 않은 경우 확장자로 판단
        String lowerExt = extension.toLowerCase();
        if (lowerExt.matches("\\.(png|jpg|jpeg|gif|bmp|svg)")) {
            return WikiFile.FileType.IMAGE;
        } else if (lowerExt.matches("\\.(pdf|doc|docx|xls|xlsx|ppt|pptx)")) {
            return WikiFile.FileType.DOCUMENT;
        }

        return WikiFile.FileType.ATTACHMENT;
    }
}
