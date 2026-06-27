package com.drivingschool.ai.controller;

import com.drivingschool.ai.dto.ChatRequestDTO;
import com.drivingschool.ai.dto.ChatResponseDTO;
import com.drivingschool.ai.entity.ChatHistory;
import com.drivingschool.ai.service.AiService;
import com.drivingschool.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * AI助手控制器
 * <p>
 * 提供基于阿里云百炼平台的AI知识问答功能，包括：
 * 1. 流式对话（SSE）- 逐字推送AI回复，前端实现打字机效果
 * 2. 同步对话 - 一次性返回完整回复，供小程序端使用
 * 3. 会话管理 - 查看会话列表、加载历史记录、删除会话
 * 4. 模型列表 - 获取可用的AI模型列表
 * </p>
 * <p>
 * 接口权限说明：
 * - 所有接口均需要登录认证
 * - 无角色限制，所有角色（管理员、前台、教练、学员）均可使用
 * </p>
 * 基础路径：/ai
 *
 * @author drivingschool
 */
@Tag(name = "AI助手")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    /** AI助手服务，处理对话的核心业务逻辑 */
    private final AiService aiService;

    /**
     * 流式对话接口（SSE）
     * <p>
     * 使用Server-Sent Events实现流式输出，前端可以逐字显示AI回复。
     * 支持多轮对话，通过conversationId关联历史消息。
     * 当模型额度不足时自动切换到备用模型。
     * </p>
     *
     * @param authentication Spring Security认证对象，包含当前用户身份信息
     * @param dto            聊天请求DTO（包含消息内容、可选的会话ID和模型选择）
     * @return SseEmitter 流式响应，包含message/done/model-switch/error事件
     * @summary AI流式对话
     * @权限要求 需要登录（所有角色均可使用）
     */
    @Operation(summary = "AI流式对话")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(Authentication authentication, @Valid @RequestBody ChatRequestDTO dto) {
        // 获取当前登录用户ID
        Long userId = (Long) authentication.getPrincipal();
        return aiService.streamChat(userId, dto);
    }

    /**
     * 获取可用模型列表
     * <p>
     * 返回百炼平台支持的所有模型，按分组组织（通义千问/Qwen2.5/Qwen3/DeepSeek等）
     * </p>
     *
     * @return 模型列表，每个元素包含id、name、description、group
     * @summary 获取可用模型列表
     * @权限要求 需要登录
     */
    @Operation(summary = "获取可用模型列表")
    @GetMapping("/models")
    public R<List<Map<String, String>>> getModels() {
        return R.ok(aiService.getAvailableModels());
    }

    /**
     * 同步对话接口（小程序端使用）
     * <p>
     * 一次性返回完整的AI回复，适用于不支持SSE的客户端（如微信小程序）。
     * 当模型额度不足时自动切换到备用模型，通过modelSwitched字段告知客户端。
     * </p>
     *
     * @param authentication Spring Security认证对象
     * @param dto            聊天请求DTO
     * @return ChatResponseDTO 包含AI回复、会话ID和可能的模型切换信息
     * @summary AI同步对话（小程序端）
     * @权限要求 需要登录（所有角色均可使用）
     */
    @Operation(summary = "AI同步对话（小程序端）")
    @PostMapping("/chat-sync")
    public R<ChatResponseDTO> chatSync(Authentication authentication, @Valid @RequestBody ChatRequestDTO dto) {
        // 获取当前登录用户ID
        Long userId = (Long) authentication.getPrincipal();
        ChatResponseDTO response = aiService.chatSync(userId, dto);
        return R.ok(response);
    }

    /**
     * 获取会话列表
     * <p>
     * 查询当前用户的所有对话会话，按最新消息时间倒序排列。
     * 每个会话包含：conversationId、latestTime、messageCount、lastMessage
     * </p>
     *
     * @param authentication Spring Security认证对象
     * @return 会话列表
     * @summary 获取会话列表
     * @权限要求 需要登录
     */
    @Operation(summary = "获取会话列表")
    @GetMapping("/conversations")
    public R<List<Map<String, Object>>> getConversations(Authentication authentication) {
        // 获取当前登录用户ID
        Long userId = (Long) authentication.getPrincipal();
        return R.ok(aiService.getConversations(userId));
    }

    /**
     * 获取会话聊天历史
     * <p>
     * 查询指定会话的所有聊天记录，按时间正序排列，用于前端恢复对话上下文
     * </p>
     *
     * @param authentication Spring Security认证对象
     * @param conversationId 会话ID
     * @return 聊天记录列表
     * @summary 获取会话历史
     * @权限要求 需要登录（只能查看自己的会话）
     */
    @Operation(summary = "获取会话历史")
    @GetMapping("/conversations/{conversationId}")
    public R<List<ChatHistory>> getConversationHistory(
            Authentication authentication,
            @PathVariable String conversationId) {
        // 获取当前登录用户ID
        Long userId = (Long) authentication.getPrincipal();
        return R.ok(aiService.getConversationHistory(userId, conversationId));
    }

    /**
     * 删除会话
     * <p>
     * 逻辑删除指定会话的所有聊天记录
     * </p>
     *
     * @param authentication Spring Security认证对象
     * @param conversationId 会话ID
     * @return 操作结果
     * @summary 删除会话
     * @权限要求 需要登录（只能删除自己的会话）
     */
    @Operation(summary = "删除会话")
    @DeleteMapping("/conversations/{conversationId}")
    public R<Void> deleteConversation(
            Authentication authentication,
            @PathVariable String conversationId) {
        // 获取当前登录用户ID
        Long userId = (Long) authentication.getPrincipal();
        aiService.deleteConversation(userId, conversationId);
        return R.ok();
    }
}
