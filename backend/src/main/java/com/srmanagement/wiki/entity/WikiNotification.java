package com.srmanagement.wiki.entity;

import com.srmanagement.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 위키 알림 엔티티
 */
@Entity
@Table(name = "wiki_notifications", indexes = {
    @Index(name = "idx_notification_user", columnList = "user_id"),
    @Index(name = "idx_notification_read", columnList = "is_read"),
    @Index(name = "idx_notification_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "wiki_notification_seq_gen")
    @SequenceGenerator(name = "wiki_notification_seq_gen", sequenceName = "wiki_notification_seq", allocationSize = 1)
    private Long id;

    /**
     * 알림 받을 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 관련 문서
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private WikiDocument document;

    /**
     * 알림 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    private NotificationType type;

    /**
     * 알림 제목
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 알림 내용
     */
    @Column(columnDefinition = "TEXT")
    private String message;

    /**
     * 알림을 발생시킨 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by_id")
    private User triggeredBy;

    /**
     * 읽음 여부
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /**
     * 읽은 시간
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * 생성 시간
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 알림 읽음 처리
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    /**
     * 리소스 유형 (Wiki, Survey, SR 구분용)
     */
    @Column(name = "resource_type", length = 20)
    private String resourceType;

    /**
     * 리소스 ID (Survey ID 또는 SR ID)
     */
    @Column(name = "resource_id")
    private Long resourceId;

    /**
     * 알림 유형 열거형
     */
    public enum NotificationType {
        DOCUMENT_CREATED,      // 새 문서 생성
        DOCUMENT_UPDATED,      // 문서 수정
        DOCUMENT_DELETED,      // 문서 삭제
        CATEGORY_CREATED,      // 카테고리 생성
        CATEGORY_UPDATED,      // 카테고리 수정
        MENTIONED,             // 멘션됨
        // OPEN API 현황조사 알림
        SURVEY_CREATED,        // 현황조사 생성
        SURVEY_UPDATED,        // 현황조사 수정
        // SR 알림
        SR_CREATED,            // SR 생성
        SR_UPDATED,            // SR 수정
        SR_ASSIGNED,           // SR 담당자 지정
        SR_STATUS_CHANGED      // SR 상태 변경
    }
}
