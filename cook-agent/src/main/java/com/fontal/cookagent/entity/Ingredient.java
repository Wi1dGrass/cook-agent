package com.fontal.cookagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ingredient")
public class Ingredient {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 菜品ID */
    private Long recipeId;

    /** 原料名称 */
    private String name;

    /** 品牌/供应商 */
    private String brand;

    /** 用量 */
    private String quantity;

    /** 备注 */
    private String note;

    /** 排序 */
    private Integer sortOrder;
}
