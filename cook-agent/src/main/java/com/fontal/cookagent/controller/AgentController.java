package com.fontal.cookagent.controller;

import com.fontal.cookagent.app.agent.CookManus;
import com.fontal.cookagent.common.BizException;
import com.fontal.cookagent.common.ErrorCode;
import com.fontal.cookagent.security.CurrentUser;
import com.fontal.cookagent.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;
import java.util.Map;

/**
 * CookManus Agent 控制器 — SSE 流式对话。
 */
@Tag(name = "Agent 对话", description = "CookManus ReAct Agent，支持 SSE 流式与同步对话")
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final CookManus cookManus;
    private final HistoryService historyService;

    @Operation(summary = "SSE 流式对话", description = "实时推送 Agent 每步 think/act/observation 结果")
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestParam String message) {
        if (message.isBlank()) {
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send("消息不能为空");
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }
        return cookManus.runStream(message);
    }

    @Operation(summary = "同步对话", description = "等待 Agent 全部执行完成后返回最终结果")
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "消息不能为空");
        }
        String reply = cookManus.run(message);
        // Agent 是单次执行没 conversationId，按需生成
        String conversationId = UUID.randomUUID().toString();
        try {
            CurrentUser.AuthPrincipal user = CurrentUser.get();
            if (user != null) {
                historyService.record(user.userId(), conversationId, message, reply, "AGENT");
            }
        } catch (Exception ignored) {
        }
        return Map.of("conversationId", conversationId, "reply", reply);
    }
}
