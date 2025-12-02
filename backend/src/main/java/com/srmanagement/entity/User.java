package com.srmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 엔티티
 * 
 * 시스템 사용자 정보를 저장합니다.
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq_gen")
    @SequenceGenerator(name = "user_seq_gen", sequenceName = "user_seq", allocationSize = 1)
    private Long id;

    /** 사용자명 (로그인 ID) */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** 사용자 이름 (실명) */
    @Column(nullable = false, length = 50)
    private String name;

    /** 암호화된 비밀번호 */
    @Column(nullable = false)
    private String password;

    /** 이메일 주소 */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** 사용자 역할 (ADMIN, USER) */
    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    /** 생성 일시 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
