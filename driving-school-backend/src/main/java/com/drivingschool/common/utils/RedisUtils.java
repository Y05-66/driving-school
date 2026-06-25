package com.drivingschool.common.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 操作工具类。
 * <p>
 * 封装了常用的 Redis 操作，基于 StringRedisTemplate 实现。
 * 本系统中 Redis 主要用于：
 * - JWT 令牌黑名单管理（用户登出后令牌失效）
 * - 缓存热点数据
 * - 计数器（如登录失败次数限制）
 * </p>
 * <p>
 * 所有值均以字符串形式存储，如需存储复杂对象，
 * 建议先序列化为 JSON 字符串再存入。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class RedisUtils {

    /** Redis 字符串模板，封装了 Redis 的基本操作 */
    private final StringRedisTemplate redisTemplate;

    /**
     * 设置键值对并指定过期时间。
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时间数值
     * @param unit    过期时间单位（如 TimeUnit.SECONDS、TimeUnit.MINUTES）
     */
    public void set(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 根据键获取缓存值。
     *
     * @param key 缓存键
     * @return 缓存值，如果键不存在则返回 null
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除指定键的缓存。
     *
     * @param key 要删除的缓存键
     * @return true 表示删除成功，false 表示键不存在
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 检查指定键是否存在。
     *
     * @param key 要检查的缓存键
     * @return true 表示键存在，false 表示不存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 将指定键的值自增 1。
     * <p>
     * 如果键不存在，会先初始化为 0 再自增，结果为 1。
     * 常用于计数器场景，如接口调用次数统计、登录失败次数等。
     * </p>
     *
     * @param key 要自增的缓存键
     * @return 自增后的值
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 为指定键设置过期时间。
     * <p>
     * 如果键不存在或已过期，返回 false。
     * </p>
     *
     * @param key     缓存键
     * @param timeout 过期时间数值
     * @param unit    过期时间单位
     * @return true 表示设置成功，false 表示键不存在
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }
}
