package com.srmanagement.dto.request;

import com.srmanagement.entity.SrStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SR 상태 변경 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SrStatusUpdateRequest {

    /** 변경할 상태 */
    @NotNull(message = "Status is required")
    private SrStatus status;
}
