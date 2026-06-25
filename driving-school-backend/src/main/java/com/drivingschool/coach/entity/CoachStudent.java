package com.drivingschool.coach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 教练-学员分配关系实体类
 * 对应数据库表：coach_student
 * 用于记录教练与学员之间的教学分配关系，支持一个学员不同科目由不同教练负责
 */
@Data
@TableName("coach_student")
public class CoachStudent {

    /** 分配记录ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 教练ID，关联coach表，标识负责教学的教练 */
    private Long coachId;

    /** 学员ID，关联student表，标识被分配的学员 */
    private Long studentId;

    /** 负责科目，该教练负责教授学员的具体科目（如：科目二、科目三） */
    private String subjectName;

    /** 分配日期，教练与学员建立教学关系的日期 */
    private LocalDate assignDate;

    /** 分配状态，0-进行中 1-已完成 2-已解除 */
    private Integer status;

    /** 备注，关于该分配关系的补充说明（如换教练原因等） */
    private String remark;

    /** 创建时间，记录分配记录的创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，记录分配记录最后修改时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标志，0-未删除 1-已删除，实现软删除功能 */
    @TableLogic
    private Integer deleted;
}
