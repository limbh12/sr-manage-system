package com.srmanagement.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 작업 설정
 * - 임베딩 생성 등 시간이 오래 걸리는 작업을 비동기로 처리
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    @Bean(name = "embeddingTaskExecutor")
    public Executor embeddingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);      // 기본 스레드 수
        executor.setMaxPoolSize(4);       // 최대 스레드 수
        executor.setQueueCapacity(10);    // 대기 큐 크기
        executor.setThreadNamePrefix("Embedding-");
        executor.setRejectedExecutionHandler((r, e) ->
            log.warn("임베딩 작업 거부됨 - 큐가 가득 참"));
        executor.initialize();
        return executor;
    }
}
