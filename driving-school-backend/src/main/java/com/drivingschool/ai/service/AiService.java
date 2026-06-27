package com.drivingschool.ai.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drivingschool.ai.dto.ChatRequestDTO;
import com.drivingschool.ai.dto.ChatResponseDTO;
import com.drivingschool.ai.entity.ChatHistory;
import com.drivingschool.ai.mapper.ChatHistoryMapper;
import com.drivingschool.common.exception.BusinessException;
import io.reactivex.disposables.Disposable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * AI助手服务类
 * <p>
 * 基于阿里云百炼平台（DashScope SDK）提供AI知识问答功能，支持以下特性：
 * 1. 流式输出（SSE）- 逐字推送AI回复，前端实现打字机效果
 * 2. 同步调用 - 一次性返回完整回复，供小程序等不支持SSE的客户端使用
 * 3. 模型自动降级 - 当前模型额度不足时，自动切换到备用模型继续服务
 * 4. 多轮对话 - 通过conversationId关联历史消息，保持上下文连贯
 * 5. 会话管理 - 支持查看会话列表、加载历史记录、删除会话
 * </p>
 * <p>
 * 模型降级顺序：qwen-plus → qwen-turbo → qwen-max → qwen-long → qwen2.5-72b → qwen3-32b → deepseek-v3 → deepseek-r1
 * </p>
 *
 * @author drivingschool
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    /** 聊天记录数据访问层 */
    private final ChatHistoryMapper chatHistoryMapper;

    /** 百炼平台API密钥，通过环境变量 DASHSCOPE_API_KEY 配置 */
    @Value("${ai.dashscope.api-key:}")
    private String apiKey;

    /** 默认模型名称，可通过请求参数覆盖 */
    @Value("${ai.dashscope.model:qwen-plus}")
    private String model;

    /** 系统提示词，定义AI助手的角色和行为 */
    @Value("${ai.dashscope.system-prompt:你是驾校智能助手，专门解答驾校相关问题。}")
    private String systemPrompt;

    /** 模型降级顺序：额度不足时按此顺序依次尝试 */
    private static final List<String> FALLBACK_MODELS = List.of(
            "qwen-plus", "qwen-turbo", "qwen-max", "qwen-long",
            "qwen2.5-72b-instruct", "qwen2.5-32b-instruct",
            "qwen3-32b", "qwen3-235b-a22b",
            "deepseek-v3", "deepseek-r1"
    );

    /** 线程池用于异步处理流式响应，避免阻塞请求线程 */
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * 应用关闭时清理线程池
     * <p>
     * 等待正在执行的任务完成，超时则强制终止，防止JVM无法正常退出
     * </p>
     */
    @PreDestroy
    public void destroy() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 流式对话（SSE），带模型自动降级
     * <p>
     * 处理流程：
     * 1. 生成或复用会话ID
     * 2. 保存用户消息到数据库
     * 3. 构建消息列表（系统提示 + 历史消息）
     * 4. 按降级顺序依次尝试模型
     * 5. 成功则逐字推送SSE事件，失败则切换下一个模型
     * 6. 所有模型均失败则返回错误事件
     * </p>
     * <p>
     * SSE事件类型：
     * - message: AI回复内容片段（可多次推送）
     * - done: 对话完成，data为conversationId
     * - model-switch: 模型切换通知，data为新模型名称
     * - error: 错误信息
     * </p>
     *
     * @param userId 当前登录用户ID
     * @param dto    聊天请求DTO（包含消息内容、会话ID、模型选择）
     * @return SseEmitter 用于流式推送AI回复的发射器
     * @summary AI流式对话
     * @权限要求 需要登录（所有角色均可使用）
     */
    public SseEmitter streamChat(Long userId, ChatRequestDTO dto) {
        checkApiKey();

        // 创建SSE发射器，超时时间60秒
        SseEmitter emitter = new SseEmitter(60000L);

        // 生成或使用已有的会话ID
        String conversationId = dto.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString().replace("-", "");
        }

        // 保存用户消息到数据库
        saveMessage(userId, conversationId, "user", dto.getMessage());

        // 构建消息列表和模型尝试顺序
        String finalConversationId = conversationId;
        List<Message> messages = buildMessages(userId, finalConversationId);
        List<String> modelOrder = buildModelOrder(resolveModel(dto.getModel()));

        // 异步处理流式响应
        executorService.execute(() -> {
            Exception lastError = null;
            Disposable currentDisposable = null; // 当前活跃的订阅，用于资源清理

            // 按降级顺序依次尝试模型
            for (int i = 0; i < modelOrder.size(); i++) {
                String tryModel = modelOrder.get(i);
                log.info("尝试使用模型: {}", tryModel);

                // 如果不是第一个模型，发送模型切换和重置通知（清除客户端之前的残余内容）
                if (i > 0) {
                    safeSend(emitter, "model-switch", tryModel);
                    safeSend(emitter, "reset", tryModel);
                }

                try {
                    // 构建DashScope请求参数
                    GenerationParam param = GenerationParam.builder()
                            .apiKey(apiKey)
                            .model(tryModel)
                            .messages(messages)
                            .incrementalOutput(true)
                            .build();

                    // 调用DashScope流式API
                    Generation gen = new Generation();
                    var flowable = gen.streamCall(param);

                    StringBuilder fullResponse = new StringBuilder();
                    boolean[] done = {false};   // 标记当前模型是否完成
                    boolean[] isQuota = {false}; // 标记是否为额度不足错误

                    // 订阅流式响应
                    currentDisposable = flowable.subscribe(
                            chunk -> {
                                // 接收并推送每个内容片段
                                try {
                                    String content = extractContent(chunk);
                                    if (content != null) {
                                        fullResponse.append(content);
                                        emitter.send(SseEmitter.event()
                                                .name("message")
                                                .data(content));
                                    }
                                } catch (IOException e) {
                                    log.warn("发送SSE事件失败: {}", e.getMessage());
                                }
                            },
                            error -> {
                                // 处理错误：额度不足则标记，其他错误则直接报错
                                log.warn("模型 {} 流式调用失败: {}", tryModel, error.getMessage());
                                if (isQuotaError(error)) {
                                    isQuota[0] = true;
                                } else {
                                    safeSend(emitter, "error", "AI服务调用失败，请稍后重试");
                                    safeComplete(emitter, error);
                                }
                                synchronized (done) {
                                    done[0] = true;
                                    done.notify();
                                }
                            },
                            () -> {
                                // 流式完成：保存AI回复，发送完成事件
                                try {
                                    saveMessage(userId, finalConversationId, "assistant", fullResponse.toString());
                                    emitter.send(SseEmitter.event()
                                            .name("done")
                                            .data(finalConversationId));
                                    emitter.complete();
                                } catch (IOException e) {
                                    log.warn("发送完成事件失败: {}", e.getMessage());
                                    safeComplete(emitter, null);
                                } finally {
                                    synchronized (done) {
                                        done[0] = true;
                                        done.notify();
                                    }
                                }
                            }
                    );

                    // 客户端断开时取消当前订阅
                    final Disposable d = currentDisposable;
                    emitter.onCompletion(() -> { if (d != null && !d.isDisposed()) d.dispose(); });
                    emitter.onError(e -> { if (d != null && !d.isDisposed()) d.dispose(); });

                    // 阻塞等待当前模型完成（成功或额度不足）
                    synchronized (done) {
                        while (!done[0]) {
                            try {
                                done.wait(55000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }

                    // 如果是额度不足，清理订阅后继续尝试下一个模型
                    if (isQuota[0]) {
                        log.warn("模型 {} 额度不足，尝试下一个模型", tryModel);
                        if (currentDisposable != null && !currentDisposable.isDisposed()) {
                            currentDisposable.dispose();
                        }
                        lastError = new RuntimeException("模型额度不足: " + tryModel);
                        continue;
                    }

                    // 其他情况（成功或非额度错误），退出循环
                    return;

                } catch (Exception e) {
                    // 同步异常（模型启动失败等），清理订阅
                    if (currentDisposable != null && !currentDisposable.isDisposed()) {
                        currentDisposable.dispose();
                    }
                    log.warn("模型 {} 启动失败: {}", tryModel, e.getMessage());
                    lastError = e;
                    if (!isQuotaError(e)) {
                        break; // 非额度错误，不继续尝试
                    }
                    // 额度错误，继续尝试下一个模型
                }
            }

            // 所有模型都失败，发送错误事件
            log.error("所有模型均不可用");
            String errMsg = (lastError != null && isQuotaError(lastError))
                    ? "所有模型额度已用完，请稍后重试"
                    : "AI服务暂时不可用，请稍后重试";
            safeSend(emitter, "error", errMsg);
            safeComplete(emitter, lastError != null ? lastError : new RuntimeException("所有模型不可用"));
        });

        // 超时回调
        emitter.onTimeout(() -> {
            log.warn("SSE连接超时");
            safeComplete(emitter, null);
        });

        return emitter;
    }

    /**
     * 同步对话（小程序端使用），带模型自动降级
     * <p>
     * 一次性返回完整的AI回复，适用于不支持SSE的客户端（如微信小程序）。
     * 当模型额度不足时自动切换到备用模型，如果发生降级则在响应中通过modelSwitched字段告知客户端。
     * </p>
     *
     * @param userId 当前登录用户ID
     * @param dto    聊天请求DTO
     * @return ChatResponseDTO 包含AI回复、会话ID和可能的模型切换信息
     * @summary AI同步对话
     * @权限要求 需要登录（所有角色均可使用）
     */
    public ChatResponseDTO chatSync(Long userId, ChatRequestDTO dto) {
        checkApiKey();

        // 生成或使用已有的会话ID
        String conversationId = dto.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString().replace("-", "");
        }

        // 保存用户消息
        saveMessage(userId, conversationId, "user", dto.getMessage());

        // 构建消息列表和模型尝试顺序
        List<Message> messages = buildMessages(userId, conversationId);
        List<String> modelOrder = buildModelOrder(resolveModel(dto.getModel()));
        Exception lastError = null;

        // 按降级顺序依次尝试模型
        for (String tryModel : modelOrder) {
            try {
                log.info("尝试使用模型: {}", tryModel);

                GenerationParam param = GenerationParam.builder()
                        .apiKey(apiKey)
                        .model(tryModel)
                        .messages(messages)
                        .build();

                // 同步调用DashScope API
                Generation gen = new Generation();
                GenerationResult result = gen.call(param);
                String reply = extractContent(result);

                // 保存AI回复
                saveMessage(userId, conversationId, "assistant", reply);

                // 构建响应
                ChatResponseDTO response = new ChatResponseDTO();
                response.setReply(reply);
                response.setConversationId(conversationId);
                // 如果发生了模型降级，告知客户端实际使用的模型
                if (!tryModel.equals(modelOrder.get(0))) {
                    response.setModelSwitched(tryModel);
                }
                return response;

            } catch (Exception e) {
                log.error("模型 {} 调用失败: {}", tryModel, e.getMessage(), e);
                lastError = e;
                if (!isQuotaError(e)) {
                    break; // 非额度错误，不继续尝试
                }
                // 额度错误，继续尝试下一个模型
            }
        }

        // 所有模型都失败
        String errorMsg = lastError != null ? lastError.getMessage() : "未知错误";
        log.error("所有模型均不可用，最后错误: {}", errorMsg);
        if (lastError != null && isQuotaError(lastError)) {
            throw new BusinessException("所有模型额度已用完，请稍后重试");
        }
        throw new BusinessException("AI服务调用失败: " + errorMsg);
    }

    /**
     * 获取用户的会话列表
     * <p>
     * 查询当前用户的所有对话会话，按最新消息时间倒序排列。
     * 每个会话包含：conversationId、latestTime、messageCount、lastMessage
     * </p>
     *
     * @param userId 用户ID
     * @return 会话列表，每个元素包含会话ID、最新时间、消息数、最后一条消息
     * @summary 获取会话列表
     * @权限要求 需要登录
     */
    public List<Map<String, Object>> getConversations(Long userId) {
        return chatHistoryMapper.selectConversations(userId);
    }

    /**
     * 获取指定会话的聊天历史
     * <p>
     * 查询指定会话下的所有聊天记录，按时间正序排列，用于前端恢复对话上下文
     * </p>
     *
     * @param userId         用户ID（用于权限验证，确保只能查看自己的会话）
     * @param conversationId 会话ID
     * @return 聊天记录列表
     * @summary 获取会话历史
     * @权限要求 需要登录（只能查看自己的会话）
     */
    public List<ChatHistory> getConversationHistory(Long userId, String conversationId) {
        LambdaQueryWrapper<ChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatHistory::getUserId, userId)
                .eq(ChatHistory::getConversationId, conversationId)
                .orderByAsc(ChatHistory::getCreateTime);
        return chatHistoryMapper.selectList(wrapper);
    }

    /**
     * 删除会话（逻辑删除该会话下的所有消息）
     * <p>
     * 使用MyBatis-Plus的逻辑删除机制，将deleted字段设为1
     * </p>
     *
     * @param userId         用户ID（用于权限验证）
     * @param conversationId 会话ID
     * @summary 删除会话
     * @权限要求 需要登录（只能删除自己的会话）
     */
    public void deleteConversation(Long userId, String conversationId) {
        int rows = chatHistoryMapper.delete(
                new LambdaQueryWrapper<ChatHistory>()
                        .eq(ChatHistory::getUserId, userId)
                        .eq(ChatHistory::getConversationId, conversationId)
        );
        if (rows == 0) {
            throw new BusinessException("会话不存在或已被删除");
        }
    }

    /**
     * 获取可用模型列表
     * <p>
     * 返回百炼平台支持的所有模型，按分组组织：
     * - 通义千问：qwen-turbo/plus/max/long
     * - 视觉模型：qwen-vl-plus/vl-max
     * - Qwen2.5：7b/14b/32b/72b
     * - Qwen3：8b/14b/32b/235b
     * - 代码模型：qwen2.5-coder-32b
     * - DeepSeek：v3/r1
     * </p>
     *
     * @return 模型列表，每个元素包含id、name、description、group
     * @summary 获取可用模型列表
     * @权限要求 需要登录
     */
    public List<Map<String, String>> getAvailableModels() {
        List<Map<String, String>> models = new ArrayList<>();
        // Qwen 通义千问系列
        models.add(Map.of("id", "qwen-turbo", "name", "Qwen-Turbo", "description", "速度最快，适合简单问答", "group", "通义千问"));
        models.add(Map.of("id", "qwen-plus", "name", "Qwen-Plus", "description", "均衡模式，推荐日常使用", "group", "通义千问"));
        models.add(Map.of("id", "qwen-max", "name", "Qwen-Max", "description", "最强能力，适合复杂问题", "group", "通义千问"));
        models.add(Map.of("id", "qwen-long", "name", "Qwen-Long", "description", "长上下文，适合长文分析", "group", "通义千问"));
        // Qwen-VL 视觉理解
        models.add(Map.of("id", "qwen-vl-plus", "name", "Qwen-VL-Plus", "description", "视觉理解，图文问答", "group", "视觉模型"));
        models.add(Map.of("id", "qwen-vl-max", "name", "Qwen-VL-Max", "description", "最强视觉理解能力", "group", "视觉模型"));
        // Qwen2.5 开源系列
        models.add(Map.of("id", "qwen2.5-7b-instruct", "name", "Qwen2.5-7B", "description", "轻量级，响应快", "group", "Qwen2.5"));
        models.add(Map.of("id", "qwen2.5-14b-instruct", "name", "Qwen2.5-14B", "description", "中等规模，性价比高", "group", "Qwen2.5"));
        models.add(Map.of("id", "qwen2.5-32b-instruct", "name", "Qwen2.5-32B", "description", "大规模，能力强", "group", "Qwen2.5"));
        models.add(Map.of("id", "qwen2.5-72b-instruct", "name", "Qwen2.5-72B", "description", "最强开源模型", "group", "Qwen2.5"));
        // Qwen3 最新系列
        models.add(Map.of("id", "qwen3-8b", "name", "Qwen3-8B", "description", "最新轻量模型", "group", "Qwen3"));
        models.add(Map.of("id", "qwen3-14b", "name", "Qwen3-14B", "description", "最新中等模型", "group", "Qwen3"));
        models.add(Map.of("id", "qwen3-32b", "name", "Qwen3-32B", "description", "最新大规模模型", "group", "Qwen3"));
        models.add(Map.of("id", "qwen3-235b-a22b", "name", "Qwen3-235B", "description", "最新旗舰MoE模型", "group", "Qwen3"));
        // 代码模型
        models.add(Map.of("id", "qwen2.5-coder-32b-instruct", "name", "Qwen2.5-Coder-32B", "description", "代码生成与理解", "group", "代码模型"));
        // DeepSeek 系列（百炼平台托管）
        models.add(Map.of("id", "deepseek-v3", "name", "DeepSeek-V3", "description", "通用对话模型", "group", "DeepSeek"));
        models.add(Map.of("id", "deepseek-r1", "name", "DeepSeek-R1", "description", "推理增强模型", "group", "DeepSeek"));
        return models;
    }

    // ========== 私有方法 ==========

    /**
     * 构建消息列表（系统提示 + 历史消息）
     * <p>
     * 从数据库查询最近20条历史消息（约10轮对话），
     * 加上系统提示词组成完整的消息列表发送给AI模型
     * </p>
     *
     * @param userId         用户ID
     * @param conversationId 会话ID
     * @return 消息列表，包含system/user/assistant角色消息
     */
    private List<Message> buildMessages(Long userId, String conversationId) {
        List<Message> messages = new ArrayList<>();
        // 添加系统提示词
        messages.add(Message.builder()
                .role(Role.SYSTEM.getValue())
                .content(systemPrompt)
                .build());

        // 查询历史消息（最近20条，约10轮对话）
        LambdaQueryWrapper<ChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatHistory::getUserId, userId)
                .eq(ChatHistory::getConversationId, conversationId)
                .orderByAsc(ChatHistory::getCreateTime)
                .last("LIMIT 20");
        List<ChatHistory> history = chatHistoryMapper.selectList(wrapper);

        // 将历史消息转换为DashScope消息格式
        for (ChatHistory record : history) {
            messages.add(Message.builder()
                    .role(record.getRole().equals("user") ? Role.USER.getValue() : Role.ASSISTANT.getValue())
                    .content(record.getContent())
                    .build());
        }
        return messages;
    }

    /**
     * 保存消息到数据库
     *
     * @param userId         用户ID
     * @param conversationId 会话ID
     * @param role           消息角色（user/assistant）
     * @param content        消息内容
     */
    private void saveMessage(Long userId, String conversationId, String role, String content) {
        ChatHistory record = new ChatHistory();
        record.setUserId(userId);
        record.setConversationId(conversationId);
        record.setRole(role);
        record.setContent(content != null ? content : "");
        chatHistoryMapper.insert(record);
    }

    /**
     * 构建模型尝试顺序：用户选择的模型优先，其余按降级顺序排列
     *
     * @param preferred 用户选择的首选模型
     * @return 模型尝试顺序列表
     */
    private List<String> buildModelOrder(String preferred) {
        List<String> order = new ArrayList<>();
        order.add(preferred);
        for (String m : FALLBACK_MODELS) {
            if (!m.equals(preferred)) {
                order.add(m);
            }
        }
        return order;
    }

    /**
     * 解析模型名称：优先使用请求中的模型，否则使用配置的默认模型
     *
     * @param requestModel 请求中指定的模型名称
     * @return 最终使用的模型名称
     */
    private String resolveModel(String requestModel) {
        if (requestModel != null && !requestModel.isBlank()) {
            return requestModel;
        }
        return model;
    }

    /**
     * 安全提取流式/同步响应内容，防止NPE和IndexOutOfBoundsException
     * <p>
     * DashScope SDK返回的响应结构可能包含null值，
     * 需要逐层检查避免空指针异常
     * </p>
     *
     * @param chunk DashScope响应结果
     * @return 提取的文本内容，解析失败返回null
     */
    private String extractContent(GenerationResult chunk) {
        try {
            if (chunk == null || chunk.getOutput() == null) return null;
            // 尝试 OpenAI 兼容格式: output.choices[0].message.content
            try {
                var choices = chunk.getOutput().getChoices();
                if (choices != null && !choices.isEmpty()) {
                    var choice = choices.get(0);
                    if (choice != null && choice.getMessage() != null) {
                        String c = choice.getMessage().getContent();
                        if (c != null) return c;
                    }
                }
            } catch (Exception ignored) {}
            // 尝试 DashScope 原生格式: output.text
            try {
                String text = chunk.getOutput().getText();
                if (text != null && !text.isEmpty()) return text;
            } catch (Exception ignored) {}
            log.warn("无法从响应中提取内容，output class: {}", chunk.getOutput().getClass().getName());
            return null;
        } catch (Exception e) {
            log.warn("解析响应内容失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 安全发送SSE事件，捕获IO异常
     *
     * @param emitter   SSE发射器
     * @param eventName 事件名称（message/done/model-switch/error）
     * @param data      事件数据
     */
    private void safeSend(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            log.warn("发送SSE事件 {} 失败: {}", eventName, e.getMessage());
        }
    }

    /**
     * 安全完成SseEmitter，避免重复完成导致异常
     *
     * @param emitter SSE发射器
     * @param error   错误信息，为null则正常完成
     */
    private void safeComplete(SseEmitter emitter, Throwable error) {
        try {
            if (error != null) {
                emitter.completeWithError(error);
            } else {
                emitter.complete();
            }
        } catch (Exception e) {
            log.warn("完成SseEmitter失败: {}", e.getMessage());
        }
    }

    /**
     * 判断是否为额度不足错误
     * <p>
     * 通过检查异常消息中的关键词判断：
     * - HTTP 429 状态码
     * - insufficient_quota: API额度不足
     * - rate_limit_exceeded: 请求频率超限
     * - quota exceeded: 配额用尽
     * - throttling: 被限流
     * </p>
     *
     * @param e 异常对象
     * @return true表示额度不足，false表示其他错误
     */
    private boolean isQuotaError(Throwable e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        msg = msg.toLowerCase();
        return msg.contains("429")
                || msg.contains("insufficient_quota")
                || msg.contains("rate_limit_exceeded")
                || msg.contains("quota exceeded")
                || msg.contains("throttling");
    }

    /**
     * 检查API Key是否已配置
     *
     * @throws BusinessException 未配置或使用占位值时抛出
     */
    private void checkApiKey() {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("sk-xxx")) {
            throw new BusinessException("AI服务未配置API Key，请联系管理员");
        }
    }
}
