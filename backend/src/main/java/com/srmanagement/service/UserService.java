package com.srmanagement.service;

import com.srmanagement.dto.response.UserResponse;
import com.srmanagement.entity.User;
import com.srmanagement.exception.CustomException;
import com.srmanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 서비스
 * 
 * 사용자 관련 비즈니스 로직을 처리합니다.
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * 사용자 목록 조회 (관리자 전용)
     * @param pageable 페이지네이션
     * @return Page<UserResponse>
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    /**
     * 현재 사용자 정보 조회
     * @param username 사용자명
     * @return UserResponse
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
        return UserResponse.from(user);
    }

    /**
     * 사용자 ID로 조회
     * @param id 사용자 ID
     * @return UserResponse
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException("User not found with id: " + id, HttpStatus.NOT_FOUND));
        return UserResponse.from(user);
    }
}
