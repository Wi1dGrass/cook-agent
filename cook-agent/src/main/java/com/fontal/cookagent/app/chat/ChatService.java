package com.fontal.cookagent.app.chat;

import com.fontal.cookagent.rag.advisor.MyLoggerAdvisor;
import com.fontal.cookagent.rag.advisor.RagDebugAdvisor;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 普通对话服务 — 带 RAG 检索 + JDBC 持久化记忆。
 *
 * Advisor 链：Memory(defaultAdvisor) → RAG → Debug → Logger → LLM
 * <ul>
 *   <li>Memory — ChatClient defaultAdvisors 中已配置</li>
 *   <li>RAG   — 向量检索相关菜谱增强上下文，allowEmptyContext=true</li>
 *   <li>Debug — 打印检索结果</li>
 *   <li>Logger— 打印请求和响应文本</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final RetrievalAugmentationAdvisor retrievalAdvisor;
    private final RagDebugAdvisor debugAdvisor;

    /**
     * 在已有对话中发送消息。
     *
     * @param conversationId 对话 ID（同一次对话传入相同 ID 以保持上下文）
     * @param message        用户消息
     * @return AI 回答
     */
    public String chat(String conversationId, String message) {
        return chatClient.prompt()
                .system("""
                        你是一位中餐厨师AI助手。
                        - 有菜谱参考时，结合参考内容回答烹饪问题。
                        - 没有相关菜谱时，直接用常识和对话记忆回答，不要说"不知道"。
                        - 保持回答简洁专业。
                        """)
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .advisors(retrievalAdvisor, debugAdvisor, new MyLoggerAdvisor())
                .call()
                .content();
    }

    /**
     * 开始新对话并发送首条消息。
     *
     * @param message 用户首条消息
     * @return 包含新对话 ID 和 AI 回复的结果
     */
    public ChatResult startNewChat(String message) {
        String conversationId = UUID.randomUUID().toString();
        String reply = chat(conversationId, message);
        return new ChatResult(conversationId, reply);
    }

    /** 对话结果：包含对话 ID 和 AI 回复 */
    public record ChatResult(String conversationId, String reply) {
    }
}
