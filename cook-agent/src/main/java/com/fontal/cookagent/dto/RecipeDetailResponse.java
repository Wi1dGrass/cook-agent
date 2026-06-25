package com.fontal.cookagent.dto;

import com.fontal.cookagent.entity.Ingredient;
import com.fontal.cookagent.entity.Recipe;
import com.fontal.cookagent.entity.RecipeStep;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜谱详情响应 — 包含配料、步骤、营养成分的完整信息。
 */
public record RecipeDetailResponse(
        Long id,
        String name,
        Long categoryId,
        String alias,
        String imageUrl,
        String summary,
        String remark,
        String nutritionJson,
        String rawMarkdown,
        String sourceFile,
        List<IngredientInfo> ingredients,
        List<StepInfo> steps,
        LocalDateTime createdAt
) {
    public record IngredientInfo(
            Long id,
            String name,
            String brand,
            String quantity,
            String note
    ) {
        public static IngredientInfo from(Ingredient i) {
            return new IngredientInfo(i.getId(), i.getName(), i.getBrand(), i.getQuantity(), i.getNote());
        }
    }

    public record StepInfo(int stepOrder, String description) {
        public static StepInfo from(RecipeStep s) {
            return new StepInfo(s.getStepOrder(), s.getDescription());
        }
    }

    public static RecipeDetailResponse from(Recipe recipe, List<Ingredient> ingredients, List<RecipeStep> steps) {
        return new RecipeDetailResponse(
                recipe.getId(),
                recipe.getName(),
                recipe.getCategoryId(),
                recipe.getAlias(),
                recipe.getImageUrl(),
                recipe.getSummary(),
                recipe.getRemark(),
                recipe.getNutritionJson(),
                recipe.getRawMarkdown(),
                recipe.getSourceFile(),
                ingredients.stream().map(IngredientInfo::from).toList(),
                steps.stream().map(StepInfo::from).toList(),
                recipe.getCreatedAt()
        );
    }
}
