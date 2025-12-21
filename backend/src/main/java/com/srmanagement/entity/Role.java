package com.srmanagement.entity;

/**
 * 사용자 역할 열거형
 */
public enum Role {
    /** 관리자 - 모든 권한 */
    ADMIN,
    /** 위키 편집자 - 위키 문서 편집/삭제 권한 */
    WIKI_EDITOR,
    /** 일반 사용자 - 읽기 전용 */
    USER
}
