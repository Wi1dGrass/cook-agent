package com.fontal.cookagent.controller;

import com.fontal.cookagent.app.agent.CookManus;
import com.fontal.cookagent.common.BizException;
import com.fontal.cookagent.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * CookManus Agent 控制器 — SSE 流式对话。
 *
 * <pre>
 * 流式调用：
 *   GET  /api/agent/chat/stream?message=帮我搜索红烧肉的做法
 *
 * 同步调用：
 *   POST /api/agent/chat
 *   Body: {"message": "帮我搜索红烧肉的做法"}
 *   Response: {"reply": "..."}
 * </pre>
 */
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final CookManus cookManus;

    /**
     * SSE 流式对话 — 每步 Agent 执行结果实时推送。
     */
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

    /**
     * 同步对话 — 等待 Agent 全部执行完成后返回最终结果。
     */
    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "消息不能为空");
        }
        String reply = cookManus.run(message);
        return Map.of("reply", reply);
    }
}
