package com.fontal.cookagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("recipe")
public class Recipe {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 菜品名称 */
    private String name;

    /** 分类ID */
    private Long categoryId;

    /** 别名 */
    private String alias;

    /** 图片路径 */
    private String imageUrl;

    /** 简介 */
    private String summary;

    /** 备注 */
    private String remark;

    /** 营养成分 JSON */
    @TableField("nutrition_json")
    private String nutritionJson;

    /** 原始 Markdown */
    private String rawMarkdown;

    /** 源文件路径 */
    private String sourceFile;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
