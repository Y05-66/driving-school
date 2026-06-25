package com.drivingschool.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 分期付款明细实体类
 * 记录每一期的还款信息，包括应还金额、应还日期、实付时间等
 *
 * @author drivingschool
 */
@Data
@TableName("installment_detail")
public class InstallmentDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 分期计划ID
     */
    @TableField("plan_id")
    private Long planId;

    /**
     * 第几期（从1开始）
     */
    @TableField("period")
    private Integer period;

    /**
     * 本期应还金额
     */
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 应还日期
     */
    @TableField("due_date")
    private LocalDate dueDate;

    /**
     * 实际支付时间
     */
    @TableField("pay_time")
    private LocalDateTime payTime;

    /**
     * 关联支付记录ID
     */
    @TableField("payment_id")
    private Long paymentId;

    /**
     * 明细状态：0-待还，1-已还，2-逾期
     */
    @TableField("status")
    private Integer status;

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
