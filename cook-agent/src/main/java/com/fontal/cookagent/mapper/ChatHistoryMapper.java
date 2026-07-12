package com.fontal.cookagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fontal.cookagent.dto.SessionSummary;
import com.fontal.cookagent.entity.ChatHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {

    /**
     * 按用户ID列出会话摘要（按最后活动时间倒序）。
     * 从 chat_history 表按 conversation_id 分组聚合。
     */
    @Select("""
            SELECT
                conversation_id      AS conversationId,
                MIN(title)            AS title,
                MIN(query)            AS firstQuery,
                MAX(channel)          AS channel,
                COUNT(*)              AS messageCount,
                MIN(created_at)       AS createdAt,
                MAX(created_at)       AS lastAt
            FROM chat_history
            WHERE user_id = #{userId}
            GROUP BY conversation_id
            ORDER BY lastAt DESC
            """)
    List<SessionSummary> selectSessionList(@Param("userId") Long userId);
}
