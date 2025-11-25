package com.srmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS 설정 클래스
 */
@Configuration
public class CorsConfig {

    /**
     * CORS 필터 Bean
     * 
     * React 프론트엔드와의 통신을 위한 CORS 설정
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // 자격 증명 허용 (쿠키, 인증 헤더 등)
        config.setAllowCredentials(true);
        
        // 허용할 Origin (프론트엔드 주소)
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",  // Vite 개발 서버
                "http://localhost:3000"   // 기타 개발 서버
        ));
        
        // 허용할 헤더
        config.setAllowedHeaders(List.of("*"));
        
        // 허용할 HTTP 메서드
        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // 노출할 헤더 (프론트엔드에서 접근 가능)
        config.setExposedHeaders(Arrays.asList(
                "Authorization", "Content-Type"
        ));
        
        // 프리플라이트 요청 캐시 시간 (1시간)
        config.setMaxAge(3600L);
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
