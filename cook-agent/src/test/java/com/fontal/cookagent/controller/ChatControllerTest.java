package com.fontal.cookagent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fontal.cookagent.app.chat.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ChatController 单元测试 — 使用 pgvector profile + Mockito。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("pgvector")
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatService chatService;

    // ========== 新对话 ==========

    @Nested
    @DisplayName("新对话 POST /chat/new")
    class NewChatTest {

        @Test
        @DisplayName("正常消息返回 conversationId + reply")
        void newChatWithValidMessage() throws Exception {
            when(chatService.startNewChat(anyString()))
                    .thenReturn(new ChatService.ChatResult("uuid-123", "红烧肉的做法是..."));

            mockMvc.perform(post("/chat/new")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("message", "红烧肉怎么做"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conversationId").value("uuid-123"))
                    .andExpect(jsonPath("$.reply").value("红烧肉的做法是..."));
        }

        @Test
        @DisplayName("消息为空时返回 400 + 错误码")
        void newChatWithEmptyMessage() throws Exception {
            mockMvc.perform(post("/chat/new")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("message", ""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PARAM_INVALID"))
                    .andExpect(jsonPath("$.message").value("消息不能为空"))
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("请求体为空时返回 400")
        void newChatWithEmptyBody() throws Exception {
            mockMvc.perform(post("/chat/new")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("缺少 message 字段时返回 400 + 错误码")
        void newChatWithoutMessageField() throws Exception {
            mockMvc.perform(post("/chat/new")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PARAM_INVALID"))
                    .andExpect(jsonPath("$.message").value("消息不能为空"))
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    // ========== 继续对话 ==========

    @Nested
    @DisplayName("继续对话 POST /chat/send")
    class SendChatTest {

        @Test
        @DisplayName("正常 conversationId + 消息返回回复")
        void sendWithValidRequest() throws Exception {
            when(chatService.chat(anyString(), anyString()))
                    .thenReturn("需要五花肉、冰糖、生抽...");

            mockMvc.perform(post("/chat/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("conversationId", "uuid-123", "message", "需要什么材料"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reply").value("需要五花肉、冰糖、生抽..."));
        }

        @Test
        @DisplayName("缺少 conversationId 时返回 400 + 错误码")
        void sendWithoutConversationId() throws Exception {
            mockMvc.perform(post("/chat/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("message", "需要什么材料"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PARAM_INVALID"))
                    .andExpect(jsonPath("$.message").value("conversationId 不能为空"))
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("conversationId 为空时返回 400 + 错误码")
        void sendWithEmptyConversationId() throws Exception {
            mockMvc.perform(post("/chat/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("conversationId", "", "message", "需要什么材料"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PARAM_INVALID"))
                    .andExpect(jsonPath("$.message").value("conversationId 不能为空"))
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("消息为空时返回 400 + 错误码")
        void sendWithEmptyMessage() throws Exception {
            mockMvc.perform(post("/chat/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("conversationId", "uuid-123", "message", ""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PARAM_INVALID"))
                    .andExpect(jsonPath("$.message").value("消息不能为空"))
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("请求体为空时返回 400")
        void sendWithEmptyBody() throws Exception {
            mockMvc.perform(post("/chat/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());
        }
    }
}
