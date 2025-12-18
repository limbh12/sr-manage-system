package com.srmanagement.dto.request;

import com.srmanagement.entity.Priority;
import com.srmanagement.entity.SrStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * SR 수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SrUpdateRequest {

    /** SR 제목 */
    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    /** SR 설명 */
    private String description;

    /** 처리 내용 */
    private String processingDetails;

    /** SR 상태 */
    private SrStatus status;

    /** 우선순위 */
    private Priority priority;

    /** 분류 */
    private String category;

    /** 요청구분 */
    private String requestType;

    /** 담당자 ID */
    private Long assigneeId;

    /** OPEN API 현황조사 ID */
    private Long openApiSurveyId;

    /** 요청자 이름 */
    private String applicantName;

    /** 요청자 연락처 */
    private String applicantPhone;

    /** 처리예정일자 */
    private LocalDate expectedCompletionDate;
}
