package com.srmanagement.wiki.controller;

import com.srmanagement.entity.User;
import com.srmanagement.exception.CustomException;
import com.srmanagement.repository.UserRepository;
import com.srmanagement.wiki.dto.WikiVersionResponse;
import com.srmanagement.wiki.service.WikiVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/wiki/documents/{documentId}/versions")
@RequiredArgsConstructor
public class WikiVersionController {

    private final WikiVersionService wikiVersionService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<WikiVersionResponse>> getDocumentVersions(@PathVariable Long documentId) {
        List<WikiVersionResponse> versions = wikiVersionService.getDocumentVersions(documentId);
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<WikiVersionResponse>> getDocumentVersionsPaged(
            @PathVariable Long documentId,
            @PageableDefault(size = 10, sort = "version", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<WikiVersionResponse> versions = wikiVersionService.getDocumentVersionsPaged(documentId, pageable);
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/{version}")
    public ResponseEntity<WikiVersionResponse> getVersion(
            @PathVariable Long documentId,
            @PathVariable Integer version) {
        WikiVersionResponse response = wikiVersionService.getVersion(documentId, version);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/latest")
    public ResponseEntity<WikiVersionResponse> getLatestVersion(@PathVariable Long documentId) {
        WikiVersionResponse response = wikiVersionService.getLatestVersion(documentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{version}/rollback")
    public ResponseEntity<Void> rollbackToVersion(
            @PathVariable Long documentId,
            @PathVariable Integer version,
            Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
        wikiVersionService.rollbackToVersion(documentId, version, user.getId());
        return ResponseEntity.ok().build();
    }
}
