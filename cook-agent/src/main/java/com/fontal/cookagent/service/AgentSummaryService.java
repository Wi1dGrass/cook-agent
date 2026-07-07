package com.fontal.cookagent.service;

import cn.hutool.core.util.StrUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fontal.cookagent.app.agent.CookManus;
import com.fontal.cookagent.app.agent.model.AgentState;
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
 * 不修改原有 Agent 核心代码（BaseAgent/ReActAgent/ToolCallAgent/CookManus），
 * 仅通过调用公开 API 编排"执行 + 总结"流程。
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
            """;

    private static final String SUMMARY_USER = """
            用户问题：
            {userMessage}

            Agent 执行记录：
            {trace}

            请基于以上记录，为用户生成最终回复。""";

    private final ChatClient summaryChatClient;
    private final CookManus cookManus;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public AgentSummaryService(OpenAiChatModel chatModel, CookManus cookManus) {
        this.summaryChatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.cookManus = cookManus;
    }

    /**
     * 反转义工具返回结果中的 JSON 字符串编码。
     * <p>
     * Spring AI 的 {@code DefaultToolCallResultConverter} 用 {@code JsonParser.toJson()} 序列化工具返回值，
     * 对 String 类型会加引号并转义换行符为字面 {@code \n}（反斜杠+n）。
     * {@code ToolCallAgent.act()} 拼接格式为 {@code 工具 XXX 返回的结果："JSON字符串"}，
     * 每个工具响应占一行（用真正换行分隔）。
     * <p>
     * 此方法提取每行中 JSON 编码的部分并用 Jackson 反序列化，恢复真正的换行和原始内容。
     *
     * @param text step() 返回的原始结果
     * @return 反转义后的可读文本
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
     * 重置 Agent 单例状态到 IDLE，清理消息上下文和步骤计数。
     * <p>
     * CookManus 是 Spring 单例，BaseAgent.run() / runStream() 执行完毕后 state 会停留在
     * FINISHED 或 ERROR，且 BaseAgent.cleanup() 在子类中未被重写（空实现），
     * 导致后续请求因 state != IDLE 被永久阻塞。此方法确保每次调用后状态归位。
     */
    private void resetAgentState() {
        cookManus.setState(AgentState.IDLE);
        cookManus.setMessageList(new ArrayList<>());
        cookManus.setCurrentStep(0);
    }

    /**
     * 将 Agent 执行轨迹总结为用户友好的最终回复。
     *
     * @param userMessage 用户原始问题
     * @param trace       Agent 执行轨迹（Step 1: ... Step 2: ...）
     * @return 总结回复；总结失败时返回原始轨迹
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

    /**
     * 同步执行 Agent 并返回总结。
     *
     * @param message 用户输入
     * @return 总结回复
     */
    public String runWithSummary(String message) {
        String trace;
        try {
            trace = cookManus.run(message);
        } finally {
            resetAgentState();
        }
        trace = unescapeToolResponse(trace);
        return summarize(message, trace);
    }

    /**
     * 流式执行 Agent，每步推送结果，最后推送总结。
     * <p>
     * 自己驱动 cookManus.step() 循环（调用公开 API），以便在结尾追加总结事件，
     * 而不修改 BaseAgent.runStream 的核心代码。
     *
     * @param message 用户输入
     * @return SseEmitter
     */
    public SseEmitter runStreamWithSummary(String message) {
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
                // 初始化 Agent 状态（复刻 BaseAgent.runStream 的启动逻辑）
                cookManus.setState(AgentState.RUNNING);
                ownsRun = true;
                cookManus.setMessageList(new ArrayList<>());
                cookManus.setCurrentStep(0);
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
                // 只有当前请求持有 Agent 控制权时才重置，避免误重置其他并发执行的状态
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
