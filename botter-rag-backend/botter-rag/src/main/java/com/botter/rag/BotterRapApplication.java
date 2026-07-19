package com.botter.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Hello world!
 *
 */
@SpringBootApplication
@EnableAsync        // 开启异步，支持索引任务异步执行
@EnableRetry        // 开启重试，支持 Embedding 调用失败重试
public class BotterRapApplication
{
    public static void main( String[] args )
    {
        SpringApplication.run(BotterRapApplication.class, args);
    }
}
