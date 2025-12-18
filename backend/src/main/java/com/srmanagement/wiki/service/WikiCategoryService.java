package com.srmanagement.wiki.service;

import com.srmanagement.wiki.dto.WikiCategoryRequest;
import com.srmanagement.wiki.dto.WikiCategoryResponse;
import com.srmanagement.wiki.entity.WikiCategory;
import com.srmanagement.wiki.repository.WikiCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WikiCategoryService {

    private final WikiCategoryRepository wikiCategoryRepository;

    @Transactional
    public WikiCategoryResponse createCategory(WikiCategoryRequest request) {
        log.info("Creating wiki category: {}", request.getName());

        WikiCategory category = WikiCategory.builder()
                .name(request.getName())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        // 부모 카테고리 설정
        if (request.getParentId() != null) {
            WikiCategory parent = wikiCategoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("부모 카테고리를 찾을 수 없습니다"));
            category.setParent(parent);
        }

        WikiCategory savedCategory = wikiCategoryRepository.save(category);
        log.info("Wiki category created with ID: {}", savedCategory.getId());

        return WikiCategoryResponse.fromEntity(savedCategory);
    }

    @Transactional
    public WikiCategoryResponse updateCategory(Long id, WikiCategoryRequest request) {
        log.info("Updating wiki category: {}", id);

        WikiCategory category = wikiCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다"));

        category.setName(request.getName());
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

        // 부모 카테고리 변경
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new RuntimeException("자기 자신을 부모로 설정할 수 없습니다");
            }
            WikiCategory parent = wikiCategoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("부모 카테고리를 찾을 수 없습니다"));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        WikiCategory savedCategory = wikiCategoryRepository.save(category);
        log.info("Wiki category updated: {}", id);

        return WikiCategoryResponse.fromEntity(savedCategory);
    }

    @Transactional
    public void deleteCategory(Long id) {
        log.info("Deleting wiki category: {}", id);

        WikiCategory category = wikiCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다"));

        // 자식 카테고리가 있으면 삭제 불가
        if (!category.getChildren().isEmpty()) {
            throw new RuntimeException("하위 카테고리가 있는 카테고리는 삭제할 수 없습니다");
        }

        // 문서가 있으면 삭제 불가
        if (!category.getDocuments().isEmpty()) {
            throw new RuntimeException("문서가 있는 카테고리는 삭제할 수 없습니다");
        }

        wikiCategoryRepository.deleteById(id);
        log.info("Wiki category deleted: {}", id);
    }

    @Transactional(readOnly = true)
    public WikiCategoryResponse getCategory(Long id) {
        WikiCategory category = wikiCategoryRepository.findByIdWithChildren(id)
                .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다"));

        return WikiCategoryResponse.fromEntity(category, true);
    }

    @Transactional(readOnly = true)
    public List<WikiCategoryResponse> getAllCategories() {
        return wikiCategoryRepository.findAll().stream()
                .map(WikiCategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WikiCategoryResponse> getRootCategories() {
        return wikiCategoryRepository.findByParentIsNullOrderBySortOrderAsc().stream()
                .map(category -> WikiCategoryResponse.fromEntity(category, true))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WikiCategoryResponse> getChildCategories(Long parentId) {
        return wikiCategoryRepository.findByParentIdOrderBySortOrderAsc(parentId).stream()
                .map(WikiCategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WikiCategoryResponse> searchCategories(String keyword) {
        return wikiCategoryRepository.findByNameContainingIgnoreCase(keyword).stream()
                .map(WikiCategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
