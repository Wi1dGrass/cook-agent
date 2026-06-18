package com.fontal.cookagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_recommend")
public class DailyRecommend {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 推荐日期 */
    private LocalDate recommendDate;

    /** 推荐菜品ID列表 JSON */
    @TableField("recipe_ids")
    private String recipeIds;

    /** 推荐理由 */
    private String reason;

    private LocalDateTime createdAt;
}
