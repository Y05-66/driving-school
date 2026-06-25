package com.drivingschool.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试报名记录实体类
 * 对应数据库表：exam_enrollment
 * 用于记录学员的考试报名信息及考试结果，是学员与考试之间的关联表
 */
@Data
@TableName("exam_enrollment")
public class ExamEnrollment {

    /** 报名记录ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 学员ID，关联student表，报名考试的学员 */
    private Long studentId;

    /** 考试ID，关联exam表，学员报名的考试场次 */
    private Long examId;

    /** 报名时间，学员提交考试报名申请的时间 */
    private LocalDateTime applyTime;

    /** 报名状态，如：0-已报名 1-已确认 2-已取消 3-已考试等 */
    private Integer status;

    /** 考试成绩，学员本次考试的得分，未考试时为null */
    private Integer score;

    /** 备注，关于本次报名或考试的补充说明 */
    private String remark;

    /** 创建时间，记录报名记录的创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，记录报名记录最后修改时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标志，0-未删除 1-已删除，实现软删除功能 */
    @TableLogic
    private Integer deleted;
}
