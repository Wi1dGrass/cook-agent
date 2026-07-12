package com.fontal.cookagent.app.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fontal.cookagent.entity.ChatMemory;
import com.fontal.cookagent.mapper.ChatMemoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 基于 MySQL 的 ChatMemoryRepository — 替代 FileBasedChatMemory（Kryo 文件）。
 * <p>
 * 配合 MessageWindowChatMemory 使用，滑动窗口裁剪由 MessageWindowChatMemory 负责，
 * 本类只负责将 Message 列表持久化到 chat_memory 表 / 从中恢复。
 * <p>
 * 与 Spring AI 官方 JdbcChatMemoryRepository 不同，本实现完整保留工具调用消息
 * （AssistantMessage.toolCalls 和 ToolResponseMessage），不进行过滤。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MysqlChatMemoryRepository implements ChatMemoryRepository {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ChatMemoryMapper chatMemoryMapper;

    @Override
    public List<String> findConversationIds() {
        List<ChatMemory> rows = chatMemoryMapper.selectList(new LambdaQueryWrapper<ChatMemory>());
        Set<String> ids = new LinkedHashSet<>();
        for (ChatMemory row : rows) {
            ids.add(row.getConversationId());
        }
        return new ArrayList<>(ids);
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        List<ChatMemory> rows = chatMemoryMapper.selectList(
                new LambdaQueryWrapper<ChatMemory>()
                        .eq(ChatMemory::getConversationId, conversationId)
                        .orderByAsc(ChatMemory::getSeqId));
        List<Message> messages = new ArrayList<>(rows.size());
        for (ChatMemory row : rows) {
            Message msg = toMessage(row);
            if (msg != null) {
                messages.add(msg);
            }
        }
        return messages;
    }
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        // 全量覆盖：先删后插
        deleteByConversationId(conversationId);
        if (messages == null || messages.isEmpty()) {
            return;
        }
        for (int i = 0; i < messages.size(); i++) {
            ChatMemory row = toRow(conversationId, i, messages.get(i));
            if (row != null) {
                chatMemoryMapper.insert(row);
            }
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        chatMemoryMapper.delete(new LambdaQueryWrapper<ChatMemory>()
                .eq(ChatMemory::getConversationId, conversationId));
    }

    // ==================== Message <-> Row 转换 ====================

    private Message toMessage(ChatMemory row) {
        MessageType type = MessageType.valueOf(row.getMessageType());
        return switch (type) {
            case USER -> new UserMessage(row.getContent() == null ? "" : row.getContent());
            case SYSTEM -> new SystemMessage(row.getContent() == null ? "" : row.getContent());
            case ASSISTANT -> {
                String content = row.getContent() == null ? "" : row.getContent();
                if (row.getToolCallsJson() != null) {
                    try {
                        List<?> list = JSON.readValue(row.getToolCallsJson(), List.class);
                        List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();
                        for (Object o : list) {
                            if (o instanceof java.util.Map<?, ?> m) {
                                String id = (String) m.get("id");
                                String type2 = m.get("type") == null ? "function" : String.valueOf(m.get("type"));
                                String name = (String) m.get("name");
                                String arguments = (String) m.get("arguments");
                                toolCalls.add(new AssistantMessage.ToolCall(id, type2, name, arguments));
                            }
                        }
                        if (!toolCalls.isEmpty()) {
                            yield new AssistantMessage(content, java.util.Map.of(), toolCalls);
                        }
                    } catch (Exception e) {
                        log.warn("反序列化 toolCalls 失败: {}", e.getMessage());
                    }
                }
                yield new AssistantMessage(content);
            }
            case TOOL -> {
                if (row.getToolResponsesJson() == null) {
                    yield null;
                }
                try {
                    List<?> list = JSON.readValue(row.getToolResponsesJson(), List.class);
                    List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
                    for (Object o : list) {
                        if (o instanceof java.util.Map<?, ?> m) {
                            String id = (String) m.get("id");
                            String name = (String) m.get("name");
                            String data = m.get("responseData") == null ? "" : String.valueOf(m.get("responseData"));
                            responses.add(new ToolResponseMessage.ToolResponse(id, name, data));
                        }
                    }
                    yield new ToolResponseMessage(responses);
                } catch (Exception e) {
                    log.warn("反序列化 toolResponses 失败: {}", e.getMessage());
                    yield null;
                }
            }
        };
    }

    private ChatMemory toRow(String conversationId, int seqId, Message message) {
        ChatMemory row = new ChatMemory();
        row.setConversationId(conversationId);
        row.setSeqId(seqId);
        row.setMessageType(message.getMessageType().name());
        row.setContent(message.getText());

        // 保留工具调用信息
        if (message instanceof AssistantMessage am && am.getToolCalls() != null && !am.getToolCalls().isEmpty()) {
            try {
                List<Object> calls = new ArrayList<>();
                for (AssistantMessage.ToolCall tc : am.getToolCalls()) {
                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", tc.id());
                    m.put("type", tc.type());
                    m.put("name", tc.name());
                    m.put("arguments", tc.arguments());
                    calls.add(m);
                }
                row.setToolCallsJson(JSON.writeValueAsString(calls));
            } catch (Exception e) {
                log.warn("序列化 toolCalls 失败: {}", e.getMessage());
            }
        } else if (message instanceof ToolResponseMessage trm && trm.getResponses() != null && !trm.getResponses().isEmpty()) {
            try {
                List<Object> resps = new ArrayList<>();
                for (ToolResponseMessage.ToolResponse tr : trm.getResponses()) {
                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("name", tr.name());
                    m.put("responseData", tr.responseData());
                    resps.add(m);
                }
                row.setToolResponsesJson(JSON.writeValueAsString(resps));
            } catch (Exception e) {
                log.warn("序列化 toolResponses 失败: {}", e.getMessage());
            }
        }
        return row;
    }
}
