package com.drivingschool.notification.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知消息实体类
 * 对应数据库表：notification
 * 用于存储系统发送的各类通知消息，支持按角色广播和指定用户发送
 */
@Data
@TableName("notification")
public class Notification {

    /** 通知ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 通知标题，通知消息的简要概括 */
    private String title;

    /** 通知内容，通知消息的详细正文内容 */
    private String content;

    /** 通知类型，1-系统通知 2-考试通知 3-课程通知 4-紧急通知 */
    private Integer type;

    /** 目标角色，通知的接收角色范围，ALL-全部用户 STUDENT-学员 COACH-教练 STAFF-工作人员 */
    private String targetRole;

    /** 指定用户ID，当需要向特定用户发送通知时填写，为空则按targetRole进行角色广播 */
    private Long targetUserId;

    /** 发布者ID，关联sys_user表，发送该通知的管理员或系统账号 */
    private Long publisherId;

    /** 已读状态，0-未读 1-已读，标记接收用户是否已阅读该通知 */
    private Integer isRead;

    /** 创建时间，记录通知发送的时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标志，0-未删除 1-已删除，实现软删除功能 */
    @TableLogic
    private Integer deleted;
}
