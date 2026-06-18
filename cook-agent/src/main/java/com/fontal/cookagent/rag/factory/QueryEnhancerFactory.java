package com.fontal.cookagent.rag.factory;

import com.fontal.cookagent.rag.enhancer.CookContextQueryEnhancer;
import com.fontal.cookagent.rag.enhancer.QueryEnhancer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 查询增强器工厂 — 按领域名称创建/获取增强器 */
public class QueryEnhancerFactory {

    private static final Map<String, QueryEnhancer> CACHE = new ConcurrentHashMap<>();

    static {
        CACHE.put("cooking", new CookContextQueryEnhancer());
    }

    /** 获取指定领域的增强器，默认为 cooking */
    public static QueryEnhancer getEnhancer(String domain) {
        return CACHE.computeIfAbsent(domain != null ? domain : "cooking", k -> new CookContextQueryEnhancer());
    }

    /** 获取默认烹饪增强器 */
    public static QueryEnhancer getEnhancer() {
        return getEnhancer("cooking");
    }
}
