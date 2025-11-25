package com.srmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SR(Service Request) 엔티티
 * 
 * 서비스 요청 정보를 저장합니다.
 */
@Entity
@Table(name = "sr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SR 제목 */
    @Column(nullable = false, length = 200)
    private String title;

    /** SR 상세 설명 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** SR 상태 (OPEN, IN_PROGRESS, RESOLVED, CLOSED) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SrStatus status = SrStatus.OPEN;

    /** 우선순위 (LOW, MEDIUM, HIGH, CRITICAL) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    /** 요청자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    /** 담당자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    /** 생성 일시 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 수정 일시 */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
