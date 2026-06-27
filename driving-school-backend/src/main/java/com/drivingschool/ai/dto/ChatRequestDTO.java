package com.drivingschool.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI聊天请求DTO
 * 用于接收用户的聊天请求
 */
@Data
public class ChatRequestDTO {

    /** 用户消息内容 */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000字")
    private String message;

    /** 会话ID，为空则新建会话 */
    private String conversationId;

    /** 使用的模型名称，为空则使用系统默认模型 */
    private String model;
}
