package com.drivingschool.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.system.entity.SysLog;
import com.drivingschool.system.mapper.SysLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 系统操作日志服务类
 * <p>
 * 提供操作日志的异步保存和分页查询功能。
 * 日志通过AOP切面（{@link com.drivingschool.common.config.LogAspect}）自动记录，
 * 使用异步线程写入数据库，避免影响业务接口响应速度。
 * </p>
 *
 * @author drivingschool
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SysLogService {

    /** 操作日志数据访问层 */
    private final SysLogMapper logMapper;

    /**
     * 异步保存操作日志
     * <p>
     * 使用 @Async 注解在独立线程中执行日志写入，
     * 线程池由 {@link com.drivingschool.common.config.AsyncConfig} 配置。
     * </p>
     *
     * @param log 操作日志实体
     */
    @Async("asyncExecutor")
    public void save(SysLog log) {
        logMapper.insert(log);
    }

    /**
     * 分页查询操作日志
     * <p>
     * 支持按用户ID、操作描述、时间范围进行筛选，按创建时间倒序排列。
     * </p>
     *
     * @param page      分页参数
     * @param userId    用户ID（可选）
     * @param operation 操作描述关键词（可选，模糊匹配）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 分页结果
     */
    public Page<SysLog> page(Page<SysLog> page, Long userId, String operation,
                             LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, SysLog::getUserId, userId)
                .like(operation != null, SysLog::getOperation, operation)
                .ge(startTime != null, SysLog::getCreateTime, startTime)
                .le(endTime != null, SysLog::getCreateTime, endTime)
                .orderByDesc(SysLog::getCreateTime);
        return logMapper.selectPage(page, wrapper);
    }
}
