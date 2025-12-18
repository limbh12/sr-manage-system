package com.srmanagement.wiki.service;

import com.srmanagement.entity.User;
import com.srmanagement.repository.UserRepository;
import com.srmanagement.wiki.dto.WikiFileResponse;
import com.srmanagement.wiki.entity.WikiDocument;
import com.srmanagement.wiki.entity.WikiFile;
import com.srmanagement.wiki.repository.WikiDocumentRepository;
import com.srmanagement.wiki.repository.WikiFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WikiFileService {

    private final WikiFileRepository wikiFileRepository;
    private final WikiDocumentRepository wikiDocumentRepository;
    private final UserRepository userRepository;

    @Value("${wiki.upload.base-path:./data/wiki-uploads}")
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

        // DB에 파일 정보 저장
        WikiFile wikiFile = WikiFile.builder()
                .document(document)
                .originalFileName(originalFileName)
                .storedFileName(storedFileName)
                .filePath(filePath.toString())
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .type(fileType)
                .uploadedBy(user)
                .build();

        WikiFile savedFile = wikiFileRepository.save(wikiFile);
        log.info("File uploaded successfully: {}", savedFile.getId());

        return WikiFileResponse.fromEntity(savedFile);
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
