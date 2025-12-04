package com.srmanagement.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class BulkUploadResult {
    private int totalCount;
    private int successCount;
    private int failureCount;
    private List<FailureDetail> failures;

    @Data
    @Builder
    public static class FailureDetail {
        private int rowNumber;
        private String reason;
        private String data;
    }
}
