package com.srmanagement.dto.response;

import com.srmanagement.entity.Priority;
import com.srmanagement.entity.Sr;
import com.srmanagement.entity.SrStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SR 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrResponse {

    /** SR ID (DB PK) */
    private Long id;

    /** SR ID (SR-YYMM-XXXX) */
    private String srId;

    /** SR 제목 */
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

    /** 요청자 정보 */
    private UserResponse requester;

    /** 담당자 정보 */
    private UserResponse assignee;

    /** 생성 일시 */
    private LocalDateTime createdAt;

    /** 수정 일시 */
    private LocalDateTime updatedAt;

    /** OPEN API 현황조사 ID */
    private Long openApiSurveyId;

    /** 요청자 이름 */
    private String applicantName;

    /** 요청자 연락처 */
    private String applicantPhone;

    /** 삭제 여부 */
    private Boolean deleted;

    /** 삭제 일시 */
    private LocalDateTime deletedAt;

    /** 처리예정일자 */
    private LocalDate expectedCompletionDate;

    /**
     * Sr 엔티티를 SrResponse로 변환
     * @param sr Sr 엔티티
     * @return SrResponse
     */
    public static SrResponse from(Sr sr) {
        return SrResponse.builder()
                .id(sr.getId())
                .srId(sr.getSrId())
                .title(sr.getTitle())
                .description(sr.getDescription())
                .processingDetails(sr.getProcessingDetails())
                .status(sr.getStatus())
                .priority(sr.getPriority())
                .category(sr.getCategory())
                .requestType(sr.getRequestType())
                .requester(UserResponse.from(sr.getRequester()))
                .assignee(sr.getAssignee() != null ? UserResponse.from(sr.getAssignee()) : null)
                .createdAt(sr.getCreatedAt())
                .updatedAt(sr.getUpdatedAt())
                .openApiSurveyId(sr.getOpenApiSurveyId())
                .applicantName(sr.getApplicantName())
                .applicantPhone(sr.getApplicantPhone())
                .deleted(sr.getDeleted())
                .deletedAt(sr.getDeletedAt())
                .expectedCompletionDate(sr.getExpectedCompletionDate())
                .build();
    }
}
