package com.fontal.cookagent.rag.enhancer;

import java.util.Map;

/** 烹饪领域查询增强器 — 注入厨师角色和分类上下文 */
public class CookContextQueryEnhancer implements QueryEnhancer {

    @Override
    public String enhance(String query, Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是老香鸡餐厅的厨师助手，以下是一个关于烹饪的问题：");
        sb.append(query);

        if (context != null) {
            Object category = context.get("category");
            if (category != null) {
                sb.append(" 请重点关注").append(category).append("类菜品。");
            }

            Object ingredient = context.get("ingredient");
            if (ingredient != null) {
                sb.append(" 请优先匹配含").append(ingredient).append("的菜品。");
            }
        }

        sb.append(" 请从菜谱库中检索最匹配的菜品并给出准确回答。");
        return sb.toString();
    }
}
