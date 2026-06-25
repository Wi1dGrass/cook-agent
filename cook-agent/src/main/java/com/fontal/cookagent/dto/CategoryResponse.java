package com.fontal.cookagent.dto;

import com.fontal.cookagent.entity.Category;

/**
 * 分类响应 — 含菜品数量。
 */
public record CategoryResponse(
        Long id,
        String name,
        String dirName,
        Integer sortOrder,
        Long recipeCount
) {
    public static CategoryResponse from(Category category, long recipeCount) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDirName(),
                category.getSortOrder(),
                recipeCount
        );
    }
}
