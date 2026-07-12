package com.fontal.cookagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fontal.cookagent.app.agent.MessageJsonCodec;
import com.fontal.cookagent.entity.AgentSession;
import com.fontal.cookagent.mapper.AgentSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent 会话服务 — 管理 Agent 多轮对话的会话生命周期与上下文持久化。
 * <p>
 * 职责：
 * <ul>
 *   <li>创建新会话（生成 conversationId，初始化 messageList）</li>
 *   <li>加载已有会话（恢复 messageList 实现 Agent 多轮对话）</li>
 *   <li>保存会话状态（每次 Agent 执行后持久化 messageList）</li>
 *   <li>关闭会话（触发上下文压缩）</li>
 *   <li>列出用户会话（供前端侧边栏 / 历史页展示）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentSessionService {

    private final AgentSessionMapper agentSessionMapper;
    private final AgentContextCompressor compressor;

    // ==================== 会话创建 / 加载 ====================

    /**
     * 创建新会话。返回新会话的 conversationId（messageList 为空）。
     */
    public AgentSession createSession(Long userId) {
        AgentSession session = new AgentSession();
        session.setUserId(userId);
        session.setConversationId(UUID.randomUUID().toString());
        session.setStatus("ACTIVE");
        session.setMessageList("[]");
        session.setCurrentStep(0);
        session.setCompressed(0);
        agentSessionMapper.insert(session);
        return session;
    }

    /**
     * 按 conversationId 加载会话。返回的 messageList 已反序列化就绪。
     */
    public AgentSession loadSession(Long userId, String conversationId) {
        AgentSession session = agentSessionMapper.selectOne(
                new LambdaQueryWrapper<AgentSession>()
                        .eq(AgentSession::getConversationId, conversationId));
        if (session == null) {
            return null;
        }
        if (userId != null && !userId.equals(session.getUserId())) {
            return null;
        }
        return session;
    }

    /**
     * 获取会话的完整消息列表（从 messageList JSON 反序列化）。
     */
    public List<Message> loadMessages(AgentSession session) {
        if (session == null || session.getMessageList() == null || session.getMessageList().isBlank()) {
            return new ArrayList<>();
        }
        return MessageJsonCodec.fromJson(session.getMessageList());
    }

    /**
     * 保存会话消息列表 + 当前步骤。
     *
     * @param autoCompress 是否在保存前检查并自动压缩（执行中调用）
     */
    public void saveMessages(AgentSession session, List<Message> messages, int currentStep, boolean autoCompress) {
        if (autoCompress) {
            messages = compressor.compressIfNeeded(messages);
        }
        session.setMessageList(MessageJsonCodec.toJson(messages));
        session.setCurrentStep(currentStep);
        session.setUpdatedAt(LocalDateTime.now());
        agentSessionMapper.updateById(session);
    }

    // ==================== 会话关闭 / 压缩 ====================

    /**
     * 关闭会话并压缩上下文。
     * <p>
     * 将所有已完成轮次的 think/act/工具调用压缩为摘要，保留用户消息和摘要消息。
     */
    public AgentSession closeSession(Long userId, String conversationId) {
        AgentSession session = loadSession(userId, conversationId);
        if (session == null) {
            return null;
        }
        List<Message> messages = loadMessages(session);
        List<Message> compressed = compressor.compressForClose(messages);
        session.setMessageList(MessageJsonCodec.toJson(compressed));
        session.setStatus("CLOSED");
        session.setCompressed(1);
        session.setUpdatedAt(LocalDateTime.now());
        agentSessionMapper.updateById(session);
        log.info("会话 {} 已关闭并压缩：{} 条消息 → {} 条", conversationId, messages.size(), compressed.size());
        return session;
    }

    // ==================== 会话列表 ====================

    /**
     * 列出用户的所有 Agent 会话（用于侧边栏 / 历史页）。
     * 按更新时间倒序。
     */
    public List<AgentSession> listSessions(Long userId) {
        return agentSessionMapper.selectList(
                new LambdaQueryWrapper<AgentSession>()
                        .eq(AgentSession::getUserId, userId)
                        .orderByDesc(AgentSession::getUpdatedAt));
    }

    // ==================== 标题 ====================

    /**
     * 更新会话标题。
     */
    public void updateTitle(String conversationId, String title) {
        AgentSession session = agentSessionMapper.selectOne(
                new LambdaQueryWrapper<AgentSession>()
                        .eq(AgentSession::getConversationId, conversationId));
        if (session != null) {
            session.setTitle(title);
            session.setUpdatedAt(LocalDateTime.now());
            agentSessionMapper.updateById(session);
        }
    }

    // ==================== 删除 ====================

    /**
     * 删除会话。
     */
    public boolean deleteSession(Long userId, String conversationId) {
        AgentSession session = loadSession(userId, conversationId);
        if (session == null) {
            return false;
        }
        agentSessionMapper.deleteById(session.getId());
        return true;
    }
}
