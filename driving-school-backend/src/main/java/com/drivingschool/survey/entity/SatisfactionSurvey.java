package com.drivingschool.survey.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 满意度调查实体类
 * 对应数据库表：satisfaction_survey
 * 用于存储学员在完成课时后对教练教学的满意度评价
 *
 * <p>评分维度说明（均为1-5分制）：</p>
 * <ul>
 *   <li>teachingRating - 教学质量评分，衡量教练的专业教学能力</li>
 *   <li>attitudeRating - 服务态度评分，衡量教练的服务态度和沟通方式</li>
 *   <li>environmentRating - 教学环境评分，衡量训练场地和车辆条件</li>
 *   <li>overallRating - 综合满意度评分，学员对本次教学体验的总体评价</li>
 * </ul>
 */
@Data
@TableName("satisfaction_survey")
public class SatisfactionSurvey {

    /** 调查记录ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 课时ID，关联lesson表，标识本次评价对应的课时记录 */
    private Long lessonId;

    /** 学员ID，关联student表，标识提交评价的学员 */
    private Long studentId;

    /** 教练ID，关联coach表，标识被评价的教练 */
    private Long coachId;

    /** 教学质量评分，1-5分，1分为最低，5分为最高 */
    private Integer teachingRating;

    /** 服务态度评分，1-5分，1分为最低，5分为最高 */
    private Integer attitudeRating;

    /** 教学环境评分，1-5分，1分为最低，5分为最高 */
    private Integer environmentRating;

    /** 综合满意度评分，1-5分，1分为最低，5分为最高 */
    private Integer overallRating;

    /** 评价内容，学员对本次教学的文字评价 */
    private String content;

    /** 改进建议，学员对驾校或教练提出的改进建议 */
    private String suggestion;

    /** 创建时间，记录评价提交的时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标志，0-未删除 1-已删除，实现软删除功能 */
    @TableLogic
    private Integer deleted;
}
