package com.fontal.cookagent.controller;

import com.fontal.cookagent.app.chat.ChatService;
import com.fontal.cookagent.common.BizException;
import com.fontal.cookagent.common.ErrorCode;
import com.fontal.cookagent.security.CurrentUser;
import com.fontal.cookagent.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 普通对话控制器 — 带 RAG 检索 + JDBC 记忆持久化。
 */
@Tag(name = "普通对话", description = "带 RAG 检索增强 + 多轮记忆的普通对话接口")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final HistoryService historyService;

    @Operation(summary = "开始新对话", description = "开启新对话并返回 conversationId 和首轮回复")
    @PostMapping("/new")
    public Map<String, Object> newChat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "消息不能为空");
        }
        ChatService.ChatResult result = chatService.startNewChat(message);
        recordHistory(message, result.conversationId(), result.reply(), "CHAT");
        return Map.of(
                "conversationId", result.conversationId(),
                "reply", result.reply()
        );
    }

    @Operation(summary = "继续对话", description = "在已有 conversationId 中发送后续消息，自动注入历史记忆")
    @PostMapping("/send")
    public Map<String, String> send(@RequestBody Map<String, String> request) {
        String conversationId = request.get("conversationId");
        String message = request.get("message");
        if (conversationId == null || conversationId.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "conversationId 不能为空");
        }
        if (message == null || message.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "消息不能为空");
        }
        String reply = chatService.chat(conversationId, message);
        recordHistory(message, conversationId, reply, "CHAT");
        return Map.of("reply", reply);
    }

    /** 仅当已登录用户存在时记录，避免公开调用写入失败 */
    private void recordHistory(String query, String conversationId, String reply, String channel) {
        try {
            CurrentUser.AuthPrincipal user = CurrentUser.get();
            if (user != null) {
                historyService.record(user.userId(), conversationId, query, reply, channel);
            }
        } catch (Exception e) {
            // 历史记录失败不影响主流程
        }
    }
}
