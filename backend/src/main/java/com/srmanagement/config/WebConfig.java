package com.srmanagement.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

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

    /**
     * SPA 라우팅 지원
     * /api로 시작하지 않는 모든 경로를 index.html로 포워딩
     */
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(@NonNull String resourcePath, @NonNull Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        // 요청된 리소스가 존재하면 그대로 반환
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }

                        // API 경로는 null 반환 (Spring Security에서 처리)
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("h2-console/")) {
                            return null;
                        }

                        // 그 외 경로는 index.html로 포워딩 (SPA 라우팅)
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
