package com.drivingschool.course.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 时间段模板实体类
 * 对应数据库表：time_slot_template
 * 用于定义可预约的课程时间段模板，支持按星期配置不同时间段及其容量限制
 */
@Data
@TableName("time_slot_template")
public class TimeSlotTemplate {

    /** 模板ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 模板名称，时间段模板的描述性名称（如：上午第一班、下午第二班等） */
    private String name;

    /** 适用星期，该模板适用的星期几，多个值用逗号分隔（1=周一,2=周二,...,7=周日） */
    private String dayOfWeek;

    /** 开始时间，该时间段的起始时刻 */
    private LocalTime startTime;

    /** 结束时间，该时间段的截止时刻 */
    private LocalTime endTime;

    /** 最大预约人数，该时间段内允许的最大同时预约学员数量，用于控制教学资源 */
    private Integer maxCapacity;

    /** 模板状态，0-禁用 1-启用，控制该时间段模板是否可用于预约 */
    private Integer status;

    /** 创建时间，记录模板的创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，记录模板最后修改时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标志，0-未删除 1-已删除，实现软删除功能 */
    @TableLogic
    private Integer deleted;
}
