package com.drivingschool.registration.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报名状态查询响应VO
 * <p>
 * 用于返回给小程序端的报名状态信息，仅包含客户端需要的字段。
 * 避免返回完整的实体对象，防止泄露内部信息（如reviewerId、userId等）。
 * </p>
 *
 * @author drivingschool
 */
@Data
public class RegistrationStatusVO {

    /** 报名ID */
    private Long id;

    /** 报名者姓名 */
    private String name;

    /** 手机号 */
    private String phone;

    /** 报考类型 */
    private String applyType;

    /**
     * 报名状态
     * <ul>
     *   <li>0-待审核</li>
     *   <li>1-已通过</li>
     *   <li>2-已驳回</li>
     * </ul>
     */
    private Integer status;

    /** 审核意见（驳回时显示） */
    private String reviewComment;

    /** 审核时间 */
    private LocalDateTime reviewTime;

    /** 提交时间 */
    private LocalDateTime createTime;
}
