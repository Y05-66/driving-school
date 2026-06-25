package com.drivingschool.channel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 招生渠道实体类
 * 记录驾校的招生渠道信息，包括渠道名称、类型、联系方式、成本等
 *
 * @author drivingschool
 */
@Data
@TableName("channel")
public class Channel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 渠道名称
     */
    @TableField("name")
    private String name;

    /**
     * 渠道类型：1-线上，2-线下，3-转介绍，4-其他
     */
    @TableField("type")
    private Integer type;

    /**
     * 联系人姓名
     */
    @TableField("contact")
    private String contact;

    /**
     * 联系电话
     */
    @TableField("phone")
    private String phone;

    /**
     * 渠道成本/费用
     */
    @TableField("cost")
    private BigDecimal cost;

    /**
     * 渠道状态：0-停用，1-启用
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
