package com.fontal.cookagent.service;

import com.fontal.cookagent.app.agent.MessageJsonCodec;
import com.fontal.cookagent.config.AgentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 上下文压缩器。
 * <p>
 * 两种压缩时机：
 * <ol>
 *   <li>自动压缩：执行中估算 token 超过 {@link AgentProperties#getContextMaxTokens()} 时，
 *       压缩最早已完成的轮次（保留最近 keepRecentTurns 轮完整上下文）。</li>
 *   <li>关闭会话压缩：用户关闭会话时，将所有已完成轮次的 think/act/工具调用
 *       合并为一条摘要 AssistantMessage，保留用户消息和最终摘要消息。</li>
 * </ol>
 * <p>
 * 压缩后保留"用户对话气泡 + Agent 摘要气泡"，中间的工具执行细节被总结替代。
 */
@Slf4j
@Service
public class AgentContextCompressor {

    /** 摘要系统提示词 */
    private static final String COMPRESS_SYSTEM = """
            你是对话上下文压缩助手。请将给定的 Agent 执行过程压缩为简洁的摘要消息。
            要求：
            - 保留关键信息：用户问题意图、工具调用的核心结论、最终回答要点
            - 丢弃冗余的思考过程、工具调用参数细节、重复信息
            - 用中文，简洁清晰，300 字以内
            - 输出格式为第三人称叙述，例如："用户询问红烧肉做法，通过检索菜谱获取到以下信息：..."
            """;

    private static final String COMPRESS_USER = """
            请压缩以下 Agent 执行过程：

            {trace}

            请输出压缩后的摘要（300字以内）：""";

    private final ChatClient compressClient;
    private final AgentProperties properties;

    public AgentContextCompressor(OpenAiChatModel chatModel, AgentProperties properties) {
        this.compressClient = ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder().temperature(0.0).build())
                .build();
        this.properties = properties;
    }

    // ==================== Token 估算 ====================

    /**
     * 估算消息列表的 token 数。
     * 中文 1 字 ≈ 1.5 token，英文 4 字符 ≈ 1 token。
     */
    public int estimateTokens(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (Message msg : messages) {
            total += estimateTokens(msg.getText());
            // 工具调用参数也算
            if (msg instanceof AssistantMessage am && am.getToolCalls() != null) {
                for (AssistantMessage.ToolCall tc : am.getToolCalls()) {
                    total += estimateTokens(tc.arguments());
                }
            } else if (msg instanceof ToolResponseMessage trm && trm.getResponses() != null) {
                for (ToolResponseMessage.ToolResponse tr : trm.getResponses()) {
                    total += estimateTokens(String.valueOf(tr.responseData()));
                }
            }
        }
        return total;
    }

    /** 估算单段文本的 token 数。 */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int cjk = 0;
        int other = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isCjk(c)) {
                cjk++;
            } else {
                other++;
            }
        }
        // CJK: 1.5 token/字；其他: 4 字符/token
        return (int) Math.ceil(cjk * 1.5 + other / 4.0);
    }

    private boolean isCjk(char c) {
        return (c >= 0x4E00 && c <= 0x9FFF)   // CJK 统一汉字
                || (c >= 0x3400 && c <= 0x4DBF) // CJK 扩展 A
                || (c >= 0x3000 && c <= 0x303F) // CJK 符号和标点
                || (c >= 0xFF00 && c <= 0xFFEF); // 全角字符
    }

    // ==================== 自动压缩 ====================

    /**
     * 如果 token 超阈值，压缩最早的已完成轮次，保留最近 keepRecentTurns 轮。
     *
     * @return 压缩后的消息列表（如果未超阈值则原样返回）
     */
    public List<Message> compressIfNeeded(List<Message> messages) {
        int tokens = estimateTokens(messages);
        if (tokens <= properties.getContextMaxTokens()) {
            return messages;
        }
        log.info("上下文 token 估算 {} 超过阈值 {}，触发自动压缩", tokens, properties.getContextMaxTokens());
        return compressOldTurns(messages, properties.getKeepRecentTurns());
    }

    // ==================== 关闭会话压缩 ====================

    /**
     * 关闭会话时压缩：将所有已完成轮次压缩为 [用户消息 + 摘要消息] 序列。
     * 保留最近一轮的完整上下文（若仍需要）或全部压缩。
     */
    public List<Message> compressForClose(List<Message> messages) {
        return compressOldTurns(messages, 0);
    }

    // ==================== 核心压缩逻辑 ====================

    /**
     * 压缩已完成轮次，保留最近 keepRecent 轮的完整上下文。
     * <p>
     * 一"轮"= UserMessage + 后续所有非 UserMessage 消息（AssistantMessage/ToolResponse）直到下一个 UserMessage。
     * 压缩时将旧轮次的全部消息替换为一条摘要 AssistantMessage。
     *
     * @param messages    完整消息列表
     * @param keepRecent 保留最近 N 轮不压缩（0 = 全部压缩）
     * @return 压缩后的消息列表
     */
    private List<Message> compressOldTurns(List<Message> messages, int keepRecent) {
        if (messages == null || messages.size() <= 2) {
            return messages;
        }

        // 按轮次切分
        List<List<Message>> turns = splitIntoTurns(messages);
        if (turns.size() <= keepRecent) {
            return messages;
        }

        int compressCount = turns.size() - keepRecent;
        List<Message> compressed = new ArrayList<>();

        // 压缩前 compressCount 轮
        List<Message> toCompress = new ArrayList<>();
        for (int i = 0; i < compressCount; i++) {
            toCompress.addAll(turns.get(i));
        }
        if (!toCompress.isEmpty()) {
            String summary = summarizeTurns(toCompress);
            if (summary != null && !summary.isBlank()) {
                compressed.add(new AssistantMessage("【历史对话摘要】" + summary));
            }
        }

        // 保留最近 keepRecent 轮原样
        for (int i = compressCount; i < turns.size(); i++) {
            compressed.addAll(turns.get(i));
        }

        log.info("压缩完成：{} 条消息 → {} 条（压缩 {} 轮，保留 {} 轮）",
                messages.size(), compressed.size(), compressCount, keepRecent);
        return compressed;
    }

    /**
     * 将消息列表按轮次切分。每轮以 UserMessage 开始。
     */
    private List<List<Message>> splitIntoTurns(List<Message> messages) {
        List<List<Message>> turns = new ArrayList<>();
        List<Message> current = null;
        for (Message msg : messages) {
            if (msg instanceof UserMessage) {
                if (current != null) {
                    turns.add(current);
                }
                current = new ArrayList<>();
            }
            if (current == null) {
                current = new ArrayList<>();
            }
            current.add(msg);
        }
        if (current != null && !current.isEmpty()) {
            turns.add(current);
        }
        return turns;
    }

    /**
     * 用 LLM 将多轮消息总结为一段摘要文本。
     */
    private String summarizeTurns(List<Message> messages) {
        String trace = buildTrace(messages);
        if (trace.isBlank()) {
            return "";
        }
        try {
            String userText = COMPRESS_USER.replace("{trace}", trace);
            return compressClient.prompt()
                    .system(COMPRESS_SYSTEM)
                    .user(userText)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("LLM 压缩失败，使用简单截断: {}", e.getMessage());
            // 降级：简单提取用户消息和助手消息
            return buildFallbackSummary(messages);
        }
    }

    /**
     * 构建可读的执行轨迹文本（用于 LLM 压缩输入）。
     */
    private String buildTrace(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        for (Message msg : messages) {
            switch (msg.getMessageType()) {
                case USER -> sb.append("[用户] ").append(msg.getText()).append("\n");
                case ASSISTANT -> {
                    AssistantMessage am = (AssistantMessage) msg;
                    if (am.getToolCalls() != null && !am.getToolCalls().isEmpty()) {
                        sb.append("[Agent调用工具] ");
                        for (AssistantMessage.ToolCall tc : am.getToolCalls()) {
                            sb.append(tc.name()).append("(").append(tc.arguments()).append(") ");
                        }
                        sb.append("\n");
                    }
                    if (am.getText() != null && !am.getText().isBlank()) {
                        sb.append("[Agent回复] ").append(am.getText()).append("\n");
                    }
                }
                case TOOL -> {
                    if (msg instanceof ToolResponseMessage trm && trm.getResponses() != null) {
                        for (ToolResponseMessage.ToolResponse tr : trm.getResponses()) {
                            String data = String.valueOf(tr.responseData());
                            if (data.length() > 200) {
                                data = data.substring(0, 200) + "...";
                            }
                            sb.append("[工具结果] ").append(tr.name()).append(": ").append(data).append("\n");
                        }
                    }
                }
                case SYSTEM -> { /* 跳过系统消息 */ }
            }
        }
        return sb.toString();
    }

    /**
     * LLM 压缩失败时的降级摘要：提取用户问题和助手最终回复。
     */
    private String buildFallbackSummary(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        for (Message msg : messages) {
            if (msg instanceof UserMessage) {
                sb.append("用户问：").append(msg.getText()).append("；");
            } else if (msg instanceof AssistantMessage am && am.getText() != null && !am.getText().isBlank()) {
                String text = am.getText();
                if (text.length() > 100) {
                    text = text.substring(0, 100) + "...";
                }
                sb.append("答：").append(text).append("；");
            }
        }
        return sb.toString();
    }

    /** 将消息列表序列化为 JSON（供 AgentSessionService 持久化使用）。 */
    public String encodeMessages(List<Message> messages) {
        return MessageJsonCodec.toJson(messages);
    }

    /** 将 JSON 反序列化为消息列表（供 AgentSessionService 恢复使用）。 */
    public List<Message> decodeMessages(String json) {
        return MessageJsonCodec.fromJson(json);
    }
}
