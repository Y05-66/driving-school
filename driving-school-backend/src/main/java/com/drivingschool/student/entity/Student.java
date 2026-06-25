package com.drivingschool.student.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学员实体类
 * 对应数据库表：student
 * 用于存储驾校学员的基本信息，包括个人信息、报名信息及学习状态等
 */
@Data
@TableName("student")
public class Student {

    /** 学员ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 学员姓名，学员的真实中文姓名 */
    private String name;

    /** 身份证号码，学员的18位身份证号，用于身份核验和考试报名 */
    private String idCard;

    /** 手机号码，学员的联系电话，用于通知和沟通 */
    private String phone;

    /** 性别，0-女 1-男 */
    private Integer gender;

    /** 出生日期，学员的出生年月日 */
    private LocalDate birthday;

    /** 联系地址，学员的家庭住址或通讯地址 */
    private String address;

    /** 照片地址，存储学员证件照或报名照片的访问路径 */
    private String photo;

    /** 学员状态，如：0-已报名 1-学习中 2-已结业 3-已退学等 */
    private Integer status;

    /** 报名日期，学员在驾校的报名注册日期 */
    private LocalDate registerDate;

    /** 申请类型，报考的驾照类型（如：C1-小型汽车、C2-小型自动挡汽车等） */
    private String applyType;

    /** 关联用户ID，关联sys_user表，学员对应的系统登录账号 */
    private Long userId;

    /** 备注，关于学员的补充说明信息 */
    private String remark;

    /** 创建时间，记录学员信息的创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，记录学员信息最后修改时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标志，0-未删除 1-已删除，实现软删除功能 */
    @TableLogic
    private Integer deleted;
}
