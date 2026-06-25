package com.drivingschool.reminder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 自动提醒实体类
 * <p>
 * 用于存储系统自动提醒信息，支持课前提醒、考试提醒、合同提醒、保险提醒等多种类型。
 * 支持系统消息、短信、微信等多种发送方式。
 * </p>
 *
 * @author drivingschool
 * @since 1.0.0
 */
@Data
@TableName("reminder")
public class Reminder {

    /**
     * 提醒主键ID（雪花算法生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 目标用户ID
     * <p>接收提醒的用户ID，可以是学员、教练或管理员</p>
     */
    private Long targetUserId;

    /**
     * 提醒类型
     * <ul>
     *   <li>1 - 课前提醒：课程开始前的提醒</li>
     *   <li>2 - 考试提醒：考试日期临近的提醒</li>
     *   <li>3 - 合同提醒：合同签署、到期等提醒</li>
     *   <li>4 - 保险提醒：保险到期、续保等提醒</li>
     *   <li>5 - 自定义提醒：管理员自定义的提醒</li>
     * </ul>
     */
    private Integer type;

    /**
     * 提醒标题
     * <p>提醒消息的标题，简要描述提醒内容</p>
     */
    private String title;

    /**
     * 提醒内容
     * <p>提醒消息的详细内容</p>
     */
    private String content;

    /**
     * 提醒时间
     * <p>计划发送提醒的时间，系统会在此时间发送提醒</p>
     */
    private LocalDateTime remindTime;

    /**
     * 关联业务ID
     * <p>与提醒相关的业务对象ID，如课程ID、合同ID等</p>
     */
    private Long relatedId;

    /**
     * 关联业务类型
     * <p>标识relatedId对应的业务类型，如lesson、contract等</p>
     */
    private String relatedType;

    /**
     * 提醒状态
     * <ul>
     *   <li>0 - 待发送：等待发送时间到达</li>
     *   <li>1 - 已发送：提醒已成功发送</li>
     *   <li>2 - 已取消：提醒被取消</li>
     * </ul>
     */
    private Integer status;

    /**
     * 发送方式
     * <ul>
     *   <li>1 - 系统消息：通过系统内部消息通知</li>
     *   <li>2 - 短信：通过短信方式发送</li>
     *   <li>3 - 微信：通过微信模板消息发送</li>
     * </ul>
     */
    private Integer sendMethod;

    /**
     * 创建时间
     * <p>记录插入时自动填充</p>
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     * <p>记录插入或更新时自动填充</p>
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标志
     * <ul>
     *   <li>0 - 未删除</li>
     *   <li>1 - 已删除</li>
     * </ul>
     */
    @TableLogic
    private Integer deleted;
}
