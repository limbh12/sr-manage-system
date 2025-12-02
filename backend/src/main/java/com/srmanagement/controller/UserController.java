package com.srmanagement.controller;

import com.srmanagement.dto.response.UserResponse;
import com.srmanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 컨트롤러
 * 
 * 사용자 정보 조회 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 현재 사용자 정보 조회
     * @param authentication 현재 인증 정보
     * @return UserResponse
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        UserResponse response = userService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 목록 조회 (관리자 전용)
     * @param pageable 페이지네이션
     * @return Page<UserResponse>
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserResponse> response = userService.getAllUsers(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 목록 조회 (옵션용)
     * @return List<UserResponse>
     */
    @GetMapping("/options")
    public ResponseEntity<java.util.List<UserResponse>> getUserOptions() {
        java.util.List<UserResponse> response = userService.getAllUsersList();
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 사용자 정보 조회
     * @param id 사용자 ID
     * @return UserResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 정보 수정 (관리자 전용)
     * @param id 사용자 ID
     * @param request 수정 요청 DTO
     * @return UserResponse
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @RequestBody com.srmanagement.dto.request.UserUpdateRequest request) {
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(response);
    }
}
