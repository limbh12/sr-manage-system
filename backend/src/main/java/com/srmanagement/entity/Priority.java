package com.srmanagement.entity;

/**
 * SR 우선순위 열거형
 */
public enum Priority {
    /** 낮음 - 처리 권장: 5일 이내 */
    LOW,
    /** 보통 - 처리 권장: 3일 이내 */
    MEDIUM,
    /** 높음 - 처리 권장: 1일 이내 */
    HIGH,
    /** 긴급 - 처리 권장: 4시간 이내 */
    CRITICAL
}
