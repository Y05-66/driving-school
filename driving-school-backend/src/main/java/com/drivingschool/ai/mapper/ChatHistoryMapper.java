package com.drivingschool.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.ai.entity.ChatHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * AI聊天记录Mapper接口
 * 对应实体类：{@link ChatHistory}（chat_history表）
 * 提供聊天记录的数据库CRUD操作
 */
@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {

    /**
     * 查询用户的会话列表（按最新消息时间倒序）
     * 每个会话返回：conversationId, latestTime, messageCount, lastMessage
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    @Select("SELECT h.conversation_id AS conversationId, " +
            "h.create_time AS latestTime, " +
            "(SELECT COUNT(*) FROM chat_history WHERE conversation_id = h.conversation_id AND user_id = h.user_id AND deleted = 0) AS messageCount, " +
            "h.content AS lastMessage " +
            "FROM chat_history h " +
            "INNER JOIN (SELECT conversation_id, MAX(create_time) AS max_time FROM chat_history WHERE user_id = #{userId} AND deleted = 0 GROUP BY conversation_id) latest " +
            "ON h.conversation_id = latest.conversation_id AND h.create_time = latest.max_time " +
            "WHERE h.user_id = #{userId} AND h.deleted = 0 " +
            "ORDER BY latestTime DESC")
    List<Map<String, Object>> selectConversations(@Param("userId") Long userId);
}
