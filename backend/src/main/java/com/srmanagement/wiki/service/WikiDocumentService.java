package com.srmanagement.wiki.service;

import com.srmanagement.entity.Sr;
import com.srmanagement.entity.User;
import com.srmanagement.repository.SrRepository;
import com.srmanagement.repository.UserRepository;
import com.srmanagement.wiki.dto.WikiDocumentRequest;
import com.srmanagement.wiki.dto.WikiDocumentResponse;
import com.srmanagement.wiki.entity.WikiCategory;
import com.srmanagement.wiki.entity.WikiDocument;
import com.srmanagement.wiki.entity.WikiVersion;
import com.srmanagement.wiki.repository.WikiCategoryRepository;
import com.srmanagement.wiki.repository.WikiDocumentRepository;
import com.srmanagement.wiki.repository.WikiVersionRepository;
import com.srmanagement.wiki.util.MarkdownTocGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WikiDocumentService {

    private final WikiDocumentRepository wikiDocumentRepository;
    private final WikiVersionRepository wikiVersionRepository;
    private final WikiCategoryRepository wikiCategoryRepository;
    private final UserRepository userRepository;
    private final SrRepository srRepository;

    @Transactional
    public WikiDocumentResponse createDocument(WikiDocumentRequest request, Long userId) {
        log.info("Creating wiki document: {} by user: {}", request.getTitle(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // 목차 자동 생성 (옵션)
        String content = request.getContent();
        boolean shouldGenerateToc = request.getGenerateToc() != null && request.getGenerateToc();
        if (shouldGenerateToc) {
            content = MarkdownTocGenerator.generateTableOfContents(content, true);
            log.info("목차 자동 생성 완료");
        }

        WikiDocument document = WikiDocument.builder()
                .title(request.getTitle())
                .content(content)
                .createdBy(user)
                .build();

        // 카테고리 설정
        if (request.getCategoryId() != null) {
            WikiCategory category = wikiCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다"));
            document.setCategory(category);
        }

        // SR 연계 설정 (다대다)
        if (request.getSrIds() != null && !request.getSrIds().isEmpty()) {
            List<Sr> srs = srRepository.findAllById(request.getSrIds());
            if (srs.size() != request.getSrIds().size()) {
                throw new RuntimeException("일부 SR을 찾을 수 없습니다");
            }
            document.getSrs().clear();
            document.getSrs().addAll(srs);
        }

        WikiDocument savedDocument = wikiDocumentRepository.save(document);

        // 최초 버전 생성
        WikiVersion firstVersion = WikiVersion.builder()
                .document(savedDocument)
                .version(1)
                .content(content)  // 목차가 포함된 content 사용
                .changeSummary("최초 생성")
                .createdBy(user)
                .build();
        wikiVersionRepository.save(firstVersion);

        log.info("Wiki document created with ID: {}", savedDocument.getId());
        return WikiDocumentResponse.fromEntity(savedDocument);
    }

    @Transactional
    public WikiDocumentResponse updateDocument(Long id, WikiDocumentRequest request, Long userId) {
        log.info("Updating wiki document: {} by user: {}", id, userId);

        WikiDocument document = wikiDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // 목차 자동 생성 (옵션)
        String content = request.getContent();
        boolean shouldGenerateToc = request.getGenerateToc() != null && request.getGenerateToc();
        if (shouldGenerateToc) {
            content = MarkdownTocGenerator.generateTableOfContents(content, true);
            log.info("목차 자동 생성 완료");
        }

        // 내용이 변경되었는지 확인
        boolean contentChanged = !document.getContent().equals(content);

        document.setTitle(request.getTitle());
        document.setContent(content);
        document.setUpdatedBy(user);

        // 카테고리 변경
        if (request.getCategoryId() != null) {
            WikiCategory category = wikiCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다"));
            document.setCategory(category);
        } else {
            document.setCategory(null);
        }

        // SR 연계 변경 (다대다)
        document.getSrs().clear();
        if (request.getSrIds() != null && !request.getSrIds().isEmpty()) {
            List<Sr> srs = srRepository.findAllById(request.getSrIds());
            if (srs.size() != request.getSrIds().size()) {
                throw new RuntimeException("일부 SR을 찾을 수 없습니다");
            }
            document.getSrs().addAll(srs);
        }

        WikiDocument savedDocument = wikiDocumentRepository.save(document);

        // 내용이 변경된 경우 새 버전 생성
        if (contentChanged) {
            Integer latestVersion = wikiVersionRepository.findLatestVersionNumber(id).orElse(0);
            WikiVersion newVersion = WikiVersion.builder()
                    .document(savedDocument)
                    .version(latestVersion + 1)
                    .content(content)  // 목차가 포함된 content 사용
                    .changeSummary(request.getChangeSummary() != null ? request.getChangeSummary() : "내용 수정")
                    .createdBy(user)
                    .build();
            wikiVersionRepository.save(newVersion);
            log.info("Created new version {} for document {}", newVersion.getVersion(), id);
        }

        log.info("Wiki document updated: {}", id);
        return WikiDocumentResponse.fromEntity(savedDocument);
    }

    @Transactional
    public void deleteDocument(Long id) {
        log.info("Deleting wiki document: {}", id);

        if (!wikiDocumentRepository.existsById(id)) {
            throw new RuntimeException("문서를 찾을 수 없습니다");
        }

        wikiDocumentRepository.deleteById(id);
        log.info("Wiki document deleted: {}", id);
    }

    @Transactional(readOnly = true)
    public WikiDocumentResponse getDocument(Long id) {
        WikiDocument document = wikiDocumentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다"));

        // SRs와 Versions는 별도로 fetch (MultipleBagFetchException 방지)
        wikiDocumentRepository.findByIdWithSrs(id);
        wikiDocumentRepository.findByIdWithVersions(id);

        return WikiDocumentResponse.fromEntity(document);
    }

    @Transactional
    public WikiDocumentResponse getDocumentAndIncrementViewCount(Long id) {
        WikiDocument document = wikiDocumentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다"));

        // SRs와 Versions는 별도로 fetch (MultipleBagFetchException 방지)
        wikiDocumentRepository.findByIdWithSrs(id);
        wikiDocumentRepository.findByIdWithVersions(id);

        document.incrementViewCount();
        wikiDocumentRepository.save(document);

        return WikiDocumentResponse.fromEntity(document);
    }

    @Transactional(readOnly = true)
    public Page<WikiDocumentResponse> getAllDocuments(Pageable pageable) {
        return wikiDocumentRepository.findAll(pageable)
                .map(WikiDocumentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<WikiDocumentResponse> getDocumentsByCategory(Long categoryId, Pageable pageable) {
        return wikiDocumentRepository.findByCategoryId(categoryId, pageable)
                .map(WikiDocumentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<WikiDocumentResponse> getDocumentsBySr(Long srId) {
        return wikiDocumentRepository.findBySrsId(srId).stream()
                .map(WikiDocumentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<WikiDocumentResponse> searchDocuments(String keyword, Pageable pageable) {
        return wikiDocumentRepository.searchByTitleOrContent(keyword, pageable)
                .map(WikiDocumentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<WikiDocumentResponse> getRecentlyUpdated(Pageable pageable) {
        return wikiDocumentRepository.findRecentlyUpdated(pageable)
                .map(WikiDocumentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<WikiDocumentResponse> getPopularDocuments(Pageable pageable) {
        return wikiDocumentRepository.findPopular(pageable)
                .map(WikiDocumentResponse::fromEntity);
    }
}
