package com.srmanagement.entity;

/**
 * SR 상태 열거형
 */
public enum SrStatus {
    /** 신규 등록 */
    OPEN,
    /** 처리 중 */
    IN_PROGRESS,
    /** 해결됨 */
    RESOLVED,
    /** 종료 */
    CLOSED
}
