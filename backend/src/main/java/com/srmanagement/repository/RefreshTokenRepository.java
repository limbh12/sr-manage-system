package com.srmanagement.repository;

import com.srmanagement.entity.RefreshToken;
import com.srmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 리프레시 토큰 레포지토리
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * 토큰 값으로 RefreshToken 조회
     * @param token 토큰 값
     * @return Optional<RefreshToken>
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 사용자로 RefreshToken 조회
     * @param user 사용자
     * @return Optional<RefreshToken>
     */
    Optional<RefreshToken> findByUser(User user);

    /**
     * 사용자의 RefreshToken 삭제
     * @param user 사용자
     */
    @Modifying
    void deleteByUser(User user);
}
