package com.srmanagement.dto.request;

import com.srmanagement.entity.Role;
import lombok.Data;

@Data
public class UserCreateRequest {
    private String username;
    private String name;
    private String password;
    private String email;
    private Role role;
}
