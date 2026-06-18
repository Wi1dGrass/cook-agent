package com.fontal.cookagent.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fontal.cookagent.entity.Ingredient;
import com.fontal.cookagent.entity.Recipe;
import com.fontal.cookagent.entity.RecipeStep;
import com.fontal.cookagent.mapper.IngredientMapper;
import com.fontal.cookagent.mapper.RecipeMapper;
import com.fontal.cookagent.mapper.RecipeStepMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜谱对比 Tool — 对比多个菜品的配料和步骤差异。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecipeCompareTool {

    private final RecipeMapper recipeMapper;
    private final IngredientMapper ingredientMapper;
    private final RecipeStepMapper recipeStepMapper;

    @Tool(description = "对比多个菜品的配方差异。并排展示相同原料、不同原料和用量差异。适合做配方分析。")
    public String compareRecipes(
            @ToolParam(description = "要对比的菜品名称列表，逗号分隔，例如'卤鸡腿,口水鸡'", required = true) String recipeNames) {

        List<String> names = Arrays.stream(recipeNames.split("[,，]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (names.size() < 2) {
            return "对比至少需要两个菜品。请用逗号分隔，例如'卤鸡腿,口水鸡'。";
        }
        if (names.size() > 4) {
            return "最多支持对比 4 个菜品。";
        }

        // 查询菜品
        List<Recipe> recipes = recipeMapper.selectList(
                new LambdaQueryWrapper<Recipe>().in(Recipe::getName, names));

        if (recipes.size() < 2) {
            List<String> found = recipes.stream().map(Recipe::getName).toList();
            return "找不到足够的菜品进行对比。找到：" + found + "，输入：" + names;
        }

        // 加载每个菜品的配料
        Map<String, Map<String, Ingredient>> recipeIngredientMaps = new LinkedHashMap<>();
        for (Recipe recipe : recipes) {
            List<Ingredient> ings = ingredientMapper.selectList(
                    new LambdaQueryWrapper<Ingredient>().eq(Ingredient::getRecipeId, recipe.getId()));
            Map<String, Ingredient> ingMap = ings.stream()
                    .collect(Collectors.toMap(Ingredient::getName, i -> i, (a, b) -> a));
            recipeIngredientMaps.put(recipe.getName(), ingMap);
        }

        // 收集所有原料
        Set<String> allIngredients = new TreeSet<>();
        recipeIngredientMaps.values().forEach(m -> allIngredients.addAll(m.keySet()));

        // 分类：共同原料 vs 各自独有原料
        StringBuilder sb = new StringBuilder();
        sb.append("【菜谱对比分析】").append(String.join(" vs ", recipes.stream().map(Recipe::getName).toList())).append("\n\n");

        // 共同原料
        List<String> common = allIngredients.stream()
                .filter(name -> recipes.stream().allMatch(r -> recipeIngredientMaps.get(r.getName()).containsKey(name)))
                .toList();
        sb.append("▶ 共同原料（").append(common.size()).append("）：\n");
        if (common.isEmpty()) {
            sb.append("  无共同原料\n");
        } else {
            for (String ing : common) {
                sb.append("  - ").append(ing);
                // 展示每个菜品的用量
                List<String> quantities = recipes.stream()
                        .map(r -> {
                            Ingredient i = recipeIngredientMaps.get(r.getName()).get(ing);
                            String qty = i.getQuantity() != null ? i.getQuantity() : "—";
                            return r.getName() + ":" + qty;
                        })
                        .toList();
                sb.append("（").append(String.join("，", quantities)).append("）\n");
            }
        }

        // 各自独有的原料
        sb.append("\n▶ 各菜品独有原料：\n");
        for (Recipe recipe : recipes) {
            Set<String> others = recipes.stream()
                    .filter(r -> !r.getName().equals(recipe.getName()))
                    .flatMap(r -> recipeIngredientMaps.get(r.getName()).keySet().stream())
                    .collect(Collectors.toSet());
            List<String> unique = recipeIngredientMaps.get(recipe.getName()).keySet().stream()
                    .filter(name -> !others.contains(name))
                    .toList();
            sb.append("  ").append(recipe.getName()).append(" 独有（").append(unique.size()).append("）：");
            sb.append(unique.isEmpty() ? "无" : String.join("、", unique));
            sb.append("\n");
        }

        // 配料总数对比
        sb.append("\n▶ 配料总数：\n");
        for (Recipe recipe : recipes) {
            int total = recipeIngredientMaps.get(recipe.getName()).size();
            sb.append("  ").append(recipe.getName()).append("：").append(total).append(" 种\n");
        }

        return sb.toString();
    }
}
