package com.fontal.cookagent.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fontal.cookagent.entity.Recipe;
import com.fontal.cookagent.mapper.RecipeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 营养成分查询 Tool — 查询菜品的营养成分（热量、蛋白质、脂肪、碳水、钠等）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NutritionQueryTool {

    private final RecipeMapper recipeMapper;
    private final ObjectMapper mapper = new ObjectMapper();

    @Tool(description = "查询菜品的营养成分，包括热量、蛋白质、脂肪、碳水化合物、钠等。适合做营养分析、膳食搭配。")
    public String getNutrition(
            @ToolParam(description = "菜品名称，例如'老鸡汤'、'阳春面'", required = true) String recipeName) {

        List<Recipe> recipes = recipeMapper.selectList(
                new LambdaQueryWrapper<Recipe>().like(Recipe::getName, recipeName));

        if (recipes.isEmpty()) {
            return "未找到菜品「" + recipeName + "」。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【营养成分查询】\n\n");

        for (Recipe recipe : recipes) {
            sb.append("● ").append(recipe.getName()).append("\n");

            if (recipe.getNutritionJson() == null || recipe.getNutritionJson().isBlank()) {
                sb.append("  暂无营养成分数据\n\n");
                continue;
            }

            try {
                JsonNode nutrition = mapper.readTree(recipe.getNutritionJson());
                sb.append("  （每 100g）\n");
                nutrition.fields().forEachRemaining(field -> {
                    String key = field.getKey();
                    String value = field.getValue().asText();
                    sb.append("  - ").append(key).append("：").append(value).append("\n");
                });
            } catch (Exception e) {
                sb.append("  原始数据：").append(recipe.getNutritionJson()).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
