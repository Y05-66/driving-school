package com.drivingschool.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 学员创建请求DTO（数据传输对象）
 * 用于接收前端新增学员时提交的表单数据，包含学员基本信息和登录账号信息
 * 同时会在系统中创建学员记录和对应的系统登录用户
 */
@Data
public class StudentCreateDTO {

    /** 学员姓名，必填，不能为空白字符串 */
    @NotBlank(message = "姓名不能为空")
    private String name;

    /** 身份证号码，必填，用于学员身份核验和考试报名 */
    @NotBlank(message = "身份证号不能为空")
    private String idCard;

    /** 手机号码，选填，学员联系电话 */
    private String phone;

    /** 性别，选填，0-女 1-男 */
    private Integer gender;

    /** 出生日期，选填，学员的出生年月日 */
    private LocalDate birthday;

    /** 联系地址，选填，学员的家庭住址 */
    private String address;

    /** 申请类型，选填，报考的驾照类型（如：C1、C2等） */
    private String applyType;

    /** 备注，选填，关于学员的补充说明 */
    private String remark;

    /** 登录用户名，必填，长度限制4-20个字符，用于创建学员的系统登录账号 */
    @NotBlank(message = "登录用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度4-20位")
    private String username;

    /** 登录密码，必填，长度限制6-20个字符，用于创建学员的系统登录密码 */
    @NotBlank(message = "登录密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    private String password;
}
