package com.fontal.cookagent.app.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.fontal.cookagent.app.agent.model.AgentState;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CookManus Agent 集成测试 — 需要 PGVector + DeepSeek API 在线。
 */
@SpringBootTest
@ActiveProfiles("pgvector")
class CookManusTest {

    @Autowired
    private CookManus cookManus;

    @BeforeEach
    void resetAgentState() {
        cookManus.setState(AgentState.IDLE);
        cookManus.setMessageList(new ArrayList<>());
        cookManus.setCurrentStep(0);
    }

    @Test
    @DisplayName("单轮菜谱搜索 — 搜索红烧肉做法")
    void searchSingleRecipe() {
        String result = cookManus.run("帮我搜索红烧肉的做法");

        assertThat(result).isNotBlank();
        // 应包含步骤信息或搜索结果
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("推荐 + 搜索组合任务")
    void recommendAndSearchCombo() {
        String result = cookManus.run("推荐一道适合新手的家常菜，并搜索它的详细做法");

        assertThat(result).isNotBlank();
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("非烹饪问题 — 常识回答")
    void nonCookingQuestion() {
        String result = cookManus.run("今天天气怎么样？");

        assertThat(result).isNotBlank();
        assertThat(result).isNotEmpty();
    }
}
