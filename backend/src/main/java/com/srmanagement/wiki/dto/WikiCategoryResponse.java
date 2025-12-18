package com.srmanagement.wiki.dto;

import com.srmanagement.wiki.entity.WikiCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WikiCategoryResponse {

    private Long id;
    private String name;
    private Long parentId;
    private String parentName;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private Integer documentCount;

    @Builder.Default
    private List<WikiCategoryResponse> children = new ArrayList<>();

    public static WikiCategoryResponse fromEntity(WikiCategory category) {
        return fromEntity(category, false);
    }

    public static WikiCategoryResponse fromEntity(WikiCategory category, boolean includeChildren) {
        WikiCategoryResponseBuilder builder = WikiCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .sortOrder(category.getSortOrder())
                .createdAt(category.getCreatedAt())
                .documentCount(category.getDocumentCount());

        if (category.getParent() != null) {
            builder.parentId(category.getParent().getId())
                   .parentName(category.getParent().getName());
        }

        if (includeChildren && !category.getChildren().isEmpty()) {
            List<WikiCategoryResponse> children = category.getChildren().stream()
                    .map(child -> fromEntity(child, true))
                    .collect(Collectors.toList());
            builder.children(children);
        }

        return builder.build();
    }
}
