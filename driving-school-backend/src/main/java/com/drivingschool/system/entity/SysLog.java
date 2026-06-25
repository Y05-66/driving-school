package com.drivingschool.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统操作日志实体类
 * 对应数据库表：sys_log
 * 用于记录用户在系统中的操作行为，便于审计追踪和问题排查
 */
@Data
@TableName("sys_log")
public class SysLog {

    /** 日志ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 操作用户ID，关联sys_user表，记录执行操作的用户 */
    private Long userId;

    /** 操作用户名，冗余存储便于日志查询时直接展示，无需关联用户表 */
    private String username;

    /** 操作描述，对本次操作的文字说明（如："新增学员"、"修改课程"等） */
    private String operation;

    /** 请求方法，记录调用的Controller方法全路径（如：com.drivingschool.student.controller.StudentController.add） */
    private String method;

    /** 请求参数，记录本次请求的参数JSON字符串，便于问题复现 */
    private String params;

    /** 操作IP地址，记录发起请求的客户端IP，用于安全审计 */
    private String ip;

    /** 执行时长（毫秒），记录方法执行耗时，用于性能监控分析 */
    private Long duration;

    /** 创建时间，记录操作发生的时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
