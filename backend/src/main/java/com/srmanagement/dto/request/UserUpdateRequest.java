package com.srmanagement.dto.request;

import com.srmanagement.entity.Role;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 정보 수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    /** 사용자 이름 */
    private String name;

    /** 이메일 */
    @Email(message = "Invalid email format")
    private String email;

    /** 역할 */
    private Role role;
}
