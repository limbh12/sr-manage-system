package com.srmanagement.wiki.controller;

import com.srmanagement.entity.User;
import com.srmanagement.exception.CustomException;
import com.srmanagement.repository.UserRepository;
import com.srmanagement.wiki.dto.WikiDocumentRequest;
import com.srmanagement.wiki.dto.WikiDocumentResponse;
import com.srmanagement.wiki.service.WikiDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/wiki/documents")
@RequiredArgsConstructor
public class WikiDocumentController {

    private final WikiDocumentService wikiDocumentService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WIKI_EDITOR')")
    public ResponseEntity<WikiDocumentResponse> createDocument(
            @Valid @RequestBody WikiDocumentRequest request,
            Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
        WikiDocumentResponse response = wikiDocumentService.createDocument(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WIKI_EDITOR')")
    public ResponseEntity<WikiDocumentResponse> updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody WikiDocumentRequest request,
            Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
        WikiDocumentResponse response = wikiDocumentService.updateDocument(id, request, user.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long id,
            Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
        wikiDocumentService.deleteDocument(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<WikiDocumentResponse> getDocument(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean incrementView) {
        WikiDocumentResponse response;
        if (incrementView) {
            response = wikiDocumentService.getDocumentAndIncrementViewCount(id);
        } else {
            response = wikiDocumentService.getDocument(id);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<WikiDocumentResponse>> getAllDocuments(
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<WikiDocumentResponse> documents = wikiDocumentService.getAllDocuments(pageable);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<WikiDocumentResponse>> getDocumentsByCategory(
            @PathVariable Long categoryId,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<WikiDocumentResponse> documents = wikiDocumentService.getDocumentsByCategory(categoryId, pageable);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/sr/{srId}")
    public ResponseEntity<List<WikiDocumentResponse>> getDocumentsBySr(@PathVariable Long srId) {
        List<WikiDocumentResponse> documents = wikiDocumentService.getDocumentsBySr(srId);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<WikiDocumentResponse>> searchDocuments(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<WikiDocumentResponse> documents = wikiDocumentService.searchDocuments(keyword, pageable);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/recent")
    public ResponseEntity<Page<WikiDocumentResponse>> getRecentlyUpdated(
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<WikiDocumentResponse> documents = wikiDocumentService.getRecentlyUpdated(pageable);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/popular")
    public ResponseEntity<Page<WikiDocumentResponse>> getPopularDocuments(
            @PageableDefault(size = 10, sort = "viewCount", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<WikiDocumentResponse> documents = wikiDocumentService.getPopularDocuments(pageable);
        return ResponseEntity.ok(documents);
    }
}
