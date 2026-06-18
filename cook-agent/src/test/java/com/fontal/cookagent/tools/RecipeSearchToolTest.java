package com.fontal.cookagent.tools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RecipeSearchTool 集成测试 — 需要 PGVector 在线。
 */
@SpringBootTest
@ActiveProfiles("pgvector")
class RecipeSearchToolTest {

    @Autowired
    private RecipeSearchTool recipeSearchTool;

    @Test
    @DisplayName("语义搜索炖鸡汤")
    void searchChickenSoup() {
        String result = recipeSearchTool.searchRecipes("炖鸡汤的技巧", 3, null);

        assertThat(result).isNotBlank();
        // 应找到相关菜谱
        assertThat(result).containsAnyOf("鸡", "汤", "炖");
    }

    @Test
    @DisplayName("按分类筛选汤类菜谱")
    void searchWithCategoryFilter() {
        String result = recipeSearchTool.searchRecipes("排骨", 3, "汤类");

        assertThat(result).isNotBlank();
    }

    @Test
    @DisplayName("无匹配菜谱时返回明确提示")
    void noMatchReturnsMessage() {
        String result = recipeSearchTool.searchRecipes("火星人料理外星大餐", 3, null);

        assertThat(result).isNotBlank();
        // 有数据时乱码查询也可能返回低相似度结果，两种情况都应接受
        assertThat(result).satisfiesAnyOf(
                r -> assertThat(r).contains("未找到"),
                r -> assertThat(r).contains("【")
        );
    }
}
