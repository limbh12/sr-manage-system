package com.srmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 로그인 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /** 사용자명 */
    @NotBlank(message = "Username is required")
    private String username;

    /** 비밀번호 */
    @NotBlank(message = "Password is required")
    private String password;
}
