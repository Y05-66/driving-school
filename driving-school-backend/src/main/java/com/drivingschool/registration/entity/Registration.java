package com.drivingschool.registration.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 在线报名实体类
 * 对应数据库表：registration
 * <p>
 * 学员通过小程序端提交报名申请，工作人员在PC端审核。
 * 审核通过后自动创建学员档案和登录账号。
 * </p>
 *
 * @author drivingschool
 */
@Data
@TableName("registration")
public class Registration {

    /** 报名ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 报名者姓名，真实中文姓名 */
    @NotBlank(message = "姓名不能为空")
    private String name;

    /** 身份证号码，18位，用于身份核验和学员档案创建 */
    @NotBlank(message = "身份证号不能为空")
    @jakarta.validation.constraints.Pattern(regexp = "^\\d{17}[\\dXx]$", message = "身份证号格式不正确，应为18位")
    private String idCard;

    /** 手机号码，用于联系和报名状态查询 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 性别，0-女 1-男 */
    private Integer gender;

    /** 出生日期，从身份证号解析或用户填写 */
    private java.time.LocalDate birthday;

    /** 联系地址 */
    private String address;

    /** 报考类型，如：C1-小型汽车、C2-小型自动挡汽车 */
    @NotBlank(message = "报考类型不能为空")
    private String applyType;

    /** 班型，如：普通班、VIP班、周末班等 */
    private String classType;

    /** 身份证正面照片URL */
    private String idCardFront;

    /** 身份证反面照片URL */
    private String idCardBack;

    /** 备注，报名者填写的补充说明 */
    private String remark;

    /**
     * 报名状态
     * <ul>
     *   <li>0-待审核：默认状态，等待工作人员审核</li>
     *   <li>1-已通过：审核通过，已创建学员档案</li>
     *   <li>2-已驳回：审核不通过，需修改后重新提交</li>
     * </ul>
     */
    private Integer status;

    /** 审核人ID，关联sys_user表，记录执行审核操作的工作人员 */
    private Long reviewerId;

    /** 审核意见，工作人员填写的审核备注或驳回原因 */
    private String reviewComment;

    /** 审核时间，工作人员执行审核操作的时间 */
    private LocalDateTime reviewTime;

    /** 关联学员ID，审核通过后自动创建学员档案并关联 */
    private Long studentId;

    /** 关联用户ID，审核通过后自动创建登录账号并关联 */
    private Long userId;

    /** 创建时间，报名提交时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，最后修改时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标志，0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
