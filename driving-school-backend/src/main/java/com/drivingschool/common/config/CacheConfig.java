package com.drivingschool.common.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置类
 * <p>
 * 启用 Spring Cache 抽象层，用于缓存统计数据等读多写少的数据。
 * 当前使用内存缓存（ConcurrentMapCacheManager），生产环境建议切换为 Redis 缓存。
 * </p>
 * <p>
 * 使用方式：
 * - 在 Service 方法上添加 @Cacheable("cacheName") 缓存返回结果
 * - 在写操作方法上添加 @CacheEvict(cacheNames="cacheName", allEntries=true) 清除缓存
 * </p>
 *
 * @author drivingschool
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 缓存管理器
     * <p>
     * 使用 ConcurrentMapCacheManager 作为默认实现。
     * 生产环境可替换为 RedisCacheManager 以支持分布式缓存。
     * </p>
     *
     * @return CacheManager 实例
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "overview",         // 总览统计
                "dashboard",        // 仪表盘
                "revenueTrend",     // 收入趋势
                "enrollmentTrend",  // 报名趋势
                "passRate",         // 通过率
                "coachRanking",     // 教练排名
                "vehicleUsage",     // 车辆使用率
                "studentStats",     // 学员统计
                "registrationStats" // 报名统计
        );
    }
}
