package com.srmanagement.dto.request;

import com.srmanagement.entity.Priority;
import com.srmanagement.entity.SrStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    /** SR 상태 */
    private SrStatus status;

    /** 우선순위 */
    private Priority priority;

    /** 담당자 ID */
    private Long assigneeId;
}
