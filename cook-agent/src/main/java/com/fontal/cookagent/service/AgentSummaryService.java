package com.fontal.cookagent.service;

import cn.hutool.core.util.StrUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fontal.cookagent.app.agent.CookManus;
import com.fontal.cookagent.app.agent.model.AgentState;
import com.fontal.cookagent.entity.AgentSession;
import com.fontal.cookagent.rag.advisor.MyLoggerAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Agent 总结服务 — 在 Agent 执行结束后，将执行轨迹总结为对用户友好的最终回复。
 * <p>
 * 支持多轮对话：通过 {@link AgentSessionService} 持久化和恢复 Agent 上下文。
 * <p>
 * 同步：cookManus.run() → summarize()
 * 流式：自己驱动 cookManus.step() 循环 → 每步推送 → 最后推送总结
 */
@Slf4j
@Service
public class AgentSummaryService {

    private static final String SUMMARY_SYSTEM = """
            你是中餐厨师AI助手"CookManus"。你的任务是把 Agent 的执行记录整理成对用户的最终回复。
            要求：
            - 用中文，语气亲切专业，像一位懂行的厨师朋友
            - 直接回应用户的原始问题，不要罗列执行步骤
            - 整合工具返回的关键信息，提炼成对用户有价值的内容
            - 菜谱做法按步骤清晰呈现，并标注刀工/火候/调味等关键技巧
            - 菜品推荐说明推荐理由
            - 不要提及"工具"、"步骤"、"调用"等内部执行细节
            - 如果记录中包含错误或未找到信息，如实告知并给出建议
            - 若对话有历史上下文，结合历史继续回答，保持连贯性
            """;

    private static final String SUMMARY_USER = """
            用户问题：
            {userMessage}

            Agent 执行记录：
            {trace}

            请基于以上记录，为用户生成最终回复。""";

    private final ChatClient summaryChatClient;
    private final CookManus cookManus;
    private final AgentSessionService sessionService;
    private final AgentContextCompressor compressor;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public AgentSummaryService(OpenAiChatModel chatModel, CookManus cookManus,
                                AgentSessionService sessionService, AgentContextCompressor compressor) {
        this.summaryChatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.cookManus = cookManus;
        this.sessionService = sessionService;
        this.compressor = compressor;
    }

