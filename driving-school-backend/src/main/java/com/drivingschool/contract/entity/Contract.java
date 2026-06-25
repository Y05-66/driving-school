package com.drivingschool.contract.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 电子合同实体类
 * <p>
 * 用于存储学员与驾校之间的电子合同信息，包括合同编号、金额、签署状态等。
 * 支持合同的创建、签署、作废等生命周期管理。
 * </p>
 *
 * @author drivingschool
 * @since 1.0.0
 */
@Data
@TableName("contract")
public class Contract {

    /**
     * 合同主键ID（雪花算法生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 学员ID
     * <p>关联学员表，标识合同所属学员</p>
     */
    private Long studentId;

    /**
     * 合同编号
     * <p>系统自动生成的唯一合同编号，格式：HT + 日期 + 序号</p>
     */
    private String contractNo;

    /**
     * 合同标题
     * <p>合同的名称，如"驾驶培训合同"</p>
     */
    private String title;

    /**
     * 合同内容
     * <p>合同的详细条款内容，支持富文本格式</p>
     */
    private String content;

    /**
     * 合同金额
     * <p>培训费用金额，精确到分</p>
     */
    private BigDecimal amount;

    /**
     * 申请类型
     * <p>报名类型，如C1、C2、B1等</p>
     */
    private String applyType;

    /**
     * 合同状态
     * <ul>
     *   <li>0 - 待签署：合同已创建，等待学员签署</li>
     *   <li>1 - 已签署：学员已完成签署</li>
     *   <li>2 - 已作废：合同被取消或作废</li>
     * </ul>
     */
    private Integer status;

    /**
     * 签署时间
     * <p>学员签署合同的时间</p>
     */
    private LocalDateTime signedTime;

    /**
     * 签署数据
     * <p>电子签名相关数据，如签名图片Base64、签署IP等</p>
     */
    private String signData;

    /**
     * 创建时间
     * <p>记录插入时自动填充</p>
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     * <p>记录插入或更新时自动填充</p>
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标志
     * <ul>
     *   <li>0 - 未删除</li>
     *   <li>1 - 已删除</li>
     * </ul>
     */
    @TableLogic
    private Integer deleted;
}
