package com.fontal.cookagent.tools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 新增工具集集成测试 — 5 个 MySQL 数据驱动的工具。
 * 需要 MySQL（菜品结构化数据）在线。
 */
@SpringBootTest
@ActiveProfiles("pgvector")
class NewToolsTest {

    @Autowired
    private ListCategoryTool listCategoryTool;

    @Autowired
    private IngredientSearchTool ingredientSearchTool;

    @Autowired
    private RecipeCompareTool recipeCompareTool;

    @Autowired
    private NutritionQueryTool nutritionQueryTool;

    @Autowired
    private DailyRecommendTool dailyRecommendTool;

    // ==================== ListCategoryTool ====================

    @Test
    @DisplayName("列出所有分类")
    void listAllCategories() {
        String result = listCategoryTool.listCategory(null);

        assertThat(result).isNotBlank();
        assertThat(result).contains("菜品分类列表");
        System.out.println("【分类列表】\n" + result);
    }

    @Test
    @DisplayName("列出指定分类下的菜品")
    void listCategoryByName() {
        String result = listCategoryTool.listCategory("汤");

        assertThat(result).isNotBlank();
        System.out.println("【汤类菜品】\n" + result);
    }

    @Test
    @DisplayName("不存在的分类返回提示")
    void categoryNotExist() {
        String result = listCategoryTool.listCategory("不存在的分类");

        assertThat(result).contains("不存在");
    }

    // ==================== IngredientSearchTool ====================

    @Test
    @DisplayName("食材反查 — 鸡腿")
    void searchBySingleIngredient() {
        String result = ingredientSearchTool.searchByIngredients("鸡腿", "any");

        assertThat(result).isNotBlank();
        System.out.println("【含鸡腿的菜品】\n" + result);
    }

    @Test
    @DisplayName("食材反查 — 多食材任一匹配")
    void searchByMultipleIngredientsAny() {
        String result = ingredientSearchTool.searchByIngredients("鸡蛋,辣椒", "any");

        assertThat(result).isNotBlank();
        System.out.println("【含鸡蛋或辣椒的菜品】\n" + result);
    }

    @Test
    @DisplayName("食材反查 — 多食材全部匹配")
    void searchByMultipleIngredientsAll() {
        String result = ingredientSearchTool.searchByIngredients("鸡,盐", "all");

        assertThat(result).isNotBlank();
        System.out.println("【同时含鸡和盐的菜品】\n" + result);
    }

    // ==================== RecipeCompareTool ====================

    @Test
    @DisplayName("对比两道菜品的配方")
    void compareTwoRecipes() {
        String result = recipeCompareTool.compareRecipes("老鸡汤,肥西老母鸡汤");

        assertThat(result).isNotBlank();
        assertThat(result).contains("菜谱对比分析");
        System.out.println("【菜谱对比】\n" + result);
    }

    @Test
    @DisplayName("对比时菜品不足返回提示")
    void compareSingleRecipe() {
        String result = recipeCompareTool.compareRecipes("阳春面");

        assertThat(result).contains("至少需要两个");
    }

    // ==================== NutritionQueryTool ====================

    @Test
    @DisplayName("查询菜品营养成分")
    void getNutrition() {
        String result = nutritionQueryTool.getNutrition("老鸡汤");

        assertThat(result).isNotBlank();
        System.out.println("【营养成分】\n" + result);
    }

    @Test
    @DisplayName("查询不存在的菜品营养")
    void getNutritionNotFound() {
        String result = nutritionQueryTool.getNutrition("不存在的菜");

        assertThat(result).contains("未找到");
    }

    // ==================== DailyRecommendTool ====================

    @Test
    @DisplayName("每日推荐 — 默认偏好")
    void dailyRecommendDefault() {
        String result = dailyRecommendTool.dailyRecommend(null, 1);

        assertThat(result).isNotBlank();
        assertThat(result).contains("每日推荐");
        System.out.println("【每日推荐】\n" + result);
    }

    @Test
    @DisplayName("每日推荐 — 清淡口味")
    void dailyRecommendWithPreference() {
        String result = dailyRecommendTool.dailyRecommend("清淡", 2);

        assertThat(result).isNotBlank();
        assertThat(result).contains("清淡");
        System.out.println("【清淡推荐】\n" + result);
    }
}
