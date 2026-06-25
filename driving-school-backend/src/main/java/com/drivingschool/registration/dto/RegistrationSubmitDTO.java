package com.drivingschool.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 在线报名提交请求DTO
 * <p>
 * 用于小程序端提交报名申请，仅包含允许客户端填写的字段。
 * 避免直接使用实体类绑定请求，防止客户端篡改内部字段（如status、reviewerId等）。
 * </p>
 *
 * @author drivingschool
 */
@Data
public class RegistrationSubmitDTO {

    /** 报名者姓名，真实中文姓名 */
    @NotBlank(message = "姓名不能为空")
    private String name;

    /** 身份证号码，18位 */
    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "身份证号格式不正确，应为18位")
    private String idCard;

    /** 手机号码 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 性别，0-女 1-男 */
    private Integer gender;

    /** 出生日期 */
    private LocalDate birthday;

    /** 联系地址 */
    private String address;

    /** 报考类型，如：C1、C2 */
    @NotBlank(message = "报考类型不能为空")
    private String applyType;

    /** 班型，如：普通班、VIP班 */
    private String classType;

    /** 身份证正面照片URL */
    private String idCardFront;

    /** 身份证反面照片URL */
    private String idCardBack;

    /** 备注 */
    private String remark;
}
