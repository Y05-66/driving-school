package com.drivingschool.vehicle.dto;

import lombok.Data;

/**
 * 车辆查询条件DTO（数据传输对象）
 * 用于接收前端车辆列表查询时的筛选条件和分页参数
 * 支持按车牌号、品牌、车型类别、状态等条件组合查询
 */
@Data
public class VehicleQueryDTO {

    /** 车牌号码，模糊匹配查询 */
    private String plateNo;

    /** 车辆品牌，模糊匹配查询（如：大众、丰田等） */
    private String brand;

    /** 车型类别，精确匹配查询（如：C1、C2等） */
    private String carType;

    /** 车辆状态，精确匹配查询（如：0-停用 1-正常 2-维修中等） */
    private Integer status;

    /** 页码，默认第1页 */
    private Integer pageNum = 1;

    /** 每页条数，默认每页10条记录 */
    private Integer pageSize = 10;
}
