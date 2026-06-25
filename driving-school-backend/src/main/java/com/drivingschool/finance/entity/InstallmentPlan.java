package com.drivingschool.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分期付款计划实体类
 * 记录学员的分期付款总体信息，包括总金额、已付金额、期数等
 *
 * @author drivingschool
 */
@Data
@TableName("installment_plan")
public class InstallmentPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 学员ID
     */
    @TableField("student_id")
    private Long studentId;

    /**
     * 关联合同ID
     */
    @TableField("contract_id")
    private Long contractId;

    /**
     * 分期总金额
     */
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /**
     * 已付金额
     */
    @TableField("paid_amount")
    private BigDecimal paidAmount;

    /**
     * 总期数
     */
    @TableField("installments")
    private Integer installments;

    /**
     * 已付期数
     */
    @TableField("paid_installments")
    private Integer paidInstallments;

    /**
     * 计划状态：0-进行中，1-已结清，2-逾期
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
