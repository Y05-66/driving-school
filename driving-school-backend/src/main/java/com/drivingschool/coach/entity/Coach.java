package com.drivingschool.coach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 教练员实体类
 * 对应数据库表：coach
 * 用于存储驾校教练的基本信息，包括个人资料、资质信息及教学评价等
 */
@Data
@TableName("coach")
public class Coach {

    /** 教练ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 教练姓名，教练的真实中文姓名 */
    private String name;

    /** 手机号码，教练的联系电话 */
    private String phone;

    /** 身份证号码，教练的18位身份证号，用于身份核验 */
    private String idCard;

    /** 性别，0-女 1-男 */
    private Integer gender;

    /** 教练类型，如：C1教练、C2教练、B2教练等，标识教练可教授的车型类别 */
    private String coachType;

    /** 教练证号，教练员资格证书编号 */
    private String licenseNo;

    /** 教练证有效期，教练资格证书的到期日期，到期需续期 */
    private LocalDate licenseExpire;

    /** 从业年限，教练从事驾驶培训工作的年数 */
    private Integer experienceYears;

    /** 综合评分，学员对教练的平均评价分数（满分5分） */
    private BigDecimal rating;

    /** 教练状态，如：0-离职 1-在职 2-休假等 */
    private Integer status;

    /** 关联用户ID，关联sys_user表，教练对应的系统登录账号 */
    private Long userId;

    /** 创建时间，记录教练信息的创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，记录教练信息最后修改时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标志，0-未删除 1-已删除，实现软删除功能 */
    @TableLogic
    private Integer deleted;
}
