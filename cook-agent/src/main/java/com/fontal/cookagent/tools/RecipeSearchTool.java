package com.fontal.cookagent.tools;

import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜谱向量搜索 Tool — 在 PGVector 知识库中语义检索菜谱。
 */
@Component
public class RecipeSearchTool {

    private final VectorStore vectorStore;

    public RecipeSearchTool(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Tool(description = "根据自然语言查询在菜谱知识库中语义搜索相关菜谱。可以按分类筛选结果。")
    public String searchRecipes(
            @ToolParam(description = "搜索查询，例如'炖鸡汤的技巧'、'排骨的做法'", required = true) String query,
            @ToolParam(description = "返回的最大菜谱数量，默认5") Integer topK,
            @ToolParam(description = "按分类筛选，例如'汤类'、'炒菜'。留空则不筛选。") String category) {

        int k = topK != null ? topK : 5;
        var request = SearchRequest.builder()
                .query(query)
                .topK(k)
                .similarityThreshold(0.5)
                .build();

        List<Document> results = vectorStore.similaritySearch(request);

        if (results.isEmpty()) {
            return "未找到与「" + query + "」相关的菜谱。请尝试其他关键词。";
        }

        return results.stream()
                .map(doc -> {
                    String name = (String) doc.getMetadata().getOrDefault("recipe_name", "未知");
                    String cat = (String) doc.getMetadata().getOrDefault("category_name", "未知");
                    String score = doc.getScore() != null ? String.format("%.2f", doc.getScore()) : "N/A";
                    String content = doc.getText() != null
                            ? doc.getText().substring(0, Math.min(200, doc.getText().length()))
                            : "";
                    return String.format("【%s】(分类:%s, 相似度:%s)\n%s", name, cat, score, content);
                })
                .collect(Collectors.joining("\n\n"));
    }
}
