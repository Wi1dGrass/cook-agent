package com.fontal.cookagent.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fontal.cookagent.entity.Category;
import com.fontal.cookagent.entity.Recipe;
import com.fontal.cookagent.mapper.CategoryMapper;
import com.fontal.cookagent.mapper.RecipeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 分类列表 Tool — 列出菜品分类，或列出某分类下的全部菜品。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ListCategoryTool {

    private final CategoryMapper categoryMapper;
    private final RecipeMapper recipeMapper;

    @Tool(description = "列出所有菜品分类，或列出指定分类下的全部菜品。用于浏览菜单、查看分类构成。")
    public String listCategory(
            @ToolParam(description = "分类名称，例如'主食'、'汤类'。留空则列出所有分类。") String categoryName) {

        // 无分类参数 → 列出所有分类 + 每个分类的菜品数
        if (categoryName == null || categoryName.isBlank()) {
            List<Category> categories = categoryMapper.selectList(null);
            if (categories.isEmpty()) {
                return "系统中暂无分类数据。";
            }
            return categories.stream()
                    .map(c -> {
                        long count = recipeMapper.selectCount(
                                new LambdaQueryWrapper<Recipe>().eq(Recipe::getCategoryId, c.getId()));
                        return String.format("- %s（%d 道菜品）", c.getName(), count);
                    })
                    .collect(Collectors.joining("\n", "菜品分类列表：\n", ""));
        }

        // 有分类参数 → 列出该分类下所有菜品
        Category category = categoryMapper.selectOne(
                new LambdaQueryWrapper<Category>().eq(Category::getName, categoryName));
        if (category == null) {
            return "分类「" + categoryName + "」不存在。请用此工具（分类名留空）查看所有可选分类。";
        }

        List<Recipe> recipes = recipeMapper.selectList(
                new LambdaQueryWrapper<Recipe>().eq(Recipe::getCategoryId, category.getId()));

        if (recipes.isEmpty()) {
            return "分类「" + categoryName + "」下暂无菜品。";
        }

        return recipes.stream()
                .map(r -> {
                    String summary = r.getSummary() != null && !r.getSummary().isBlank()
                            ? " — " + r.getSummary() : "";
                    return String.format("- %s%s", r.getName(), summary);
                })
                .collect(Collectors.joining("\n",
                        String.format("【%s】共 %d 道菜品：\n", categoryName, recipes.size()), ""));
    }
}
