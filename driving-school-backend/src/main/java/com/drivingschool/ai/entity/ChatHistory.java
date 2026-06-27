package com.drivingschool.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI聊天记录实体类
 * 对应数据库表：chat_history
 * 存储用户与AI助手的对话记录，支持多轮对话
 */
@Data
@TableName("chat_history")
public class ChatHistory {

    /** 记录ID，主键，使用雪花算法自动生成分布式唯一ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户ID，关联sys_user表 */
    private Long userId;

    /** 会话ID，用于标识一次对话会话，由前端生成UUID */
    private String conversationId;

    /** 消息角色，user-用户消息 assistant-AI回复 */
    private String role;

    /** 消息内容 */
    private String content;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标志，0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
