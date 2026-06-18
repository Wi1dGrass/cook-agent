package com.fontal.cookagent.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fontal.cookagent.entity.Ingredient;
import com.fontal.cookagent.entity.Recipe;
import com.fontal.cookagent.mapper.IngredientMapper;
import com.fontal.cookagent.mapper.RecipeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 食材反查 Tool — 根据用户手头的食材，查找包含这些食材的菜品。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngredientSearchTool {

    private final IngredientMapper ingredientMapper;
    private final RecipeMapper recipeMapper;

    @Tool(description = "根据食材反查菜品。输入手头有的食材列表，返回能做这些菜的菜品。支持精确匹配和模糊匹配。")
    public String searchByIngredients(
            @ToolParam(description = "食材列表，逗号分隔，例如'鸡腿,花椒,辣椒'", required = true) String ingredients,
            @ToolParam(description = "匹配模式：'all' 表示必须包含全部食材，'any' 表示包含任一即可。默认 any") String matchMode) {

        List<String> ingredientList = Arrays.stream(ingredients.split("[,，]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (ingredientList.isEmpty()) {
            return "请提供至少一个食材名称。";
        }

        boolean matchAll = "all".equalsIgnoreCase(matchMode);

        // 查询每个食材命中的 recipeId
        Map<Long, Set<String>> recipeIngredientMap = new HashMap<>();
        for (String ingredientName : ingredientList) {
            List<Ingredient> matched = ingredientMapper.selectList(
                    new LambdaQueryWrapper<Ingredient>().like(Ingredient::getName, ingredientName));
            for (Ingredient ing : matched) {
                recipeIngredientMap
                        .computeIfAbsent(ing.getRecipeId(), k -> new HashSet<>())
                        .add(ingredientName);
            }
        }

        if (recipeIngredientMap.isEmpty()) {
            return "没有找到包含「" + ingredients + "」的菜品。";
        }

        // 根据匹配模式过滤
        List<Map.Entry<Long, Set<String>>> entries;
        if (matchAll) {
            entries = recipeIngredientMap.entrySet().stream()
                    .filter(e -> e.getValue().containsAll(ingredientList))
                    .toList();
            if (entries.isEmpty()) {
                return "没有找到同时包含全部食材（" + ingredients + "）的菜品。试试 matchMode='any'。";
            }
        } else {
            entries = new ArrayList<>(recipeIngredientMap.entrySet());
            // 按命中食材数排序
            entries.sort((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()));
        }

        // 查菜品详情，限制最多 10 条
        List<Long> recipeIds = entries.stream().limit(10).map(Map.Entry::getKey).toList();
        Map<Long, Recipe> recipeMap = recipeMapper.selectBatchIds(recipeIds).stream()
                .collect(Collectors.toMap(Recipe::getId, r -> r));

        StringBuilder sb = new StringBuilder();
        sb.append(matchAll ? "同时包含" : "包含部分").append("食材「").append(ingredients).append("」的菜品：\n\n");
        int rank = 1;
        for (Map.Entry<Long, Set<String>> entry : entries.stream().limit(10).toList()) {
            Recipe recipe = recipeMap.get(entry.getKey());
            if (recipe == null) continue;
            String matchedIngredients = String.join("、", entry.getValue());
            sb.append(String.format("%d. 【%s】(匹配: %s)\n", rank++, recipe.getName(), matchedIngredients));
            if (recipe.getSummary() != null && !recipe.getSummary().isBlank()) {
                sb.append("   简介：").append(recipe.getSummary()).append("\n");
            }
        }
        return sb.toString();
    }
}
