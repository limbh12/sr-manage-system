package com.srmanagement.dto.request;

import com.srmanagement.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SR 생성 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SrCreateRequest {

    /** SR 제목 */
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    /** SR 설명 */
    private String description;

    /** 우선순위 (기본값: MEDIUM) */
    private Priority priority = Priority.MEDIUM;

    /** 담당자 ID */
    private Long assigneeId;

    /** OPEN API 현황조사 ID */
    private Long openApiSurveyId;

    /** 요청자 이름 */
    private String applicantName;

    /** 요청자 연락처 */
    private String applicantPhone;
}
