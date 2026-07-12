package com.fontal.cookagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fontal.cookagent.dto.SessionSummary;
import com.fontal.cookagent.entity.ChatHistory;
import com.fontal.cookagent.mapper.ChatHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 查询历史服务 — 记录用户每次提问与 AI 回复，并提供会话列表查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryService {

    private final ChatHistoryMapper chatHistoryMapper;

    /** 记录一次对话历史 */
    public void record(Long userId, String conversationId, String query, String reply, String channel) {
        ChatHistory history = new ChatHistory();
        history.setUserId(userId);
        history.setConversationId(conversationId);
        history.setQuery(query);
        history.setReply(reply);
        history.setChannel(channel == null ? "CHAT" : channel);
        chatHistoryMapper.insert(history);
    }

    /** 列出用户查询历史（倒序，最多 50 条） */
    public List<ChatHistory> listHistory(Long userId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return chatHistoryMapper.selectList(
                new LambdaQueryWrapper<ChatHistory>()
                        .eq(ChatHistory::getUserId, userId)
                        .orderByDesc(ChatHistory::getCreatedAt)
                        .orderByDesc(ChatHistory::getId)
                        .last("LIMIT " + safeLimit));
    }

    /** 按对话ID查询完整记录 */
    public List<ChatHistory> listByConversation(Long userId, String conversationId) {
        return chatHistoryMapper.selectList(
                new LambdaQueryWrapper<ChatHistory>()
                        .eq(ChatHistory::getUserId, userId)
                        .eq(ChatHistory::getConversationId, conversationId)
                        .orderByAsc(ChatHistory::getCreatedAt));
    }

    /** 删除某个会话历史 */
    public void deleteByConversation(Long userId, String conversationId) {
        chatHistoryMapper.delete(
                new LambdaQueryWrapper<ChatHistory>()
                        .eq(ChatHistory::getUserId, userId)
                        .eq(ChatHistory::getConversationId, conversationId));
    }

    /**
     * 列出用户的所有会话摘要（按最后活动时间倒序）。
     * 服务端按 conversation_id 分组，避免客户端分组。
     */
    public List<SessionSummary> listSessions(Long userId) {
        return chatHistoryMapper.selectSessionList(userId);
    }

    /**
     * 判断会话是否已存在（用于决定是否生成标题）。
     */
    public boolean conversationExists(Long userId, String conversationId) {
        Long count = chatHistoryMapper.selectCount(
                new LambdaQueryWrapper<ChatHistory>()
                        .eq(ChatHistory::getUserId, userId)
                        .eq(ChatHistory::getConversationId, conversationId));
        return count != null && count > 1; // > 1 因为当前记录已插入
    }
}
