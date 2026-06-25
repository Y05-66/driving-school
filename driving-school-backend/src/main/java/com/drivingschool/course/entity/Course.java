package com.drivingschool.course.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程实体类
 * 对应数据库表：course
 * 用于存储驾校开设的课程基本信息，定义课程名称、适用类型及收费标准等
 */
@Data
@TableName("course")
public class Course {

    /** 课程ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 课程名称，如：科目二场地训练、科目三道路驾驶等 */
    private String name;

    /** 适用类型，该课程适用的驾照类型（如：C1、C2等） */
    private String applyType;

    /** 总学时数，该课程规定的总学习课时数 */
    private Integer totalHours;

    /** 每课时单价（元），该课程每个学时的收费标准 */
    private BigDecimal pricePerHour;

    /** 课程描述，对课程内容、教学目标等的详细说明 */
    private String description;

    /** 创建时间，记录课程信息的创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，记录课程信息最后修改时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标志，0-未删除 1-已删除，实现软删除功能 */
    @TableLogic
    private Integer deleted;
}
