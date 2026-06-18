package com.fontal.cookagent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fontal.cookagent.app.agent.CookManus;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AgentController 单元测试 — 使用 pgvector profile + Mockito。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("pgvector")
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CookManus cookManus;

    // ========== SSE 流式 ==========

    @Nested
    @DisplayName("SSE 流式对话 GET /agent/chat/stream")
    class StreamTest {

        @Test
        @DisplayName("正常消息返回 SseEmitter")
        void streamWithValidMessage() throws Exception {
            when(cookManus.runStream(anyString())).thenReturn(new SseEmitter(300000L));

            mockMvc.perform(get("/agent/chat/stream")
                            .param("message", "红烧肉的做法"))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted());
        }

        @Test
        @DisplayName("消息为空时返回错误")
        void streamWithEmptyMessage() throws Exception {
            mockMvc.perform(get("/agent/chat/stream")
                            .param("message", ""))
                    .andExpect(status().isOk())
                    .andExpect(request().asyncStarted());
        }

        @Test
        @DisplayName("缺少 message 参数时返回 400")
        void streamWithoutMessageParam() throws Exception {
            mockMvc.perform(get("/agent/chat/stream"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========== 同步对话 ==========

    @Nested
    @DisplayName("同步对话 POST /agent/chat")
    class SyncChatTest {

        @Test
        @DisplayName("正常消息返回回复")
        void chatWithValidMessage() throws Exception {
            when(cookManus.run(anyString())).thenReturn("这是红烧肉的做法...");

            mockMvc.perform(post("/agent/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("message", "红烧肉的做法"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reply").value("这是红烧肉的做法..."));
        }

        @Test
        @DisplayName("消息为空时返回 400 + 错误码")
        void chatWithEmptyMessage() throws Exception {
            mockMvc.perform(post("/agent/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("message", ""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PARAM_INVALID"))
                    .andExpect(jsonPath("$.message").value("消息不能为空"))
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("请求体为空时返回 400")
        void chatWithEmptyBody() throws Exception {
            mockMvc.perform(post("/agent/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());
        }
    }
}
