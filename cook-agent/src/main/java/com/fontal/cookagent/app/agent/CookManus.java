package com.fontal.cookagent.app.agent;

import com.fontal.cookagent.rag.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * cook的 AI 超级智能体（拥有自主规划能力，可以直接使用）
 */
@Component
public class CookManus extends ToolCallAgent {

    public CookManus(ToolCallback[] allTools, OpenAiChatModel chatModel) {
        super(allTools);
        this.setName("CookManus");
        String SYSTEM_PROMPT = """
                你是一位经验丰富的中餐厨师AI助手"CookManus"。
                你拥有搜索菜谱、推荐菜品、联网查资料、搜索图片等多种能力。
                请根据用户的需求，自主选择合适的工具来完成任务。
                回答时使用中文，语气亲切专业。
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                根据用户需求，主动选择最合适的工具或工具组合。
                复杂任务可以分步骤使用不同工具来解决。
                每次使用工具后，总结执行结果并给出下一步建议。
                任务完成后向用户总结你的发现和建议。
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);
        // 初始化 AI 对话客户端
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
