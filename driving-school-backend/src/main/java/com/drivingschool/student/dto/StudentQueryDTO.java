package com.drivingschool.student.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 学员查询条件DTO（数据传输对象）
 * 用于接收前端学员列表查询时的筛选条件和分页参数
 * 支持按姓名、身份证、手机号、状态、申请类型等条件组合查询
 */
@Data
public class StudentQueryDTO {

    /** 学员姓名，模糊匹配查询 */
    private String name;

    /** 身份证号码，精确匹配查询 */
    private String idCard;

    /** 手机号码，模糊匹配查询 */
    private String phone;

    /** 学员状态，精确匹配查询（如：0-已报名 1-学习中 2-已结业等） */
    private Integer status;

    /** 申请类型，精确匹配查询（如：C1、C2等） */
    private String applyType;

    /** 页码，默认第1页，最小值为1 */
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    /** 每页条数，默认每页10条记录，范围1-100 */
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private Integer pageSize = 10;
}
