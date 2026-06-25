package com.drivingschool.coach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教练评价实体类
 * 对应数据库表：coach_evaluation
 * 用于存储学员对教练的教学评价信息，支持按课程单次评价
 */
@Data
@TableName("coach_evaluation")
public class CoachEvaluation {

    /** 评价ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 教练ID，关联coach表，被评价的教练 */
    private Long coachId;

    /** 学员ID，关联student表，提交评价的学员 */
    private Long studentId;

    /** 课程ID，关联lesson表，本次评价对应的课程记录 */
    private Long lessonId;

    /** 评分，学员对教练本次教学的评分，取值范围1-5分 */
    private Integer rating;

    /** 评价内容，学员对教练教学的文字评价和建议 */
    private String content;

    /** 创建时间，记录评价提交的时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标志，0-未删除 1-已删除，实现软删除功能 */
    @TableLogic
    private Integer deleted;
}
