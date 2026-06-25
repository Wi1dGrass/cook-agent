package com.fontal.cookagent.controller;

import com.fontal.cookagent.common.BizException;
import com.fontal.cookagent.common.ErrorCode;
import com.fontal.cookagent.config.RateLimit;
import com.fontal.cookagent.dto.CategoryResponse;
import com.fontal.cookagent.dto.CreateRecipeRequest;
import com.fontal.cookagent.dto.PageResult;
import com.fontal.cookagent.dto.RecipeDetailResponse;
import com.fontal.cookagent.dto.RecipeSummaryResponse;
import com.fontal.cookagent.dto.UpdateRecipeRequest;
import com.fontal.cookagent.entity.Category;
import com.fontal.cookagent.service.RecipeService;
import com.fontal.cookagent.service.RecipeService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 菜谱 REST 控制器 — 菜谱 CRUD、搜索、分类浏览、食材反查、每日推荐等。
 */
@Tag(name = "菜谱管理", description = "菜谱 CRUD / 向量搜索 / 分类浏览 / 食材反查 / 每日推荐 / 对比 / 营养")
@RestController
@RequestMapping
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    // ==================== 分页查询 ====================

    @Operation(summary = "分页查询菜谱列表", description = "支持按分类筛选和关键词模糊搜索")
    @GetMapping("/recipes/page")
    public PageResult<RecipeSummaryResponse> page(
            @Parameter(description = "页码，从1开始", example = "1") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数", example = "10") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "分类ID，为空则不筛选") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "关键词，为空则不筛选") @RequestParam(required = false) String keyword) {
        return recipeService.page(pageNum, pageSize, categoryId, keyword);
    }

    // ==================== 搜索与推荐 ====================

    @Operation(summary = "向量语义搜索菜谱", description = "基于 PGVector 语义相似度搜索 TopK 菜谱")
    @GetMapping("/recipes/search")
    @RateLimit(limit = 10)
    public Map<String, Object> search(
            @Parameter(description = "自然语言查询关键词", required = true) @RequestParam String keyword,
            @Parameter(description = "返回数量，默认5", example = "5") @RequestParam(defaultValue = "5") int topK,
            @Parameter(description = "分类名称，为空则不筛选") @RequestParam(required = false) String category) {
        if (keyword.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "keyword 不能为空");
        }
        List<RecipeSummaryResponse> results = recipeService.search(keyword, topK, category);
        return Map.of("count", results.size(), "recipes", results);
    }

    @Operation(summary = "智能推荐菜谱", description = "基于$criteria做向量推荐，菜品去重")
    @GetMapping("/recipes/recommend")
    @RateLimit(limit = 5)
    public Map<String, Object> recommend(
            @Parameter(description = "推荐条件描述", required = true) @RequestParam String criteria,
            @Parameter(description = "返回数量，默认3", example = "3") @RequestParam(defaultValue = "3") int count,
            @Parameter(description = "分类名称，为空则不筛选") @RequestParam(required = false) String category) {
        if (criteria.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "criteria 不能为空");
        }
        List<RecipeSummaryResponse> results = recipeService.recommend(criteria, count, category);
        return Map.of("count", results.size(), "recipes", results);
    }

    // ==================== 菜谱详情 ====================

    @Operation(summary = "根据ID获取菜谱摘要")
    @GetMapping("/recipes/{id}")
    public RecipeSummaryResponse getById(
            @Parameter(description = "菜谱ID", required = true, example = "1") @PathVariable Long id) {
        RecipeSummaryResponse recipe = recipeService.getById(id);
        if (recipe == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "菜品 ID " + id + " 不存在");
        }
        return recipe;
    }

    @Operation(summary = "根据ID获取菜谱完整详情", description = "包含配料、步骤、营养成分的完整信息")
    @GetMapping("/recipes/{id}/detail")
    public RecipeDetailResponse getDetail(
            @Parameter(description = "菜谱ID", required = true, example = "1") @PathVariable Long id) {
        RecipeDetailResponse detail = recipeService.getDetail(id);
        if (detail == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "菜品 ID " + id + " 不存在");
        }
        return detail;
    }

    @Operation(summary = "按名称模糊搜索菜谱")
    @GetMapping("/recipes")
    public Map<String, Object> findByName(
            @Parameter(description = "菜谱名称关键词", required = true) @RequestParam String name) {
        if (name.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "name 不能为空");
        }
        List<RecipeSummaryResponse> results = recipeService.findByName(name);
        return Map.of("count", results.size(), "recipes", results);
    }

    // ==================== 新增 / 修改 / 删除 ====================

    @Operation(summary = "新增菜谱", description = "新增菜品并级联插入配料和步骤，名称需唯一")
    @RateLimit(limit = 5)
    @PostMapping("/recipes")
    public RecipeDetailResponse create(@Valid @RequestBody CreateRecipeRequest request) {
        return recipeService.create(request);
    }

    @Operation(summary = "修改菜谱", description = "全量覆盖配料和步骤（字段为null表示不更新对应字段）")
    @RateLimit(limit = 5)
    @PutMapping("/recipes/{id}")
    public RecipeDetailResponse update(
            @Parameter(description = "菜谱ID", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateRecipeRequest request) {
        return recipeService.update(id, request);
    }

    @Operation(summary = "删除菜谱", description = "级联删除菜品关联的配料和步骤")
    @RateLimit(limit = 5)
    @DeleteMapping("/recipes/{id}")
    public Map<String, Object> delete(
            @Parameter(description = "菜谱ID", required = true, example = "1") @PathVariable Long id) {
        recipeService.delete(id);
        return Map.of("id", id, "deleted", true);
    }

    // ==================== 菜谱对比 ====================

    @Operation(summary = "对比多个菜品的配方差异", description = "给出共同原料和各自独有原料")
    @GetMapping("/recipes/compare")
    @RateLimit(limit = 5)
    public CompareResult compare(
            @Parameter(description = "菜品名称列表，逗号分隔（2-4个）", required = true, example = "红烧肉,口水鸡")
            @RequestParam String names) {
        List<String> nameList = parseCommaList(names);
        if (nameList.size() < 2) {
            throw new BizException(ErrorCode.PARAM_INVALID, "对比至少需要两个菜品，用逗号分隔");
        }
        if (nameList.size() > 4) {
            throw new BizException(ErrorCode.PARAM_INVALID, "最多支持对比 4 个菜品");
        }
        return recipeService.compareRecipes(nameList);
    }

    // ==================== 每日推荐 ====================

    @Operation(summary = "每日智能推荐", description = "根据当令季节和营养搭配推荐菜品组合")
    @GetMapping("/recipes/daily-recommend")
    public DailyRecommendResult dailyRecommend(
            @Parameter(description = "口味偏好（清淡/重口等），为空则默认") @RequestParam(required = false) String preference,
            @Parameter(description = "组合数（1-3）", example = "1") @RequestParam(defaultValue = "1") int comboCount) {
        return recipeService.dailyRecommend(preference, comboCount);
    }

    // ==================== 营养成分 ====================

    @Operation(summary = "查询菜品营养成分", description = "按菜名模糊查询，返回首匹配的完整详情")
    @GetMapping("/recipes/nutrition")
    public RecipeDetailResponse nutrition(
            @Parameter(description = "菜谱名称关键词", required = true) @RequestParam String name) {
        if (name.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "name 不能为空");
        }
        RecipeDetailResponse result = recipeService.getNutrition(name);
        if (result == null) {
            throw new BizException(ErrorCode.SEARCH_NO_RESULTS, "未找到菜品「" + name + "」");
        }
        return result;
    }

    // ==================== 分类 ====================

    @Operation(summary = "列出全部分类", description = "包含每个分类的菜品数量")
    @GetMapping("/categories")
    public List<CategoryResponse> listCategories() {
        return recipeService.listCategories();
    }

    @Operation(summary = "列出某分类下的所有菜品")
    @GetMapping("/categories/{id}/recipes")
    public Map<String, Object> listRecipesByCategory(
            @Parameter(description = "分类ID", required = true, example = "1") @PathVariable Long id) {
        List<RecipeSummaryResponse> recipes = recipeService.listRecipesByCategory(id);
        Category category = recipeService.findCategoryById(id);
        String categoryName = category != null ? category.getName() : "未知";
        return Map.of(
                "categoryId", id,
                "categoryName", categoryName,
                "count", recipes.size(),
                "recipes", recipes
        );
    }

    // ==================== 食材反查 ====================

    @Operation(summary = "食材反查菜品", description = "按手头食材匹配菜品，mode=any匹配任意一种，mode=all需全部包含")
    @GetMapping("/ingredients/search")
    @RateLimit(limit = 5)
    public Map<String, Object> searchByIngredients(
            @Parameter(description = "食材名称列表（逗号分隔）", required = true, example = "鸡腿,花椒")
            @RequestParam String names,
            @Parameter(description = "匹配模式：any=任一食材，all=全部食材", example = "any")
            @RequestParam(defaultValue = "any") String mode) {
        List<String> ingredientList = parseCommaList(names);
        if (ingredientList.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请提供至少一个食材名称");
        }
        boolean matchAll = "all".equalsIgnoreCase(mode);
        List<IngredientMatchResult> results = recipeService.searchByIngredients(ingredientList, matchAll);
        return Map.of("count", results.size(), "matchMode", mode, "results", results);
    }

    // ==================== 辅助 ====================

    private List<String> parseCommaList(String input) {
        return Arrays.stream(input.split("[,，]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}