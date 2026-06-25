package com.drivingschool.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置类。
 * <p>
 * 启用 Spring 异步执行功能，并配置线程池参数。
 * 用于支持 @Async 注解标记的异步方法，使耗时操作（如日志记录）
 * 可以在独立线程中执行，不阻塞主请求线程。
 * </p>
 * <p>
 * 线程池参数说明：
 * - 核心线程数：2（常驻线程，即使空闲也会保留）
 * - 最大线程数：10（线程池允许创建的最大线程数）
 * - 队列容量：100（当核心线程都在忙时，新任务会进入队列等待）
 * - 线程名称前缀：async-log-（方便日志排查）
 * </p>
 */
@Configuration
@EnableAsync  // 启用异步方法执行功能
public class AsyncConfig {

    /**
     * 配置异步任务线程池。
     * <p>
     * Bean 名称为 "asyncExecutor"，在 @Async 注解中可指定使用此线程池。
     * 如果不指定，Spring 会使用默认的 SimpleAsyncTaskExecutor（每次创建新线程，不推荐）。
     * </p>
     *
     * @return Executor 线程池执行器实例
     */
    @Bean("asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：线程池中始终保持的线程数量
        executor.setCorePoolSize(2);
        // 最大线程数：线程池允许创建的最大线程数量
        executor.setMaxPoolSize(10);
        // 队列容量：当核心线程都在执行任务时，新任务在此队列中等待
        executor.setQueueCapacity(100);
        // 线程名称前缀：便于在日志中识别异步任务线程
        executor.setThreadNamePrefix("async-log-");
        executor.initialize();
        return executor;
    }
}
