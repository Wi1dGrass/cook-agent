package com.fontal.cookagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fontal.cookagent.common.BizException;
import com.fontal.cookagent.common.ErrorCode;
import com.fontal.cookagent.dto.CategoryResponse;
import com.fontal.cookagent.dto.CreateRecipeRequest;
import com.fontal.cookagent.dto.PageResult;
import com.fontal.cookagent.dto.RecipeDetailResponse;
import com.fontal.cookagent.dto.RecipeSummaryResponse;
import com.fontal.cookagent.dto.UpdateRecipeRequest;
import com.fontal.cookagent.entity.*;
import com.fontal.cookagent.mapper.*;
import com.fontal.cookagent.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜谱业务服务 — 整合结构化查询和向量检索，为 RecipeController 提供数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeService {

    private final VectorStore vectorStore;
    private final RecipeMapper recipeMapper;
    private final CategoryMapper categoryMapper;
    private final IngredientMapper ingredientMapper;
    private final RecipeStepMapper recipeStepMapper;
    private final FavoriteService favoriteService;

    // ==================== 搜索与推荐 ====================

    /** 向量语义搜索菜谱 */
    public List<RecipeSummaryResponse> search(String keyword, Integer topK, String category) {
        int k = topK != null ? topK : 5;
        var request = SearchRequest.builder()
                .query(keyword)
                .topK(k)
                .similarityThreshold(0.3)
                .build();

        List<Document> results = vectorStore.similaritySearch(request);
        if (results.isEmpty()) {
            return List.of();
        }

        return results.stream()
                .map(doc -> {
                    Long recipeId = doc.getMetadata().get("recipe_id") instanceof Number n
                            ? n.longValue() : null;
                    if (recipeId == null) return null;
                    Recipe recipe = recipeMapper.selectById(recipeId);
                    return recipe != null ? RecipeSummaryResponse.from(recipe) : null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /** 向量推荐菜谱 */
    public List<RecipeSummaryResponse> recommend(String criteria, Integer count, String category) {
        int topK = count != null ? count * 2 : 6;
        var request = SearchRequest.builder()
                .query(criteria)
                .topK(topK)
                .similarityThreshold(0.2)
                .build();

        List<Document> results = vectorStore.similaritySearch(request);

        Set<Long> seenIds = new LinkedHashSet<>();
        return results.stream()
                .filter(doc -> {
                    Long recipeId = doc.getMetadata().get("recipe_id") instanceof Number n
                            ? n.longValue() : null;
                    return recipeId != null && seenIds.add(recipeId);
                })
                .limit(count != null ? count : 3)
                .map(doc -> {
                    Long recipeId = doc.getMetadata().get("recipe_id") instanceof Number n
                            ? n.longValue() : null;
                    Recipe recipe = recipeMapper.selectById(recipeId);
                    return recipe != null ? RecipeSummaryResponse.from(recipe) : null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    // ==================== 菜谱详情 ====================

    /** 获取菜谱基本信息 */
    public RecipeSummaryResponse getById(Long id) {
        Recipe recipe = recipeMapper.selectById(id);
        return recipe != null ? RecipeSummaryResponse.from(recipe) : null;
    }

    /** 获取菜谱完整详情（含配料、步骤） */
    public RecipeDetailResponse getDetail(Long id) {
        Recipe recipe = recipeMapper.selectById(id);
        if (recipe == null) return null;

        List<Ingredient> ingredients = ingredientMapper.selectList(
                new LambdaQueryWrapper<Ingredient>().eq(Ingredient::getRecipeId, id)
                        .orderByAsc(Ingredient::getSortOrder));
        List<RecipeStep> steps = recipeStepMapper.selectList(
                new LambdaQueryWrapper<RecipeStep>().eq(RecipeStep::getRecipeId, id)
                        .orderByAsc(RecipeStep::getStepOrder));

        boolean favorited = false;
        try {
            Long userId = CurrentUser.requireUserId();
            favorited = favoriteService.isFavorited(userId, id);
        } catch (IllegalStateException ignored) {
            // 未登录用户，favorited 保持 false
        }

        return RecipeDetailResponse.from(recipe, ingredients, steps, favorited);
    }

    /** 按名称模糊搜索菜谱 */
    public List<RecipeSummaryResponse> findByName(String name) {
        List<Recipe> recipes = recipeMapper.selectList(
                new LambdaQueryWrapper<Recipe>().like(Recipe::getName, name));
        return recipes.stream().map(RecipeSummaryResponse::from).toList();
    }

    // ==================== 分页查询 ====================

    /** 分页查询菜谱（可选分类筛选、关键词模糊匹配） */
    public PageResult<RecipeSummaryResponse> page(int pageNum, int pageSize, Long categoryId, String keyword) {
        Page<Recipe> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Recipe> wrapper = new LambdaQueryWrapper<Recipe>()
                .orderByDesc(Recipe::getCreatedAt);
        if (categoryId != null) {
            wrapper.eq(Recipe::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Recipe::getName, keyword);
        }
        IPage<Recipe> result = recipeMapper.selectPage(page, wrapper);
        List<RecipeSummaryResponse> records = result.getRecords().stream()
                .map(RecipeSummaryResponse::from).toList();
        return PageResult.from(result, records);
    }

    // ==================== 新增 / 修改 / 删除 ====================

    /** 新增菜谱（含配料、步骤） */
    @Transactional
    public RecipeDetailResponse create(CreateRecipeRequest request) {
        // 校验分类是否有效
        Category category = categoryMapper.selectById(request.categoryId());
        if (category == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "分类ID " + request.categoryId() + " 不存在");
        }
        // 校验菜名唯一性
        Long existCount = recipeMapper.selectCount(
                new LambdaQueryWrapper<Recipe>().eq(Recipe::getName, request.name()));
        if (existCount > 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "菜品名称「" + request.name() + "」已存在");
        }

        Recipe recipe = new Recipe();
        recipe.setName(request.name());
        recipe.setCategoryId(request.categoryId());
        recipe.setAlias(request.alias());
        recipe.setImageUrl(request.imageUrl());
        recipe.setSummary(request.summary());
        recipe.setRemark(request.remark());
        recipe.setNutritionJson(request.nutritionJson());
        recipeMapper.insert(recipe);

        List<Ingredient> ingredients = saveIngredients(recipe.getId(), request.ingredients());
        List<RecipeStep> steps = saveSteps(recipe.getId(), request.steps());

        log.info("新增菜谱: id={}, name={}", recipe.getId(), recipe.getName());
        return RecipeDetailResponse.from(recipe, ingredients, steps);
    }

    /** 修改菜谱（含配料、步骤，字段为 null 表示不更新） */
    @Transactional
    public RecipeDetailResponse update(Long id, UpdateRecipeRequest request) {
        Recipe recipe = recipeMapper.selectById(id);
        if (recipe == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "菜品 ID " + id + " 不存在");
        }

        if (request.name() != null) recipe.setName(request.name());
        if (request.categoryId() != null) {
            Category category = categoryMapper.selectById(request.categoryId());
            if (category == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "分类ID " + request.categoryId() + " 不存在");
            }
            recipe.setCategoryId(request.categoryId());
        }
        if (request.alias() != null) recipe.setAlias(request.alias());
        if (request.imageUrl() != null) recipe.setImageUrl(request.imageUrl());
        if (request.summary() != null) recipe.setSummary(request.summary());
        if (request.remark() != null) recipe.setRemark(request.remark());
        if (request.nutritionJson() != null) recipe.setNutritionJson(request.nutritionJson());
        recipeMapper.updateById(recipe);

        List<Ingredient> ingredients;
        if (request.ingredients() != null) {
            // 全量覆盖：删除旧配料 → 插入新配料
            ingredientMapper.delete(
                    new LambdaQueryWrapper<Ingredient>().eq(Ingredient::getRecipeId, id));
            ingredients = saveIngredients(id, request.ingredients());
        } else {
            ingredients = ingredientMapper.selectList(
                    new LambdaQueryWrapper<Ingredient>().eq(Ingredient::getRecipeId, id)
                            .orderByAsc(Ingredient::getSortOrder));
        }

        List<RecipeStep> steps;
        if (request.steps() != null) {
            recipeStepMapper.delete(
                    new LambdaQueryWrapper<RecipeStep>().eq(RecipeStep::getRecipeId, id));
            steps = saveSteps(id, request.steps());
        } else {
            steps = recipeStepMapper.selectList(
                    new LambdaQueryWrapper<RecipeStep>().eq(RecipeStep::getRecipeId, id)
                            .orderByAsc(RecipeStep::getStepOrder));
        }

        log.info("修改菜谱: id={}", id);
        return RecipeDetailResponse.from(recipe, ingredients, steps);
    }

    /** 删除菜谱（级联删除配料、步骤） */
    @Transactional
    public void delete(Long id) {
        Recipe recipe = recipeMapper.selectById(id);
        if (recipe == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "菜品 ID " + id + " 不存在");
        }
        ingredientMapper.delete(
                new LambdaQueryWrapper<Ingredient>().eq(Ingredient::getRecipeId, id));
        recipeStepMapper.delete(
                new LambdaQueryWrapper<RecipeStep>().eq(RecipeStep::getRecipeId, id));
        recipeMapper.deleteById(id);
        log.info("删除菜谱: id={}, name={}", id, recipe.getName());
    }

    private List<Ingredient> saveIngredients(Long recipeId, List<CreateRecipeRequest.IngredientItem> items) {
        if (items == null) return List.of();
        List<Ingredient> ingredients = new ArrayList<>();
        for (CreateRecipeRequest.IngredientItem item : items) {
            Ingredient ing = new Ingredient();
            ing.setRecipeId(recipeId);
            ing.setName(item.name());
            ing.setBrand(item.brand());
            ing.setQuantity(item.quantity());
            ing.setNote(item.note());
            ing.setSortOrder(item.sortOrder());
            ingredientMapper.insert(ing);
            ingredients.add(ing);
        }
        return ingredients;
    }

    private List<RecipeStep> saveSteps(Long recipeId, List<CreateRecipeRequest.StepItem> items) {
        if (items == null) return List.of();
        List<RecipeStep> steps = new ArrayList<>();
        for (CreateRecipeRequest.StepItem item : items) {
            RecipeStep step = new RecipeStep();
            step.setRecipeId(recipeId);
            step.setStepOrder(item.stepOrder());
            step.setDescription(item.description());
            recipeStepMapper.insert(step);
            steps.add(step);
        }
        return steps;
    }

    // ==================== 分类 ====================

    /** 列出所有分类（含菜品数量） */
    public List<CategoryResponse> listCategories() {
        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSortOrder));
        return categories.stream()
                .map(c -> {
                    long count = recipeMapper.selectCount(
                            new LambdaQueryWrapper<Recipe>().eq(Recipe::getCategoryId, c.getId()));
                    return CategoryResponse.from(c, count);
                })
                .toList();
    }

    /** 列出某分类下的所有菜品 */
    public List<RecipeSummaryResponse> listRecipesByCategory(Long categoryId) {
        List<Recipe> recipes = recipeMapper.selectList(
                new LambdaQueryWrapper<Recipe>().eq(Recipe::getCategoryId, categoryId));
        return recipes.stream().map(RecipeSummaryResponse::from).toList();
    }

    /** 按分类名称查找分类 */
    public Category findCategoryByName(String name) {
        return categoryMapper.selectOne(
                new LambdaQueryWrapper<Category>().eq(Category::getName, name));
    }

    /** 按分类ID查找分类 */
    public Category findCategoryById(Long id) {
        return categoryMapper.selectById(id);
    }

    // ==================== 食材反查 ====================

    public record IngredientMatchResult(RecipeSummaryResponse recipe, List<String> matchedIngredients) {
    }

    /** 根据食材反查菜品 */
    public List<IngredientMatchResult> searchByIngredients(List<String> ingredientNames, boolean matchAll) {
        if (ingredientNames.isEmpty()) return List.of();

        Map<Long, Set<String>> recipeIngredientMap = new HashMap<>();
        for (String name : ingredientNames) {
            List<Ingredient> matched = ingredientMapper.selectList(
                    new LambdaQueryWrapper<Ingredient>().like(Ingredient::getName, name));
            for (Ingredient ing : matched) {
                recipeIngredientMap
                        .computeIfAbsent(ing.getRecipeId(), k -> new HashSet<>())
                        .add(name);
            }
        }

        if (recipeIngredientMap.isEmpty()) return List.of();

        List<Map.Entry<Long, Set<String>>> entries;
        if (matchAll) {
            entries = recipeIngredientMap.entrySet().stream()
                    .filter(e -> e.getValue().containsAll(ingredientNames))
                    .toList();
        } else {
            entries = new ArrayList<>(recipeIngredientMap.entrySet());
            entries.sort((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()));
        }

        List<Long> recipeIds = entries.stream().limit(10).map(Map.Entry::getKey).toList();
        Map<Long, Recipe> recipeMap = recipeMapper.selectBatchIds(recipeIds).stream()
                .collect(Collectors.toMap(Recipe::getId, r -> r));

        List<IngredientMatchResult> results = new ArrayList<>();
        for (Map.Entry<Long, Set<String>> entry : entries.stream().limit(10).toList()) {
            Recipe recipe = recipeMap.get(entry.getKey());
            if (recipe == null) continue;
            results.add(new IngredientMatchResult(
                    RecipeSummaryResponse.from(recipe),
                    List.copyOf(entry.getValue())
            ));
        }
        return results;
    }

    // ==================== 菜谱对比 ====================

    public record CompareResult(
            List<RecipeSummaryResponse> recipes,
            List<IngredientCompareItem> commonIngredients,
            Map<String, List<String>> uniqueIngredients
    ) {
    }

    public record IngredientCompareItem(String name, Map<String, String> quantities) {
    }

    /** 对比多个菜品的配方差异 */
    public CompareResult compareRecipes(List<String> names) {
        List<Recipe> recipes = recipeMapper.selectList(
                new LambdaQueryWrapper<Recipe>().in(Recipe::getName, names));

        Map<String, Map<String, Ingredient>> recipeIngredientMaps = new LinkedHashMap<>();
        for (Recipe recipe : recipes) {
            List<Ingredient> ings = ingredientMapper.selectList(
                    new LambdaQueryWrapper<Ingredient>().eq(Ingredient::getRecipeId, recipe.getId()));
            recipeIngredientMaps.put(recipe.getName(),
                    ings.stream().collect(Collectors.toMap(Ingredient::getName, i -> i, (a, b) -> a)));
        }

        Set<String> allIngredients = new TreeSet<>();
        recipeIngredientMaps.values().forEach(m -> allIngredients.addAll(m.keySet()));

        // 共同原料
        List<IngredientCompareItem> common = allIngredients.stream()
                .filter(name -> recipes.stream().allMatch(r -> recipeIngredientMaps.get(r.getName()).containsKey(name)))
                .map(ingName -> {
                    Map<String, String> qty = new LinkedHashMap<>();
                    for (Recipe r : recipes) {
                        Ingredient i = recipeIngredientMaps.get(r.getName()).get(ingName);
                        qty.put(r.getName(), i.getQuantity() != null ? i.getQuantity() : "—");
                    }
                    return new IngredientCompareItem(ingName, qty);
                })
                .toList();

        // 各自独有原料
        Map<String, List<String>> unique = new LinkedHashMap<>();
        for (Recipe recipe : recipes) {
            Set<String> others = recipes.stream()
                    .filter(r -> !r.getName().equals(recipe.getName()))
                    .flatMap(r -> recipeIngredientMaps.get(r.getName()).keySet().stream())
                    .collect(Collectors.toSet());
            List<String> uniq = recipeIngredientMaps.get(recipe.getName()).keySet().stream()
                    .filter(name -> !others.contains(name))
                    .toList();
            unique.put(recipe.getName(), uniq);
        }

        return new CompareResult(
                recipes.stream().map(RecipeSummaryResponse::from).toList(),
                common,
                unique
        );
    }

    // ==================== 每日推荐 ====================

    public record DailyRecommendResult(String season, String dietAdvice, List<RecommendCombo> combos) {
    }

    public record RecommendCombo(RecipeSummaryResponse meat, RecipeSummaryResponse veggie, RecipeSummaryResponse soup) {
    }

    /** 每日智能推荐 */
    public DailyRecommendResult dailyRecommend(String preference, Integer comboCount) {
        int combos = comboCount != null ? Math.min(comboCount, 3) : 1;
        Month currentMonth = LocalDate.now().getMonth();
        String season = getSeason(currentMonth);
        String dietAdvice = getDietAdvice(season, preference);

        Map<String, Long> categoryMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getName, Category::getId, (a, b) -> a));

        List<String> meatCategories = List.of("炒菜", "卤菜", "炖菜", "烤类");
        List<String> veggieCategories = List.of("凉拌", "蒸菜", "烫菜");
        List<String> soupCategories = List.of("汤");

        List<RecommendCombo> combosList = new ArrayList<>();
        for (int i = 0; i < combos; i++) {
            RecipeSummaryResponse meat = pickRandomRecipe(meatCategories, categoryMap);
            RecipeSummaryResponse veggie = pickRandomRecipe(veggieCategories, categoryMap);
            RecipeSummaryResponse soup = pickRandomRecipe(soupCategories, categoryMap);
            combosList.add(new RecommendCombo(meat, veggie, soup));
        }

        return new DailyRecommendResult(season, dietAdvice, combosList);
    }

    // ==================== 营养成分 ====================

    /** 查询菜品营养成分 */
    public RecipeDetailResponse getNutrition(String recipeName) {
        List<Recipe> recipes = recipeMapper.selectList(
                new LambdaQueryWrapper<Recipe>().like(Recipe::getName, recipeName));
        if (recipes.isEmpty()) return null;

        Recipe recipe = recipes.get(0);
        List<Ingredient> ingredients = ingredientMapper.selectList(
                new LambdaQueryWrapper<Ingredient>().eq(Ingredient::getRecipeId, recipe.getId()));
        List<RecipeStep> steps = recipeStepMapper.selectList(
                new LambdaQueryWrapper<RecipeStep>().eq(RecipeStep::getRecipeId, recipe.getId()));

        return RecipeDetailResponse.from(recipe, ingredients, steps);
    }

    // ==================== 辅助方法 ====================

    private RecipeSummaryResponse pickRandomRecipe(List<String> categoryNames, Map<String, Long> categoryMap) {
        List<Long> catIds = categoryNames.stream()
                .map(categoryMap::get)
                .filter(Objects::nonNull)
                .toList();
        if (catIds.isEmpty()) return null;

        List<Recipe> candidates = recipeMapper.selectList(
                new LambdaQueryWrapper<Recipe>().in(Recipe::getCategoryId, catIds));
        if (candidates.isEmpty()) return null;

        return RecipeSummaryResponse.from(candidates.get(new Random().nextInt(candidates.size())));
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
