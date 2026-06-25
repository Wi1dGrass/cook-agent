package com.fontal.cookagent.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateRecipeRequest(
        @NotBlank(message = "菜品名称不能为空")
        @Size(max = 128, message = "菜品名称最长128字符")
        String name,

        @NotNull(message = "分类ID不能为空")
        Long categoryId,

        @Size(max = 256, message = "别名最长256字符")
        String alias,

        @Size(max = 512, message = "图片URL最长512字符")
        String imageUrl,

        @Size(max = 512, message = "简介最长512字符")
        String summary,

        String remark,

        String nutritionJson,

        @Valid
        List<IngredientItem> ingredients,

        @Valid
        List<StepItem> steps
) {
    public record IngredientItem(
            @NotBlank(message = "原料名称不能为空")
            String name,
            String brand,
            String quantity,
            String note,
            Integer sortOrder
    ) {}

    public record StepItem(
            @NotNull(message = "步骤序号不能为空")
            Integer stepOrder,
            @NotBlank(message = "步骤描述不能为空")
            String description
    ) {}
}
