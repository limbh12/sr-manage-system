package com.srmanagement.wiki.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WikiDocumentRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    private String title;

    private String content;

    private Long categoryId;

    private List<Long> srIds;

    @Size(max = 200, message = "변경 요약은 200자를 초과할 수 없습니다")
    private String changeSummary;

    /**
     * 목차 자동 생성 여부 (기본값: false)
     */
    private Boolean generateToc;
}
