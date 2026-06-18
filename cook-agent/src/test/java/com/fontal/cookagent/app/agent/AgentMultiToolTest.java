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
 * CookManus Agent 多工具协作测试 — 验证 Agent 自主规划和工具组合能力。
 * 需要 PGVector + MySQL + DeepSeek API 在线。
 *
 * 覆盖 10 个工具的协作场景：
 *  - listCategory + searchRecipes     （先浏览分类再搜索）
 *  - searchByIngredients + compareRecipes （先反查再对比）
 *  - dailyRecommend + getNutrition    （先推荐再查营养）
 *  - searchRecipes + searchImages     （先搜菜谱再搜图）
 *  - webSearch + searchRecipes        （联网 + 知识库结合）
 */
@SpringBootTest
@ActiveProfiles("pgvector")
class AgentMultiToolTest {

    @Autowired
    private CookManus cookManus;

    @BeforeEach
    void resetAgentState() {
        cookManus.setState(AgentState.IDLE);
        cookManus.setMessageList(new ArrayList<>());
        cookManus.setCurrentStep(0);
    }

    @Test
    @DisplayName("场景1 — 浏览分类 + 搜索具体菜品")
    void scenario_browseAndSearch() {
        String result = cookManus.run("帮我看看汤类有哪些菜，然后挑一道告诉我详细做法");

        assertThat(result).isNotBlank();
        System.out.println("【场景1结果】\n" + result);
    }

    @Test
    @DisplayName("场景2 — 食材反查 + 配方对比")
    void scenario_ingredientAndCompare() {
        String result = cookManus.run("我家里有鸡肉，帮我找出能做的菜，然后对比其中两道菜的用料差异");

        assertThat(result).isNotBlank();
        System.out.println("【场景2结果】\n" + result);
    }

    @Test
    @DisplayName("场景3 — 每日推荐 + 营养分析")
    void scenario_recommendAndNutrition() {
        String result = cookManus.run("推荐一份清淡的晚餐搭配，并告诉我其中一道菜的营养成分");

        assertThat(result).isNotBlank();
        System.out.println("【场景3结果】\n" + result);
    }

    @Test
    @DisplayName("场景4 — 搜索菜谱 + 搜索图片")
    void scenario_searchAndImage() {
        String result = cookManus.run("搜索红烧肉的做法，并找一张红烧肉的图片");

        assertThat(result).isNotBlank();
        System.out.println("【场景4结果】\n" + result);
    }

    @Test
    @DisplayName("场景5 — 联网搜索 + 知识库结合")
    void scenario_webAndKnowledge() {
        String result = cookManus.run("炖鸡汤有什么小窍门？先查查我们知识库里的做法，再联网补充一些技巧");

        assertThat(result).isNotBlank();
        System.out.println("【场景5结果】\n" + result);
    }

    @Test
    @DisplayName("场景6 — 复杂多步骤任务")
    void scenario_complexMultiStep() {
        String result = cookManus.run("我想学做卤味，帮我：1. 列出卤菜分类 2. 推荐一道经典的 3. 查它的做法 4. 对比另一道卤菜");

        assertThat(result).isNotBlank();
        System.out.println("【场景6结果】\n" + result);
    }
}
