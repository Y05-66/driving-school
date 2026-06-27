package com.drivingschool.ai.dto;

import lombok.Data;

/**
 * AI聊天响应DTO（同步接口使用）
 * 用于返回完整的AI回复
 */
@Data
public class ChatResponseDTO {

    /** AI回复内容 */
    private String reply;

    /** 会话ID */
    private String conversationId;

    /** 如果发生了模型降级，此处为实际使用的模型名称；未降级则为null */
    private String modelSwitched;
}
