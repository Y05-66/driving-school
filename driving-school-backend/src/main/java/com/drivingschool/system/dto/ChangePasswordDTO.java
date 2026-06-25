package com.drivingschool.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求DTO（数据传输对象）
 * 用于接收用户修改登录密码时提交的表单数据
 * 需要验证旧密码正确后才能设置新密码
 */
@Data
public class ChangePasswordDTO {

    /** 旧密码，必填，用于验证用户身份，确认是本人操作 */
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    /** 新密码，必填，长度限制6-20个字符，将替换原有登录密码 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    private String newPassword;
}
