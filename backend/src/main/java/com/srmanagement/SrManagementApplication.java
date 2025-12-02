package com.srmanagement;

import com.srmanagement.entity.Role;
import com.srmanagement.entity.User;
import com.srmanagement.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * SR Management System 메인 애플리케이션 클래스
 * 
 * Service Request 관리를 위한 Spring Boot 애플리케이션입니다.
 */
@SpringBootApplication
public class SrManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(SrManagementApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // admin 사용자 생성 또는 비밀번호 업데이트
            User admin = userRepository.findByUsername("admin").orElse(null);
            
            if (admin == null) {
                admin = User.builder()
                        .username("admin")
                        .name("Administrator")
                        .password(passwordEncoder.encode("admin123"))
                        .email("admin@example.com")
                        .role(Role.ADMIN)
                        .build();
                userRepository.save(admin);
                System.out.println("Admin user created: admin / admin123");
            } else {
                // 비밀번호를 admin123으로 강제 업데이트
                admin.setPassword(passwordEncoder.encode("admin123"));
                userRepository.save(admin);
                System.out.println("Admin user password updated to: admin123");
            }
        };
    }
}
