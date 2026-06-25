package com.drivingschool.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体类
 * 对应数据库表：sys_user
 * 用于存储系统中所有用户的基本信息，包括管理员、教练、学员等角色的登录账号信息
 */
@Data
@TableName("sys_user")
public class SysUser {

    /** 用户ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 登录用户名，用于系统登录认证，不可重复 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 登录密码，存储加密后的密码密文 */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 手机号码，用于联系和接收短信通知 */
    private String phone;

    /** 真实姓名，用户的真实中文姓名 */
    private String realName;

    /** 头像地址，存储用户头像图片的访问路径 */
    private String avatar;

    /** 用户角色，标识用户在系统中的权限角色（如：ADMIN-管理员、COACH-教练、STUDENT-学员） */
    private String role;

    /** 账号状态，0-禁用 1-启用，用于控制用户是否可以登录系统 */
    private Integer status;

    /** 创建时间，记录用户账号的创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，记录用户信息最后修改时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标志，0-未删除 1-已删除，实现软删除功能 */
    @TableLogic
    private Integer deleted;
}
