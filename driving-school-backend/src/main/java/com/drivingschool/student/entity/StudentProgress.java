package com.drivingschool.student.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学员学习进度实体类
 * 对应数据库表：student_progress
 * 用于记录学员各科目（科目一至科目四）的学习进度，追踪学时完成情况
 */
@Data
@TableName("student_progress")
public class StudentProgress {

    /** 进度记录ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 学员ID，关联student表，标识该进度记录所属的学员 */
    private Long studentId;

    /** 科目名称，如：科目一（理论）、科目二（场地）、科目三（路考）、科目四（理论） */
    private String subjectName;

    /** 要求学时数，该科目规定的最低学习学时要求 */
    private Integer requiredHours;

    /** 已完成学时数，学员在该科目已实际完成的学习学时 */
    private Integer completedHours;

    /** 学习状态，0-未开始 1-进行中 2-已完成 */
    private Integer status;

    /** 开始日期，学员开始学习该科目的日期 */
    private LocalDate startDate;

    /** 完成日期，学员完成该科目学习的日期，未完成时为null */
    private LocalDate completeDate;

    /** 创建时间，记录进度记录的创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，记录进度记录最后修改时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标志，0-未删除 1-已删除，实现软删除功能 */
    @TableLogic
    private Integer deleted;
}
