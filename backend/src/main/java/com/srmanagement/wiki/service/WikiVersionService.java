package com.srmanagement.wiki.service;

import com.srmanagement.entity.User;
import com.srmanagement.repository.UserRepository;
import com.srmanagement.wiki.dto.WikiVersionResponse;
import com.srmanagement.wiki.entity.WikiDocument;
import com.srmanagement.wiki.entity.WikiVersion;
import com.srmanagement.wiki.repository.WikiDocumentRepository;
import com.srmanagement.wiki.repository.WikiVersionRepository;
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
public class WikiVersionService {

    private final WikiVersionRepository wikiVersionRepository;
    private final WikiDocumentRepository wikiDocumentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<WikiVersionResponse> getDocumentVersions(Long documentId) {
        return wikiVersionRepository.findByDocumentIdOrderByVersionDesc(documentId).stream()
                .map(WikiVersionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<WikiVersionResponse> getDocumentVersionsPaged(Long documentId, Pageable pageable) {
        return wikiVersionRepository.findByDocumentIdOrderByVersionDesc(documentId, pageable)
                .map(WikiVersionResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public WikiVersionResponse getVersion(Long documentId, Integer version) {
        WikiVersion wikiVersion = wikiVersionRepository.findByDocumentIdAndVersion(documentId, version)
                .orElseThrow(() -> new RuntimeException("버전을 찾을 수 없습니다"));

        return WikiVersionResponse.fromEntity(wikiVersion);
    }

    @Transactional(readOnly = true)
    public WikiVersionResponse getLatestVersion(Long documentId) {
        WikiVersion latestVersion = wikiVersionRepository.findLatestVersion(documentId)
                .orElseThrow(() -> new RuntimeException("버전을 찾을 수 없습니다"));

        return WikiVersionResponse.fromEntity(latestVersion);
    }

    @Transactional
    public void rollbackToVersion(Long documentId, Integer version, Long userId) {
        log.info("Rolling back document {} to version {}", documentId, version);

        WikiDocument document = wikiDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다"));

        WikiVersion targetVersion = wikiVersionRepository.findByDocumentIdAndVersion(documentId, version)
                .orElseThrow(() -> new RuntimeException("버전을 찾을 수 없습니다"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // 문서 내용을 해당 버전으로 복원
        document.setContent(targetVersion.getContent());
        document.setUpdatedBy(user);
        wikiDocumentRepository.save(document);

        // 새로운 버전 생성 (롤백 기록)
        Integer latestVersionNumber = wikiVersionRepository.findLatestVersionNumber(documentId).orElse(0);
        WikiVersion rollbackVersion = WikiVersion.builder()
                .document(document)
                .version(latestVersionNumber + 1)
                .content(targetVersion.getContent())
                .changeSummary("버전 " + version + "으로 롤백")
                .createdBy(user)
                .build();
        wikiVersionRepository.save(rollbackVersion);

        log.info("Document {} rolled back to version {}", documentId, version);
    }
}
