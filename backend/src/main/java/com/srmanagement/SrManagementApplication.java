package com.srmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
}
