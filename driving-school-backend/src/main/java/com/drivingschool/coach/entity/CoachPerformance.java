package com.drivingschool.coach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 教练绩效实体类
 * 记录教练每月的绩效数据，包括课时数、学员数、通过率、评分等
 *
 * @author drivingschool
 */
@Data
@TableName("coach_performance")
public class CoachPerformance implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 教练ID
     */
    @TableField("coach_id")
    private Long coachId;

    /**
     * 统计月份，格式：YYYY-MM
     */
    @TableField("month")
    private String month;

    /**
     * 总课时数
     */
    @TableField("total_lessons")
    private Integer totalLessons;

    /**
     * 已完成课时数
     */
    @TableField("completed_lessons")
    private Integer completedLessons;

    /**
     * 学员数量
     */
    @TableField("student_count")
    private Integer studentCount;

    /**
     * 考试通过率（百分比）
     */
    @TableField("pass_rate")
    private BigDecimal passRate;

    /**
     * 学员平均评分
     */
    @TableField("avg_rating")
    private BigDecimal avgRating;

    /**
     * 投诉次数
     */
    @TableField("complaint_count")
    private Integer complaintCount;

    /**
     * 综合绩效得分
     */
    @TableField("score")
    private BigDecimal score;

    /**
     * 备注说明
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标志：0-未删除，1-已删除
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
