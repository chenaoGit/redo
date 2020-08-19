package com.xxx.redo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {

    @Value("${threadpool.corePoolSize}")
    private Integer corePoolSize;

    @Value("${threadpool.maxPoolSize}")
    private Integer maxPoolSize;

    @Value("${threadpool.keepAliveSeconds}")
    private Integer keepAliveSeconds;

    @Value("${threadpool.queueCapacity}")
    private Integer queueCapacity;

    @Bean("commonThreadPoolTaskExecutor")
    public ThreadPoolTaskExecutor getThreadPoolTaskExecutor(){
        ThreadPoolTaskExecutor threadPoolExecutor = new ThreadPoolTaskExecutor();
        threadPoolExecutor.setCorePoolSize(corePoolSize);
        threadPoolExecutor.setKeepAliveSeconds(keepAliveSeconds);
        threadPoolExecutor.setMaxPoolSize(maxPoolSize);
        threadPoolExecutor.setQueueCapacity(queueCapacity);
        // 设置拒绝策略：由调用线程处理改任务，如果执行器已关闭,则丢弃.
        threadPoolExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return threadPoolExecutor;
    }
}
