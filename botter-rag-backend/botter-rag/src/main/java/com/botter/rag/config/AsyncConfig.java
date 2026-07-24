package com.botter.rag.config;

import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Async executor used by document indexing tasks. */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "indexTaskExecutor")
    public ThreadPoolTaskExecutor indexTaskExecutor(ThreadPoolTaskExecutorBuilder builder) {
        return builder.build();
    }
}
