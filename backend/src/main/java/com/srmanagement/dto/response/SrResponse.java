package com.srmanagement.dto.response;

import com.srmanagement.entity.Priority;
import com.srmanagement.entity.Sr;
import com.srmanagement.entity.SrStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SR 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrResponse {

    /** SR ID */
    private Long id;

    /** SR 제목 */
    private String title;

    /** SR 설명 */
    private String description;

    /** SR 상태 */
    private SrStatus status;

    /** 우선순위 */
    private Priority priority;

    /** 요청자 정보 */
    private UserResponse requester;

    /** 담당자 정보 */
    private UserResponse assignee;

    /** 생성 일시 */
    private LocalDateTime createdAt;

    /** 수정 일시 */
    private LocalDateTime updatedAt;

    /**
     * Sr 엔티티를 SrResponse로 변환
     * @param sr Sr 엔티티
     * @return SrResponse
     */
    public static SrResponse from(Sr sr) {
        return SrResponse.builder()
                .id(sr.getId())
                .title(sr.getTitle())
                .description(sr.getDescription())
                .status(sr.getStatus())
                .priority(sr.getPriority())
                .requester(UserResponse.from(sr.getRequester()))
                .assignee(sr.getAssignee() != null ? UserResponse.from(sr.getAssignee()) : null)
                .createdAt(sr.getCreatedAt())
                .updatedAt(sr.getUpdatedAt())
                .build();
    }
}
