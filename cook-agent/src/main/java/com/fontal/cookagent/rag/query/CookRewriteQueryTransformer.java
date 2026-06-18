package com.fontal.cookagent.rag.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.Query;

import java.util.Map;

/**
 * 烹饪查询重写器 — 使用 LLM 将自然语言查询优化为检索友好形式。
 * 例如 "怎么炖鸡汤好喝？" → "寻找炖鸡汤的菜谱，关注汤品类下的鸡汤制作方法和调料使用技巧"
 */
public class CookRewriteQueryTransformer {

    private static final Logger log = LoggerFactory.getLogger(CookRewriteQueryTransformer.class);

    private static final String REWRITE_PROMPT = """
            你是一个查询优化专家。请将以下烹饪相关的问题重写为更适合向量检索的查询语句。
            要求：
            1. 提取关键食材、菜名、烹饪技法
            2. 使用简洁的关键词组合
            3. 保留原问题的核心意图
            4. 只输出重写后的查询，不要解释

            原始问题：{query}
            重写查询：""";

    private final ChatModel chatModel;

    public CookRewriteQueryTransformer(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public Query transform(Query query) {
        String original = query.text();
        try {
            PromptTemplate template = new PromptTemplate(REWRITE_PROMPT);
            Prompt prompt = template.create(Map.of("query", original));
            String rewritten = chatModel.call(prompt).getResult().getOutput().getText();
            log.debug("Query rewritten: '{}' -> '{}'", original, rewritten);
            return Query.builder()
                    .text(rewritten.trim())
                    .history(query.history())
                    .context(query.context())
                    .build();
        } catch (Exception e) {
            log.warn("Query rewrite failed, using original query", e);
            return query;
        }
    }
}