    /**
     * 反转义工具返回结果中的 JSON 字符串编码。
     */
    private String unescapeToolResponse(String text) {
        if (text == null || !text.contains("返回的结果：")) {
            return text;
        }
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int idx = line.indexOf("返回的结果：");
            if (idx >= 0) {
                String prefix = line.substring(0, idx + "返回的结果：".length());
                String rest = line.substring(idx + "返回的结果：".length());
                if (rest.startsWith("\"")) {
                    try {
                        String decoded = objectMapper.readValue(rest, String.class);
                        line = prefix + decoded;
                    } catch (Exception ignored) {
                    }
                }
            }
            sb.append(line);
            if (i < lines.length - 1) sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 将 Agent 状态恢复到 IDLE 并清理上下文（供下次调用使用）。
     */
    private void resetAgentState() {
        cookManus.setState(AgentState.IDLE);
        cookManus.setMessageList(new ArrayList<>());
        cookManus.setCurrentStep(0);
    }

    /**
     * 将 Agent 执行轨迹总结为用户友好的最终回复。
     */
    public String summarize(String userMessage, String trace) {
        if (StrUtil.isBlank(trace)) {
            return "未能获取到执行结果。";
        }
        try {
            String userText = SUMMARY_USER
                    .replace("{userMessage}", userMessage == null ? "" : userMessage)
                    .replace("{trace}", trace);
            return summaryChatClient.prompt()
                    .system(SUMMARY_SYSTEM)
                    .user(userText)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("Agent 总结失败，返回原始轨迹: {}", e.getMessage());
            return trace;
        }
    }

    // ==================== 同步执行（多轮） ====================

    /**
     * 同步执行 Agent 并返回总结。支持多轮：从 session 恢复上下文，执行后保存。
     *
     * @param message 用户输入
     * @param session Agent 会话（若为 null 则不持久化）
     * @return 总结回复
     */
    public String runWithSummary(String message, AgentSession session) {
        // 从 session 恢复上下文
        if (session != null) {
            List<org.springframework.ai.chat.messages.Message> restored = sessionService.loadMessages(session);
            cookManus.setMessageList(new ArrayList<>(restored));
            cookManus.setCurrentStep(session.getCurrentStep() != null ? session.getCurrentStep() : 0);
            cookManus.setState(AgentState.IDLE);
        }

        String trace;
        try {
            trace = cookManus.run(message);
        } finally {
            // 即使出错也保存上下文（便于调试和续聊）
            if (session != null) {
                try {
                    sessionService.saveMessages(session, cookManus.getMessageList(),
                            cookManus.getCurrentStep(), true);
                } catch (Exception e) {
                    log.warn("保存 Agent 会话失败: {}", e.getMessage());
                }
            }
            resetAgentState();
        }
        trace = unescapeToolResponse(trace);
        return summarize(message, trace);
    }

    // ==================== 流式执行（多轮） ====================

    /**
     * 流式执行 Agent，每步推送结果，最后推送总结。支持多轮。
     *
     * @param message 用户输入
     * @param session Agent 会话（若为 null 则不持久化）
     * @param onSummaryReady 总结生成后的回调（接收总结文本，用于异步记录历史）
     * @return SseEmitter
     */
    public SseEmitter runStreamWithSummary(String message, AgentSession session,
                                            java.util.function.Consumer<String> onSummaryReady) {
        SseEmitter emitter = new SseEmitter(300000L);
        CompletableFuture.runAsync(() -> {
            boolean ownsRun = false;
            try {
                if (StrUtil.isBlank(message)) {
                    emitter.send("错误：消息不能为空");
                    emitter.complete();
                    return;
                }
                if (cookManus.getState() != AgentState.IDLE) {
                    emitter.send("错误：Agent 正忙，请稍后再试");
                    emitter.complete();
                    return;
                }
                // 从 session 恢复上下文
                if (session != null) {
                    List<org.springframework.ai.chat.messages.Message> restored = sessionService.loadMessages(session);
                    cookManus.setMessageList(new ArrayList<>(restored));
                    cookManus.setCurrentStep(session.getCurrentStep() != null ? session.getCurrentStep() : 0);
                } else {
                    cookManus.setMessageList(new ArrayList<>());
                    cookManus.setCurrentStep(0);
                }
                cookManus.setState(AgentState.RUNNING);
                ownsRun = true;
                cookManus.getMessageList().add(new UserMessage(message));

                List<String> traceList = new ArrayList<>();
                int maxSteps = cookManus.getMaxSteps();
                // 执行 think-act 循环
                for (int i = 0; i < maxSteps && cookManus.getState() != AgentState.FINISHED; i++) {
                    int stepNumber = i + 1;
                    cookManus.setCurrentStep(stepNumber);
                    String stepResult = cookManus.step();
                    stepResult = unescapeToolResponse(stepResult);
                    String result = "Step " + stepNumber + ": " + stepResult;
                    traceList.add(result);
                    emitter.send(result);

                    // 每步后检查是否需要自动压缩（compressIfNeeded 内部判断阈值）
                    if (session != null) {
                        List<org.springframework.ai.chat.messages.Message> compressed =
                                compressor.compressIfNeeded(cookManus.getMessageList());
                        if (compressed.size() != cookManus.getMessageList().size()) {
                            cookManus.setMessageList(compressed);
                        }
                    }
                }
                // 超出步骤限制
                if (cookManus.getCurrentStep() >= maxSteps) {
                    cookManus.setState(AgentState.FINISHED);
                    String terminateMsg = "Terminated: Reached max steps (" + maxSteps + ")";
                    traceList.add(terminateMsg);
                    emitter.send(terminateMsg);
                }
                // 生成并推送总结
                String trace = String.join("\n", traceList);
                String summary;
                try {
                    summary = summarize(message, trace);
                } catch (Exception e) {
                    summary = "总结生成失败，以上为完整执行记录。";
                }
                emitter.send("【最终总结】\n" + summary);
                // 触发回调（用于异步记录历史）
                if (onSummaryReady != null) {
                    try {
                        onSummaryReady.accept(summary);
                    } catch (Exception cbErr) {
                        log.warn("onSummaryReady 回调失败: {}", cbErr.getMessage());
                    }
                }
                emitter.complete();
            } catch (Exception e) {
                if (ownsRun) {
                    cookManus.setState(AgentState.ERROR);
                }
                log.error("Agent 流式执行出错", e);
                try {
                    emitter.send("执行错误：" + e.getMessage());
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            } finally {
                // 保存会话上下文
                if (ownsRun && session != null) {
                    try {
                        sessionService.saveMessages(session, cookManus.getMessageList(),
                                cookManus.getCurrentStep(), true);
                    } catch (Exception e) {
                        log.warn("保存 Agent 流式会话失败: {}", e.getMessage());
                    }
                }
                if (ownsRun) {
                    resetAgentState();
                }
            }
        });

        emitter.onTimeout(() -> {
            resetAgentState();
            log.warn("SSE connection timeout");
        });
        return emitter;
    }
}
