package com.srmanagement.wiki.controller;

import com.srmanagement.wiki.dto.WikiCategoryRequest;
import com.srmanagement.wiki.dto.WikiCategoryResponse;
import com.srmanagement.wiki.service.WikiCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/wiki/categories")
@RequiredArgsConstructor
public class WikiCategoryController {

    private final WikiCategoryService wikiCategoryService;

    @PostMapping
    public ResponseEntity<WikiCategoryResponse> createCategory(@Valid @RequestBody WikiCategoryRequest request) {
        WikiCategoryResponse response = wikiCategoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WikiCategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody WikiCategoryRequest request) {
        WikiCategoryResponse response = wikiCategoryService.updateCategory(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        wikiCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<WikiCategoryResponse> getCategory(@PathVariable Long id) {
        WikiCategoryResponse response = wikiCategoryService.getCategory(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<WikiCategoryResponse>> getAllCategories() {
        List<WikiCategoryResponse> categories = wikiCategoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/root")
    public ResponseEntity<List<WikiCategoryResponse>> getRootCategories() {
        List<WikiCategoryResponse> categories = wikiCategoryService.getRootCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/parent/{parentId}")
    public ResponseEntity<List<WikiCategoryResponse>> getChildCategories(@PathVariable Long parentId) {
        List<WikiCategoryResponse> categories = wikiCategoryService.getChildCategories(parentId);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/search")
    public ResponseEntity<List<WikiCategoryResponse>> searchCategories(@RequestParam String keyword) {
        List<WikiCategoryResponse> categories = wikiCategoryService.searchCategories(keyword);
        return ResponseEntity.ok(categories);
    }
}
