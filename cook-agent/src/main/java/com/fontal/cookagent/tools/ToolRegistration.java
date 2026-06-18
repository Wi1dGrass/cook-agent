package com.fontal.cookagent.tools;

import com.fontal.cookagent.mapper.CategoryMapper;
import com.fontal.cookagent.mapper.IngredientMapper;
import com.fontal.cookagent.mapper.RecipeMapper;
import com.fontal.cookagent.mapper.RecipeStepMapper;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 集中的工具注册类
 *
 * 当前工具集（10 个）：
 *  1. terminate            — 终止 Agent 循环
 *  2. searchRecipes        — 菜谱语义搜索（PGVector）
 *  3. recommendRecipes     — 菜谱推荐（PGVector）
 *  4. webSearch            — 火山引擎联网搜索
 *  5. searchImages         — Pexels 图片搜索
 *  6. listCategory         — 分类浏览 / 分类下菜品列表（MySQL）
 *  7. searchByIngredients  — 食材反查菜品（MySQL）
 *  8. compareRecipes       — 多菜品配方对比（MySQL）
 *  9. getNutrition         — 营养成分查询（MySQL）
 * 10. dailyRecommend       — 每日智能推荐（MySQL + 规则）
 */
@Configuration
public class ToolRegistration {

    @Value("${cook.pexels.api-key:}")
    private String pexelsApiKey;

    @Value("${cook.search.api-key:}")
    private String searchApiKey;

    private final VectorStore vectorStore;
    private final RecipeMapper recipeMapper;
    private final CategoryMapper categoryMapper;
    private final IngredientMapper ingredientMapper;
    private final RecipeStepMapper recipeStepMapper;

    public ToolRegistration(VectorStore vectorStore,
                            RecipeMapper recipeMapper,
                            CategoryMapper categoryMapper,
                            IngredientMapper ingredientMapper,
                            RecipeStepMapper recipeStepMapper) {
        this.vectorStore = vectorStore;
        this.recipeMapper = recipeMapper;
        this.categoryMapper = categoryMapper;
        this.ingredientMapper = ingredientMapper;
        this.recipeStepMapper = recipeStepMapper;
    }

    @Bean
    public ToolCallback[] allTools() {
        // 原有工具（PGVector + 联网）
        TerminateTool terminateTool = new TerminateTool();
        ImageSearchTool imageSearchTool = new ImageSearchTool(pexelsApiKey);
        RecipeSearchTool recipeSearchTool = new RecipeSearchTool(vectorStore);
        RecipeRecommendTool recipeRecommendTool = new RecipeRecommendTool(vectorStore);
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);

        // 新增工具（基于 MySQL 结构化数据）
        ListCategoryTool listCategoryTool = new ListCategoryTool(categoryMapper, recipeMapper);
        IngredientSearchTool ingredientSearchTool = new IngredientSearchTool(ingredientMapper, recipeMapper);
        RecipeCompareTool recipeCompareTool = new RecipeCompareTool(recipeMapper, ingredientMapper, recipeStepMapper);
        NutritionQueryTool nutritionQueryTool = new NutritionQueryTool(recipeMapper);
        DailyRecommendTool dailyRecommendTool = new DailyRecommendTool(recipeMapper, categoryMapper);

        return ToolCallbacks.from(
                terminateTool,
                recipeSearchTool,
                recipeRecommendTool,
                webSearchTool,
                imageSearchTool,
                listCategoryTool,
                ingredientSearchTool,
                recipeCompareTool,
                nutritionQueryTool,
                dailyRecommendTool
        );
    }
}
