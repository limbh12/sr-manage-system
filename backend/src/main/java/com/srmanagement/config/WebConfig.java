package com.srmanagement.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정 클래스
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Content Negotiation 설정
     * .mjs 파일을 JavaScript MIME 타입으로 서빙
     */
    @Override
    public void configureContentNegotiation(@NonNull ContentNegotiationConfigurer configurer) {
        configurer
                .favorParameter(false)
                .ignoreAcceptHeader(false)
                .defaultContentType(MediaType.APPLICATION_JSON)
                .mediaType("mjs", MediaType.valueOf("application/javascript"))
                .mediaType("js", MediaType.valueOf("application/javascript"));
    }
}
