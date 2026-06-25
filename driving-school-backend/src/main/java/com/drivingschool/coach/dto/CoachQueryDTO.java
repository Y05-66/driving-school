package com.drivingschool.coach.dto;

import lombok.Data;

/**
 * 教练查询条件DTO（数据传输对象）
 * 用于接收前端教练列表查询时的筛选条件和分页参数
 * 支持按姓名、手机号、教练类型、状态等条件组合查询
 */
@Data
public class CoachQueryDTO {

    /** 教练姓名，模糊匹配查询 */
    private String name;

    /** 手机号码，模糊匹配查询 */
    private String phone;

    /** 教练类型，精确匹配查询（如：C1教练、C2教练等） */
    private String coachType;

    /** 教练状态，精确匹配查询（如：0-离职 1-在职 2-休假等） */
    private Integer status;

    /** 页码，默认第1页 */
    private Integer pageNum = 1;

    /** 每页条数，默认每页10条记录 */
    private Integer pageSize = 10;
}
