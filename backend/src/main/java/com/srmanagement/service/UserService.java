package com.srmanagement.service;

import com.srmanagement.dto.request.UserCreateRequest;
import com.srmanagement.dto.response.UserResponse;
import com.srmanagement.entity.Role;
import com.srmanagement.entity.User;
import com.srmanagement.exception.CustomException;
import com.srmanagement.repository.RefreshTokenRepository;
import com.srmanagement.repository.SrRepository;
import com.srmanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Autowired
    private SrRepository srRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
     * 전체 사용자 목록 조회 (드롭다운용)
     * @return List<UserResponse>
     */
    @Transactional(readOnly = true)
    public java.util.List<UserResponse> getAllUsersList() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .collect(java.util.stream.Collectors.toList());
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

    /**
     * 사용자 생성 (관리자 전용)
     * @param request 생성 요청 DTO
     * @return UserResponse
     */
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        // 사용자명 중복 확인
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new CustomException("Username already exists", HttpStatus.BAD_REQUEST);
        }

        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException("Email already exists", HttpStatus.BAD_REQUEST);
        }

        // 사용자 생성
        User user = User.builder()
                .username(request.getUsername())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(request.getRole() != null ? request.getRole() : Role.USER)
                .build();

        User savedUser = userRepository.save(user);
        return UserResponse.from(savedUser);
    }

    /**
     * 사용자 정보 수정
     * @param id 사용자 ID
     * @param request 수정 요청 DTO
     * @return UserResponse
     */
    @Transactional
    public UserResponse updateUser(Long id, com.srmanagement.dto.request.UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException("User not found with id: " + id, HttpStatus.NOT_FOUND));

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new CustomException("Email already exists", HttpStatus.BAD_REQUEST);
            }
            user.setEmail(request.getEmail());
        }

        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        User updatedUser = userRepository.save(user);
        return UserResponse.from(updatedUser);
    }

    /**
     * 사용자 삭제 (관리자 전용)
     * @param id 사용자 ID
     */
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new CustomException("User not found with id: " + id, HttpStatus.NOT_FOUND);
        }

        // 연관 데이터 확인
        if (srRepository.existsByRequesterId(id)) {
            throw new CustomException("Cannot delete user. User has registered SRs.", HttpStatus.BAD_REQUEST);
        }

        if (srRepository.existsByAssigneeId(id)) {
            throw new CustomException("Cannot delete user. User is assigned to SRs.", HttpStatus.BAD_REQUEST);
        }

        // Refresh Token 삭제
        User user = userRepository.findById(id).orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
        refreshTokenRepository.deleteByUser(user);

        userRepository.deleteById(id);
    }
}
