package com.srmanagement.dto.response;

import com.srmanagement.entity.SrHistory;
import com.srmanagement.entity.SrHistoryType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SrHistoryResponse {
    private Long id;
    private String content;
    private SrHistoryType historyType;
    private String previousValue;
    private String newValue;
    private UserSummaryResponse createdBy;
    private LocalDateTime createdAt;

    public static SrHistoryResponse from(SrHistory history) {
        return SrHistoryResponse.builder()
                .id(history.getId())
                .content(history.getContent())
                .historyType(history.getHistoryType())
                .previousValue(history.getPreviousValue())
                .newValue(history.getNewValue())
                .createdBy(UserSummaryResponse.from(history.getCreatedBy()))
                .createdAt(history.getCreatedAt())
                .build();
    }
}
