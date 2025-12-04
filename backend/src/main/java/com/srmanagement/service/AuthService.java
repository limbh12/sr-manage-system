package com.srmanagement.service;

import com.srmanagement.dto.request.LoginRequest;
import com.srmanagement.dto.request.TokenRefreshRequest;
import com.srmanagement.dto.response.TokenResponse;
import com.srmanagement.dto.response.UserResponse;
import com.srmanagement.entity.RefreshToken;
import com.srmanagement.entity.Role;
import com.srmanagement.entity.User;
import com.srmanagement.exception.CustomException;
import com.srmanagement.repository.RefreshTokenRepository;
import com.srmanagement.repository.UserRepository;
import com.srmanagement.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 인증 서비스
 * 
 * 로그인, 회원가입, 토큰 갱신 등 인증 관련 비즈니스 로직을 처리합니다.
 */
@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    /**
     * 로그인
     * @param request 로그인 요청 DTO
     * @return 토큰 응답 DTO
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String accessToken = tokenProvider.generateAccessToken(authentication);
        
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
        
        // 기존 Refresh Token 삭제
        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.flush();
        
        // 새 Refresh Token 생성
        RefreshToken refreshToken = createRefreshToken(user);
        
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenValidity())
                .build();
    }

    /**
     * 토큰 갱신
     * @param request 토큰 갱신 요청 DTO
     * @return 토큰 응답 DTO
     */
    @Transactional
    public TokenResponse refreshToken(TokenRefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new CustomException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new CustomException("Refresh token has expired", HttpStatus.UNAUTHORIZED);
        }

        User user = refreshToken.getUser();
        String newAccessToken = tokenProvider.generateAccessToken(user.getUsername());
        
        // 새 Refresh Token 생성
        refreshTokenRepository.delete(refreshToken);
        refreshTokenRepository.flush();
        RefreshToken newRefreshToken = createRefreshToken(user);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenValidity())
                .build();
    }

    /**
     * 로그아웃
     * @param username 사용자명
     */
    @Transactional
    public void logout(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
        
        refreshTokenRepository.deleteByUser(user);
        SecurityContextHolder.clearContext();
    }

    /**
     * Refresh Token 생성
     * @param user 사용자
     * @return RefreshToken 엔티티
     */
    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenProvider.generateRefreshToken())
                .expiryDate(Instant.now().plusMillis(tokenProvider.getRefreshTokenValidity()))
                .build();
        
        return refreshTokenRepository.save(refreshToken);
    }
}
