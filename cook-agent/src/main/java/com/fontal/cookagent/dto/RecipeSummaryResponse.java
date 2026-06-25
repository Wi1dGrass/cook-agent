package com.fontal.cookagent.dto;

import com.fontal.cookagent.entity.Recipe;

import java.time.LocalDateTime;

/**
 * 菜谱摘要响应 — 用于列表/搜索结果。
 */
public record RecipeSummaryResponse(
        Long id,
        String name,
        Long categoryId,
        String alias,
        String imageUrl,
        String summary,
        LocalDateTime createdAt
) {
    public static RecipeSummaryResponse from(Recipe recipe) {
        return new RecipeSummaryResponse(
                recipe.getId(),
                recipe.getName(),
                recipe.getCategoryId(),
                recipe.getAlias(),
                recipe.getImageUrl(),
                recipe.getSummary(),
                recipe.getCreatedAt()
        );
    }
}
