package com.fontal.cookagent.rag.enhancer;

import java.util.Map;

/** 查询增强接口 — 在检索前对用户查询注入领域上下文 */
@FunctionalInterface
public interface QueryEnhancer {

    /**
     * 增强查询。
     * @param query   原始用户查询
     * @param context 上下文参数（如分类、食材等）
     * @return 增强后的查询字符串
     */
    String enhance(String query, Map<String, Object> context);
}
