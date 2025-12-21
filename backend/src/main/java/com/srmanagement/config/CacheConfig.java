package com.srmanagement.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 캐시 설정
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 캐시 매니저 설정
     * - AI 검색 결과: 5분간 캐싱
     * - 임베딩 상태: 30초간 캐싱
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 기본 캐시 설정
        Caffeine<Object, Object> caffeineBuilder = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats();
        cacheManager.setCaffeine(caffeineBuilder);

        // 캐시 이름 등록
        cacheManager.setCacheNames(java.util.List.of(
                "aiSearchResults",      // AI 검색 결과 (5분)
                "embeddingStatus",      // 임베딩 상태 (30초)
                "documentSummary"       // 문서 요약 (10분)
        ));

        return cacheManager;
    }

    /**
     * AI 검색 결과 캐시용 Caffeine 설정
     */
    @Bean
    public Caffeine<Object, Object> aiSearchCaffeine() {
        return Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats();
    }

    /**
     * 임베딩 상태 캐시용 Caffeine 설정 (짧은 TTL)
     */
    @Bean
    public Caffeine<Object, Object> embeddingStatusCaffeine() {
        return Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .recordStats();
    }
}
