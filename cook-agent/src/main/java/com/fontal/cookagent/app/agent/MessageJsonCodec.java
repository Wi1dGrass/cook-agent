package com.fontal.cookagent.app.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Message 列表 ↔ JSON 编解码器（用于 Agent 会话持久化）。
 * <p>
 * 完整保留工具调用信息（AssistantMessage.toolCalls 和 ToolResponseMessage.responses），
 * 支持从 JSON 完整恢复消息列表以实现 Agent 多轮对话。
 * <p>
 * JSON 格式：
 * <pre>[
 *   {"type":"USER","content":"红烧肉怎么做？"},
 *   {"type":"ASSISTANT","content":"...","toolCalls":[{"name":"searchRecipes","arguments":"{...}"}]},
 *   {"type":"TOOL","toolResponses":[{"name":"searchRecipes","responseData":"..."}]},
 *   {"type":"ASSISTANT","content":"根据搜索结果..."}
 * ]</pre>
 */
@Slf4j
public final class MessageJsonCodec {

    private static final ObjectMapper JSON = new ObjectMapper();

    private MessageJsonCodec() {
    }

    /** 将消息列表序列化为 JSON 字符串。 */
    public static String toJson(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return "[]";
        }
        List<Object> list = new ArrayList<>(messages.size());
        for (Message msg : messages) {
            list.add(messageToMap(msg));
        }
        try {
            return JSON.writeValueAsString(list);
        } catch (Exception e) {
            log.error("序列化消息列表失败", e);
            return "[]";
        }
    }

    /** 将 JSON 字符串反序列化为消息列表。 */
    public static List<Message> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<Map<String, Object>> list = JSON.readValue(json, new TypeReference<>() {});
            List<Message> messages = new ArrayList<>(list.size());
            for (Map<String, Object> m : list) {
                Message msg = mapToMessage(m);
                if (msg != null) {
                    messages.add(msg);
                }
            }
            return messages;
        } catch (Exception e) {
            log.error("反序列化消息列表失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ==================== 序列化 ====================

    private static Map<String, Object> messageToMap(Message msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", msg.getMessageType().name());
        m.put("content", msg.getText());
        if (msg instanceof AssistantMessage am && am.getToolCalls() != null && !am.getToolCalls().isEmpty()) {
            List<Map<String, Object>> calls = new ArrayList<>();
            for (AssistantMessage.ToolCall tc : am.getToolCalls()) {
                Map<String, Object> c = new LinkedHashMap<>();
                c.put("id", tc.id());
                c.put("type", tc.type());
                c.put("name", tc.name());
                c.put("arguments", tc.arguments());
                calls.add(c);
            }
            m.put("toolCalls", calls);
        } else if (msg instanceof ToolResponseMessage trm && trm.getResponses() != null && !trm.getResponses().isEmpty()) {
            List<Map<String, Object>> resps = new ArrayList<>();
            for (ToolResponseMessage.ToolResponse tr : trm.getResponses()) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("name", tr.name());
                r.put("responseData", tr.responseData());
                resps.add(r);
            }
            m.put("toolResponses", resps);
        }
        return m;
    }

    // ==================== 反序列化 ====================

    @SuppressWarnings("unchecked")
    private static Message mapToMessage(Map<String, Object> m) {
        String type = (String) m.get("type");
        if (type == null) {
            return null;
        }
        return switch (MessageType.valueOf(type)) {
            case USER -> new UserMessage(asString(m.get("content"), ""));
            case SYSTEM -> new SystemMessage(asString(m.get("content"), ""));
            case ASSISTANT -> {
                String content = asString(m.get("content"), "");
                Object tc = m.get("toolCalls");
                if (tc instanceof List<?> list && !list.isEmpty()) {
                    List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();
                    for (Object o : list) {
                        if (o instanceof Map<?, ?> mm) {
                            String tcType = mm.get("type") == null ? "function" : String.valueOf(mm.get("type"));
                            toolCalls.add(new AssistantMessage.ToolCall(
                                    (String) mm.get("id"),
                                    tcType,
                                    (String) mm.get("name"),
                                    (String) mm.get("arguments")));
                        }
                    }
                    yield new AssistantMessage(content, java.util.Map.of(), toolCalls);
                }
                yield new AssistantMessage(content);
            }
            case TOOL -> {
                Object tr = m.get("toolResponses");
                if (tr instanceof List<?> list && !list.isEmpty()) {
                    List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
                    for (Object o : list) {
                        if (o instanceof Map<?, ?> mm) {
                            responses.add(new ToolResponseMessage.ToolResponse(
                                    (String) mm.get("id"),
                                    (String) mm.get("name"),
                                    mm.get("responseData") == null ? "" : String.valueOf(mm.get("responseData"))));
                        }
                    }
                    yield new ToolResponseMessage(responses);
                }
                yield null;
            }
        };
    }

    private static String asString(Object o, String def) {
        return o == null ? def : o.toString();
    }
}
