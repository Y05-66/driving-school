package com.drivingschool.vehicle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 车辆维保记录实体类
 * 对应数据库表：vehicle_maintenance
 * 用于记录教练车的保养、维修、年检、保险等维保信息，便于车辆全生命周期管理
 */
@Data
@TableName("vehicle_maintenance")
public class VehicleMaintenance {

    /** 维保记录ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 车辆ID，关联vehicle表，标识本次维保涉及的车辆 */
    private Long vehicleId;

    /** 维保类型，1-保养 2-维修 3-年检 4-保险 */
    private Integer type;

    /** 维保描述，对本次维保项目的详细说明（如：更换机油、钣金喷漆等） */
    private String description;

    /** 维保费用（元），本次维保产生的费用金额 */
    private BigDecimal cost;

    /** 维保日期，本次维保的实际执行日期 */
    private LocalDate maintenanceDate;

    /** 下次到期日，下次需要进行同类维保的提醒日期 */
    private LocalDate nextDueDate;

    /** 当时里程（公里），维保时车辆的里程表读数，用于追踪维保历史 */
    private Long mileage;

    /** 经办人，执行或记录本次维保的操作人员姓名 */
    private String operator;

    /** 创建时间，记录维保记录的创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，记录维保记录最后修改时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标志，0-未删除 1-已删除，实现软删除功能 */
    @TableLogic
    private Integer deleted;
}
