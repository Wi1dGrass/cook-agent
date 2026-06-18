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

import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 每日推荐 Tool — 根据季节、口味偏好智能推荐菜品组合。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyRecommendTool {

    private final RecipeMapper recipeMapper;
    private final CategoryMapper categoryMapper;

    @Tool(description = "每日智能推荐菜品组合。根据当前季节、用户口味偏好，推荐一荤一素一汤的搭配。")
    public String dailyRecommend(
            @ToolParam(description = "口味偏好，例如'清淡'、'重口'、'微辣'。留空则按季节推荐") String preference,
            @ToolParam(description = "推荐组合数量，默认1组（每组含一荤一素一汤）") Integer comboCount) {

        int combos = comboCount != null ? Math.min(comboCount, 3) : 1;
        Month currentMonth = LocalDate.now().getMonth();
        String season = getSeason(currentMonth);
        String dietAdvice = getDietAdvice(season, preference);

        // 缓存分类
        List<Category> categories = categoryMapper.selectList(null);
        Map<String, Long> categoryMap = categories.stream()
                .collect(Collectors.toMap(Category::getName, Category::getId, (a, b) -> a));

        // 候选荤菜分类
        List<String> meatCategories = List.of("炒菜", "卤菜", "炖菜", "烤类");
        // 候选素菜分类
        List<String> veggieCategories = List.of("凉拌", "蒸菜", "烫菜");
        // 汤类
        List<String> soupCategories = List.of("汤");

        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("【每日推荐】季节：%s%s\n\n", season,
                preference != null && !preference.isBlank() ? "｜口味：" + preference : ""));
        sb.append("饮食建议：").append(dietAdvice).append("\n\n");

        for (int i = 0; i < combos; i++) {
            sb.append("━ 推荐组合 ").append(i + 1).append(" ━\n");

            Recipe meat = pickRandomRecipe(meatCategories, categoryMap);
            Recipe veggie = pickRandomRecipe(veggieCategories, categoryMap);
            Recipe soup = pickRandomRecipe(soupCategories, categoryMap);

            if (meat != null) {
                sb.append(String.format("  荤菜：%s%s\n", meat.getName(),
                        meat.getSummary() != null ? " — " + meat.getSummary() : ""));
            }
            if (veggie != null) {
                sb.append(String.format("  素菜：%s%s\n", veggie.getName(),
                        veggie.getSummary() != null ? " — " + veggie.getSummary() : ""));
            }
            if (soup != null) {
                sb.append(String.format("  汤品：%s%s\n", soup.getName(),
                        soup.getSummary() != null ? " — " + soup.getSummary() : ""));
            }
            if (meat == null && veggie == null && soup == null) {
                sb.append("  暂无足够菜品生成推荐组合\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private Recipe pickRandomRecipe(List<String> categoryNames, Map<String, Long> categoryMap) {
        List<Long> catIds = categoryNames.stream()
                .map(categoryMap::get)
                .filter(Objects::nonNull)
                .toList();
        if (catIds.isEmpty()) return null;

        List<Recipe> candidates = recipeMapper.selectList(
                new LambdaQueryWrapper<Recipe>().in(Recipe::getCategoryId, catIds));
        if (candidates.isEmpty()) return null;

        return candidates.get(new Random().nextInt(candidates.size()));
    }

    private String getSeason(Month month) {
        return switch (month) {
            case MARCH, APRIL, MAY -> "春季";
            case JUNE, JULY, AUGUST -> "夏季";
            case SEPTEMBER, OCTOBER, NOVEMBER -> "秋季";
            case DECEMBER, JANUARY, FEBRUARY -> "冬季";
        };
    }

    private String getDietAdvice(String season, String preference) {
        String base = switch (season) {
            case "春季" -> "春季宜养肝，建议多吃时令蔬菜、清淡为主";
            case "夏季" -> "夏季宜清热解暑，建议多吃瓜果、汤水、凉拌菜";
            case "秋季" -> "秋季宜润燥，建议多吃温润食材、少辛辣";
            case "冬季" -> "冬季宜进补，建议多吃温热炖菜、汤品暖身";
            default -> "均衡饮食";
        };
        if (preference != null && !preference.isBlank()) {
            base += "；本次按「" + preference + "」口味筛选";
        }
        return base + "。";
    }
}
