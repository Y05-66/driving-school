package com.drivingschool.course.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 课程预约/课时记录实体类
 * 对应数据库表：lesson
 * 用于记录学员的每一次课程预约和上课信息，是排课和考勤的核心数据表
 */
@Data
@TableName("lesson")
public class Lesson {

    /** 课时记录ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 学员ID，关联student表，预约上课的学员 */
    private Long studentId;

    /** 教练ID，关联coach表，负责本次教学的教练 */
    private Long coachId;

    /** 车辆ID，关联vehicle表，本次课程使用的教练车 */
    private Long vehicleId;

    /** 课程ID，关联course表，本次课时所属的课程 */
    private Long courseId;

    /** 上课日期，本次课程的授课日期 */
    private LocalDate lessonDate;

    /** 开始时间，本次课程的计划开始时刻 */
    private LocalTime startTime;

    /** 结束时间，本次课程的计划结束时刻 */
    private LocalTime endTime;

    /** 课时状态，如：0-已预约 1-进行中 2-已完成 3-已取消 4-缺勤等 */
    private Integer status;

    /** 实际学时（小时），本次课程的实际授课时长，可与计划时长不同 */
    private BigDecimal actualHours;

    /** 备注，关于本次课时的补充说明（如天气影响、特殊事项等） */
    private String remark;

    /** 创建时间，记录课时记录的创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，记录课时记录最后修改时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标志，0-未删除 1-已删除，实现软删除功能 */
    @TableLogic
    private Integer deleted;
}
