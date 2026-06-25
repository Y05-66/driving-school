package com.drivingschool.coach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 教练排班实体类
 * 对应数据库表：coach_schedule
 * 教练设置可预约的时间段
 */
@Data
@TableName("coach_schedule")
public class CoachSchedule {

    /** 排班ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 教练ID */
    private Long coachId;

    /** 排班日期 */
    private LocalDate scheduleDate;

    /** 开始时间 */
    private LocalTime startTime;

    /** 结束时间 */
    private LocalTime endTime;

    /** 最大学员数（该时段可预约人数） */
    private Integer maxStudents;

    /** 已预约学员数 */
    private Integer bookedCount;

    /** 状态：0-可用 1-已满 2-已关闭 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private java.time.LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private java.time.LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
