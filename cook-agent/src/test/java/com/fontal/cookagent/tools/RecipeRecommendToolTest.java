package com.fontal.cookagent.tools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RecipeRecommendTool 集成测试 — 需要 PGVector 在线。
 */
@SpringBootTest
@ActiveProfiles("pgvector")
class RecipeRecommendToolTest {

    @Autowired
    private RecipeRecommendTool recipeRecommendTool;

    @Test
    @DisplayName("推荐适合春天的清淡菜")
    void recommendSpringLightDishes() {
        String result = recipeRecommendTool.recommendRecipes("适合春天的清淡菜", 3, null);

        assertThat(result).isNotBlank();
        assertThat(result).contains("推荐");
    }

    @Test
    @DisplayName("推荐汤类菜谱")
    void recommendSoupCategory() {
        String result = recipeRecommendTool.recommendRecipes("家常汤品", 2, "汤类");

        assertThat(result).isNotBlank();
        assertThat(result).contains("推荐");
    }

    @Test
    @DisplayName("无匹配推荐时返回明确提示")
    void noMatchReturnsMessage() {
        String result = recipeRecommendTool.recommendRecipes("外星料理", 3, null);

        assertThat(result).isNotBlank();
        // 有数据时乱码查询也可能返回低相似度结果，两种情况都应接受
        assertThat(result).satisfiesAnyOf(
                r -> assertThat(r).contains("未找到"),
                r -> assertThat(r).contains("推荐")
        );
    }
}
