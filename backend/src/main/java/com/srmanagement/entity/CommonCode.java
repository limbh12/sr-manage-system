package com.srmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "common_code", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"code_group", "code_value"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 코드 그룹 (예: SR_CATEGORY, SR_REQUEST_TYPE) */
    @Column(name = "code_group", nullable = false, length = 50)
    private String codeGroup;

    /** 코드 값 (예: OPEN_API, DATA_REQ) */
    @Column(name = "code_value", nullable = false, length = 50)
    private String codeValue;

    /** 코드 명 (예: OPEN API, 자료요청) */
    @Column(name = "code_name", nullable = false, length = 100)
    private String codeName;

    /** 정렬 순서 */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    /** 사용 여부 */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** 설명 */
    @Column(length = 255)
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
