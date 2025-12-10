package com.srmanagement.config;

import com.srmanagement.entity.Role;
import com.srmanagement.entity.User;
import com.srmanagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        // 초기 관리자 계정 생성
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .name("Administrator")
                    .email("admin@example.com")
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            logger.info("Admin user created: admin / admin123");
        }

        // 초기 일반 사용자 계정 생성
        if (!userRepository.existsByUsername("user")) {
            User user = User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("user123"))
                    .name("일반사용자")
                    .email("user@example.com")
                    .role(Role.USER)
                    .build();
            userRepository.save(user);
            logger.info("Regular user created: user / user123");
        }

        // 초기 데이터 적재 (기관 코드 및 공통 코드)
        // 운영 환경(prod)에서는 spring.sql.init.mode=never로 설정되어 있어 data.sql이 자동 실행되지 않음
        // 따라서 데이터가 없는 경우 수동으로 스크립트를 실행함
        try {
            Long orgCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM organizations", Long.class);
            logger.info("Current organization count: {}", orgCount);
            
            if (orgCount != null && orgCount == 0) {
                logger.info("Organizations table is empty. Executing data.sql...");
                Resource resource = new ClassPathResource("data.sql");
                try (Connection connection = dataSource.getConnection()) {
                    ScriptUtils.executeSqlScript(connection, resource);
                }
                logger.info("data.sql executed successfully.");
            } else {
                logger.info("Organizations table is not empty. Skipping data.sql execution.");
            }
        } catch (Exception e) {
            logger.error("Failed to execute data.sql", e);
            // 테이블이 없거나 다른 오류 발생 시 무시 (이미 초기화되었거나 오류 상황)
        }
    }
}
