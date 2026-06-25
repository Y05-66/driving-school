package com.drivingschool.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考试实体类
 * 对应数据库表：exam
 * 用于存储驾校组织或关联的考试信息，包括考试类型、时间、地点及名额等
 */
@Data
@TableName("exam")
public class Exam {

    /** 考试ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 考试类型，如：科目一（理论）、科目二（场地）、科目三（路考）、科目四（理论） */
    @NotBlank(message = "考试类型不能为空")
    private String examType;

    /** 考试日期，考试举行的具体日期 */
    @NotNull(message = "考试日期不能为空")
    private LocalDate examDate;

    /** 考试地点，考试举行的具体地点或考场名称 */
    private String examLocation;

    /** 最大考生人数，本场考试允许报名的最大考生数量 */
    @NotNull(message = "最大考生人数不能为空")
    @Min(value = 1, message = "最大考生人数至少为1")
    private Integer maxCandidates;

    /** 适用类型，该考试适用的驾照类型（如：C1、C2等） */
    private String applyType;

    /** 创建时间，记录考试信息的创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，记录考试信息最后修改时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标志，0-未删除 1-已删除，实现软删除功能 */
    @TableLogic
    private Integer deleted;
}
