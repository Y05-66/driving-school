package com.drivingschool.practice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 练习记录实体类
 * 对应数据库表：practice_record
 * 用于记录学员的每次刷题作答情况，支持错题本和刷题统计功能
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>studentId - 关联学员表的学员ID</li>
 *   <li>questionId - 关联题库表的题目ID</li>
 *   <li>studentAnswer - 学员提交的答案</li>
 *   <li>isCorrect - 作答是否正确，0-错误 1-正确</li>
 *   <li>practiceTime - 本次作答所用时间（秒）</li>
 * </ul>
 */
@Data
@TableName("practice_record")
public class PracticeRecord {

    /** 记录ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 学员ID，关联student表，标识是哪位学员的作答记录 */
    private Long studentId;

    /** 题目ID，关联question_bank表，标识作答的是哪道题 */
    private Long questionId;

    /** 学员提交的答案，格式与题库answer字段一致（单选如"A"，多选如"ABD"，判断如"A"或"B"） */
    private String studentAnswer;

    /** 作答是否正确，0-错误 1-正确 */
    private Integer isCorrect;

    /** 练习所用时间，学员作答本题所花费的时间（单位：秒） */
    private Integer practiceTime;

    /** 创建时间，记录本次作答的时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标志，0-未删除 1-已删除，实现软删除功能 */
    @TableLogic
    private Integer deleted;
}
