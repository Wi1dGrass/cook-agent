package com.fontal.cookagent.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateRecipeRequest(
        @Size(max = 128, message = "菜品名称最长128字符")
        String name,

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
        List<CreateRecipeRequest.IngredientItem> ingredients,

        @Valid
        List<CreateRecipeRequest.StepItem> steps
) {}
