package com.drivingschool.vehicle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 教练车实体类
 * 对应数据库表：vehicle
 * 用于存储驾校教学车辆的基本信息，包括车辆资料、保险年检等管理信息
 */
@Data
@TableName("vehicle")
public class Vehicle {

    /** 车辆ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 车牌号码，车辆的注册号牌（如：京A12345） */
    private String plateNo;

    /** 车辆品牌，车辆的制造商品牌（如：大众、丰田等） */
    private String brand;

    /** 车辆型号，具体车型名称（如：捷达、卡罗拉等） */
    private String model;

    /** 车型类别，车辆所属的驾照类别（如：C1、C2等） */
    private String carType;

    /** 车身颜色，车辆外观颜色 */
    private String color;

    /** 购入日期，驾校购买或注册该车辆的日期 */
    private LocalDate buyDate;

    /** 保险到期日，车辆商业保险和交强险的到期日期，到期需续保 */
    private LocalDate insuranceExpire;

    /** 年检到期日，车辆年检有效期截止日期，到期需年检 */
    private LocalDate inspectionExpire;

    /** 车辆状态，如：0-停用 1-正常 2-维修中 3-报废等 */
    private Integer status;

    /** 累计里程（公里），车辆的总行驶里程数，用于维护保养参考 */
    private Long mileage;

    /** 创建时间，记录车辆信息的创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，记录车辆信息最后修改时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标志，0-未删除 1-已删除，实现软删除功能 */
    @TableLogic
    private Integer deleted;
}
