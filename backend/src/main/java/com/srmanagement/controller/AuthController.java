package com.srmanagement.controller;

import com.srmanagement.dto.request.LoginRequest;
import com.srmanagement.dto.request.RegisterRequest;
import com.srmanagement.dto.request.TokenRefreshRequest;
import com.srmanagement.dto.response.MessageResponse;
import com.srmanagement.dto.response.TokenResponse;
import com.srmanagement.dto.response.UserResponse;
import com.srmanagement.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 컨트롤러
 * 
 * 로그인, 회원가입, 토큰 갱신, 로그아웃 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 로그인
     * @param request 로그인 요청
     * @return 토큰 응답
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 회원가입
     * @param request 회원가입 요청
     * @return 사용자 응답
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * 토큰 갱신
     * @param request 토큰 갱신 요청
     * @return 토큰 응답
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        TokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 로그아웃
     * @param authentication 현재 인증 정보
     * @return 메시지 응답
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(Authentication authentication) {
        if (authentication != null) {
            authService.logout(authentication.getName());
        }
        return ResponseEntity.ok(new MessageResponse("Successfully logged out"));
    }
}
