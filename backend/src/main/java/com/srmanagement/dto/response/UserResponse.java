package com.srmanagement.dto.response;

import com.srmanagement.entity.Role;
import com.srmanagement.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    /** 사용자 ID */
    private Long id;

    /** 사용자명 */
    private String username;

    /** 사용자 이름 */
    private String name;

    /** 이메일 */
    private String email;

    /** 역할 */
    private Role role;

    /** 생성 일시 */
    private LocalDateTime createdAt;

    /**
     * User 엔티티를 UserResponse로 변환
     * @param user User 엔티티
     * @return UserResponse
     */
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
