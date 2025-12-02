package com.srmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * 리프레시 토큰 엔티티
 * 
 * JWT Refresh Token을 저장합니다.
 */
@Entity
@Table(name = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UUID id;

    /** Refresh Token 값 */
    @Column(nullable = false, unique = true, length = 500)
    private String token;

    /** 토큰 소유 사용자 */
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 토큰 만료 일시 */
    @Column(nullable = false)
    private Instant expiryDate;

    /**
     * 토큰 만료 여부 확인
     * @return 만료되었으면 true
     */
    public boolean isExpired() {
        return expiryDate.isBefore(Instant.now());
    }
}
