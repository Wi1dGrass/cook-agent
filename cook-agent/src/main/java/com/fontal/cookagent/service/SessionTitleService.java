package com.fontal.cookagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fontal.cookagent.config.AgentProperties;
import com.fontal.cookagent.entity.ChatHistory;
import com.fontal.cookagent.mapper.ChatHistoryMapper;
import com.fontal.cookagent.mapper.AgentSessionMapper;
import com.fontal.cookagent.entity.AgentSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 会话标题生成服务 — 用 LLM 从首条用户消息生成 ≤10 字标题。
 * <p>
 * 异步生成，不阻塞主响应流程。生成后更新 chat_history 首条记录（CHAT）
 * 或 agent_session 记录（AGENT）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionTitleService {

    private static final String TITLE_SYSTEM = """
            你是会话标题生成器。根据用户的首条消息，生成一个简洁的中文会话标题。
            要求：
            - 不超过 10 个字
            - 概括用户的核心问题或意图
            - 不要加引号、标点符号（书名号除外）
            - 直接输出标题文本，不要有任何解释
            """;

    private final ChatHistoryMapper chatHistoryMapper;
    private final AgentSessionMapper agentSessionMapper;
    private final AgentProperties properties;
    private final OpenAiChatModel chatModel;

    /**
     * 异步为 CHAT 会话生成标题。
     *
     * @param conversationId 会话 ID
     * @param firstMessage    首条用户消息
     */
    @Async
    public void generateForChat(String conversationId, String firstMessage) {
        String title = generateTitle(firstMessage);
        if (title == null || title.isBlank()) {
            return;
        }
        // 更新首条 chat_history 记录的 title
        LambdaUpdateWrapper<ChatHistory> uw = new LambdaUpdateWrapper<ChatHistory>()
                .eq(ChatHistory::getConversationId, conversationId)
                .set(ChatHistory::getTitle, title);
        chatHistoryMapper.update(null, uw);
        log.info("CHAT 会话 {} 标题已生成: {}", conversationId, title);
    }

    /**
     * 异步为 AGENT 会话生成标题。
     */
    @Async
    public void generateForAgent(String conversationId, String firstMessage) {
        String title = generateTitle(firstMessage);
        if (title == null || title.isBlank()) {
            return;
        }
        AgentSession session = agentSessionMapper.selectOne(
                new LambdaQueryWrapper<AgentSession>()
                        .eq(AgentSession::getConversationId, conversationId));
        if (session != null) {
            session.setTitle(title);
            agentSessionMapper.updateById(session);
            log.info("AGENT 会话 {} 标题已生成: {}", conversationId, title);
        }
    }

    private String generateTitle(String firstMessage) {
        if (firstMessage == null || firstMessage.isBlank()) {
            return null;
        }
        try {
            String title = ChatClient.builder(chatModel)
                    .defaultOptions(OpenAiChatOptions.builder().temperature(0.0).build())
                    .build()
                    .prompt()
                    .system(TITLE_SYSTEM)
                    .user(firstMessage)
                    .call()
                    .content();
            if (title == null) {
                return null;
            }
            // 清理：去引号、去标点、截断
            title = title.trim()
                    .replaceAll("[\"'\u201C\u201D\u2018\u2019\u300C\u300D]", "")
                    .replaceAll("[\u3002.!\uFF01\uFF1F?]+$", "")
                    .trim();
            int max = properties.getTitleMaxLength();
            if (title.length() > max) {
                title = title.substring(0, max);
            }
            return title;
        } catch (Exception e) {
            log.warn("标题生成失败: {}", e.getMessage());
            return null;
        }
    }
}
