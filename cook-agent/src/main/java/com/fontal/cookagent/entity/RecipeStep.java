package com.fontal.cookagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("recipe_step")
public class RecipeStep {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 菜品ID */
    private Long recipeId;

    /** 步骤序号 */
    private Integer stepOrder;

    /** 步骤描述 */
    private String description;
}
