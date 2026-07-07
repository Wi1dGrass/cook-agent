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
            你是烹饪领域的查询优化专家。请将用户的自然语言问题重写为更适合向量检索的查询语句。

            重写原则：
            1. 提取核心要素：菜名、食材、烹饪技法、口味、菜系
            2. 转为关键词组合（用空格分隔），去掉口语化表达和疑问词
            3. 保留原问题核心意图，不引入用户没提到的内容
            4. 多意图问题取主要意图即可，不要合并成模糊查询
            5. 只输出重写后的查询，不要解释、不要加引号

            示例：
            原问题：怎么炖鸡汤好喝？
            重写：炖鸡汤 菜谱 制作方法 调料技巧

            原问题：冰箱里有鸡腿和土豆能做啥
            重写：鸡腿 土豆 菜谱 做法

            原问题：夏天想喝点清淡的汤
            重写：清淡 汤品 夏季 清热 解暑

            原问题：红烧肉和东坡肉有啥区别
            重写：红烧肉 东坡肉 做法 配料 区别

            原问题：{query}
            重写：""";

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
