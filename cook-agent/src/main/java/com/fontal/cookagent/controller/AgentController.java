package com.fontal.cookagent.controller;

import com.fontal.cookagent.app.agent.CookManus;
import com.fontal.cookagent.common.BizException;
import com.fontal.cookagent.common.ErrorCode;
import com.fontal.cookagent.entity.AgentSession;
import com.fontal.cookagent.security.CurrentUser;
import com.fontal.cookagent.service.AgentSessionService;
import com.fontal.cookagent.service.AgentSummaryService;
import com.fontal.cookagent.service.HistoryService;
import com.fontal.cookagent.service.SessionTitleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * CookManus Agent 控制器 — SSE 流式对话 + 同步对话，支持多轮。
 */
@Tag(name = "Agent 对话", description = "CookManus ReAct Agent，支持 SSE 流式与同步对话，支持多轮会话")
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final CookManus cookManus;
    private final HistoryService historyService;
    private final AgentSummaryService agentSummaryService;
    private final AgentSessionService sessionService;
    private final SessionTitleService titleService;

    @Operation(summary = "SSE 流式对话", description = "实时推送 Agent 每步 think/act 结果，结束后推送总结。支持传 conversationId 续聊。")
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @RequestParam String message,
            @RequestParam(required = false) String conversationId) {
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
        // 获取或创建会话
        AgentSession session = getOrCreateSession(conversationId);
        Long userId = CurrentUser.get() != null ? CurrentUser.get().userId() : null;
        String firstMessage = message;
        // 流式执行（会话持久化在 AgentSummaryService 内完成）
        SseEmitter emitter = agentSummaryService.runStreamWithSummary(message, session, summary -> {
            // 总结生成后异步记录历史
            try {
                if (userId != null) {
                    historyService.record(userId, session.getConversationId(), message, summary, "AGENT");
                    if (!historyService.conversationExists(userId, session.getConversationId())) {
                        titleService.generateForAgent(session.getConversationId(), firstMessage);
                    }
                }
            } catch (Exception e) {
                // 历史记录失败不影响主流程
            }
        });
        return emitter;
    }

    @Operation(summary = "同步对话", description = "等待 Agent 执行完成，返回总结后的最终结果。支持传 conversationId 续聊。")
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String conversationId = request.get("conversationId");
        if (message == null || message.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "消息不能为空");
        }
        // 获取或创建会话
        AgentSession session = getOrCreateSession(conversationId);
        // 执行（会话持久化在 AgentSummaryService 内完成）
        String reply = agentSummaryService.runWithSummary(message, session);
        // 记录历史 + 生成标题
        recordAsync(message, session, reply);
        return Map.of(
                "conversationId", session.getConversationId(),
                "reply", reply,
                "status", session.getStatus()
        );
    }

    // ==================== 会话管理 ====================

    @Operation(summary = "加载 Agent 会话完整消息", description = "按会话 ID 获取 Agent 的完整消息列表（含工具调用步骤），供前端续聊时还原上下文")
    @GetMapping("/session/{conversationId}")
    public Map<String, Object> getSession(@PathVariable String conversationId) {
        Long userId = CurrentUser.requireUserId();
        AgentSession session = sessionService.loadSession(userId, conversationId);
        if (session == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        List<org.springframework.ai.chat.messages.Message> messages = sessionService.loadMessages(session);
        return Map.of(
                "conversationId", session.getConversationId(),
                "title", session.getTitle() == null ? "" : session.getTitle(),
                "status", session.getStatus(),
                "compressed", session.getCompressed(),
                "messages", messages.stream().map(this::toMessageView).toList()
        );
    }

    @Operation(summary = "关闭 Agent 会话", description = "关闭会话并压缩上下文，将执行步骤压缩为摘要，保留用户消息和摘要消息")
    @PostMapping("/session/{conversationId}/close")
    public Map<String, Object> closeSession(@PathVariable String conversationId) {
        Long userId = CurrentUser.requireUserId();
        AgentSession session = sessionService.closeSession(userId, conversationId);
        if (session == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        return Map.of(
                "conversationId", conversationId,
                "status", session.getStatus(),
                "compressed", true
        );
    }

    @Operation(summary = "列出当前用户的 Agent 会话", description = "按最后活动时间倒序，供侧边栏展示")
    @GetMapping("/sessions")
    public List<Map<String, Object>> listSessions() {
        Long userId = CurrentUser.requireUserId();
        List<AgentSession> sessions = sessionService.listSessions(userId);
        return sessions.stream().map(s -> Map.<String, Object>of(
                "conversationId", s.getConversationId(),
                "title", s.getTitle() == null ? "" : s.getTitle(),
                "status", s.getStatus(),
                "createdAt", s.getCreatedAt() == null ? "" : s.getCreatedAt().toString(),
                "updatedAt", s.getUpdatedAt() == null ? "" : s.getUpdatedAt().toString()
        )).toList();
    }

    @Operation(summary = "删除 Agent 会话")
    @DeleteMapping("/session/{conversationId}")
    public Map<String, Object> deleteSession(@PathVariable String conversationId) {
        Long userId = CurrentUser.requireUserId();
        boolean deleted = sessionService.deleteSession(userId, conversationId);
        historyService.deleteByConversation(userId, conversationId);
        return Map.of("conversationId", conversationId, "deleted", deleted);
    }

    // ==================== 辅助 ====================

    /** 获取已有会话或创建新会话 */
    private AgentSession getOrCreateSession(String conversationId) {
        Long userId = CurrentUser.requireUserId();
        if (conversationId != null && !conversationId.isBlank()) {
            AgentSession existing = sessionService.loadSession(userId, conversationId);
            if (existing != null) {
                // 恢复 ACTIVE 状态（CLOSED 的会话可重新激活继续对话）
                if ("CLOSED".equals(existing.getStatus())) {
                    existing.setStatus("ACTIVE");
                }
                return existing;
            }
        }
        return sessionService.createSession(userId);
    }

    /** 异步记录历史 + 生成标题 */
    private void recordAsync(String message, AgentSession session, String reply) {
        try {
            CurrentUser.AuthPrincipal user = CurrentUser.get();
            if (user == null) {
                return;
            }
            // 记录到 chat_history（供历史页展示）
            historyService.record(user.userId(), session.getConversationId(), message,
                    reply == null ? "" : reply, "AGENT");
            // 仅首条消息时生成标题
            if (!historyService.conversationExists(user.userId(), session.getConversationId())) {
                titleService.generateForAgent(session.getConversationId(), message);
            }
        } catch (Exception e) {
            // 历史记录失败不影响主流程
        }
    }

    /** 将 Message 转为前端可展示的视图 */
    private Map<String, Object> toMessageView(org.springframework.ai.chat.messages.Message msg) {
        return Map.of(
                "type", msg.getMessageType().name(),
                "content", msg.getText() == null ? "" : msg.getText()
        );
    }
}
