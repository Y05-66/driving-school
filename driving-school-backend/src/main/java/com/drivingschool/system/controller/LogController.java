package com.drivingschool.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drivingschool.common.result.PageResult;
import com.drivingschool.common.result.R;
import com.drivingschool.system.entity.SysLog;
import com.drivingschool.system.service.SysLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 操作日志控制器
 * <p>
 * 功能说明：查询系统操作日志，支持按操作用户、操作类型、时间范围进行筛选。
 * 用于系统审计和操作追溯。
 * </p>
 * 基础路径：/logs
 * <p>
 * 所有接口均需要ADMIN角色权限，仅管理员可查看操作日志。
 * </p>
 *
 * @author drivingschool
 */
@Tag(name = "操作日志")
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class LogController {

    /** 日志服务，处理日志查询相关的业务逻辑 */
    private final SysLogService logService;

    /**
     * 日志列表查询接口
     * <p>
     * 分页查询系统操作日志，支持按操作用户ID、操作类型、开始时间和结束时间进行筛选。
     * 时间格式为 yyyy-MM-dd HH:mm:ss。
     * </p>
     *
     * @param userId    操作用户ID（可选，精确匹配）
     * @param operation 操作类型描述（可选，模糊匹配）
     * @param startTime 查询开始时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param endTime   查询结束时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param pageNum   页码，默认第1页
     * @param pageSize  每页条数，默认10条
     * @return 分页后的日志列表（PageResult格式）
     * @summary 查询操作日志列表
     * @权限要求 仅管理员（ADMIN）
     */
    @Operation(summary = "日志列表")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<PageResult<SysLog>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        // 构建分页对象
        Page<SysLog> page = new Page<>(pageNum, pageSize);
        // 执行分页查询
        Page<SysLog> result = logService.page(page, userId, operation, startTime, endTime);
        return R.ok(PageResult.of(result));
    }
}
