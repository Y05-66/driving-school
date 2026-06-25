package com.drivingschool.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 缴费记录实体类
 * 对应数据库表：payment
 * 用于存储学员的缴费和退费信息，是驾校财务管理的核心数据表
 */
@Data
@TableName("payment")
public class Payment {

    /** 支付记录ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 学员ID，关联student表，缴费的学员 */
    private Long studentId;

    /** 缴费类型，如：1-报名费 2-培训费 3-考试费 4-补考费 5-退费等 */
    private Integer type;

    /** 缴费金额（元），本次缴费或退费的金额 */
    private BigDecimal amount;

    /** 支付方式，如：1-现金 2-微信 3-支付宝 4-银行卡等 */
    private Integer payMethod;

    /** 支付时间，缴费实际发生的时间 */
    private LocalDateTime payTime;

    /** 收据编号，系统生成的缴费收据唯一编号，用于财务对账 */
    private String receiptNo;

    /** 备注，关于本次缴费的补充说明 */
    private String remark;

    /** 操作员ID，关联sys_user表，执行本次收费操作的工作人员 */
    private Long operatorId;

    /** 退费状态，0-正常（未退费） 1-已退费 */
    private Integer refundStatus;

    /** 退费时间，执行退费操作的时间，未退费时为null */
    private LocalDateTime refundTime;

    /** 原支付记录ID，退费时关联的原始缴费记录ID，用于追溯退费对应的原始交易 */
    private Long originalPaymentId;

    /** 创建时间，记录缴费记录的创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，记录缴费记录最后修改时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标志，0-未删除 1-已删除，实现软删除功能 */
    @TableLogic
    private Integer deleted;
}
