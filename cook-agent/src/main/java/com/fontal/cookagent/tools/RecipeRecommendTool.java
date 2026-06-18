package com.fontal.cookagent.tools;

import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜谱推荐 Tool — 根据用户偏好推荐菜谱，自动去重保证不重复推荐同一道菜。
 */
@Component
public class RecipeRecommendTool {

    private final VectorStore vectorStore;

    public RecipeRecommendTool(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Tool(description = "根据用户的口味偏好、食材、季节等条件推荐菜谱。结果自动按菜谱名去重。")
    public String recommendRecipes(
            @ToolParam(description = "推荐条件，例如'适合春天的清淡菜'、'用鸡腿能做的菜'、'适合新手的家常菜'", required = true) String criteria,
            @ToolParam(description = "推荐数量，默认3") Integer count,
            @ToolParam(description = "按分类筛选，例如'汤类'。留空则不限分类。") String category) {

        int topK = count != null ? count * 2 : 6; // 多取一些用于去重
        var request = SearchRequest.builder()
                .query(criteria)
                .topK(topK)
                .similarityThreshold(0.4)
                .build();

        List<Document> results = vectorStore.similaritySearch(request);

        // 按 recipe_name 去重
        Set<String> seenNames = new LinkedHashSet<>();
        List<Document> deduped = results.stream()
                .filter(doc -> {
                    String name = (String) doc.getMetadata().get("recipe_name");
                    return name != null && seenNames.add(name);
                })
                .limit(count != null ? count : 3)
                .toList();

        if (deduped.isEmpty()) {
            return "未找到匹配「" + criteria + "」的菜谱推荐。";
        }

        return "根据「" + criteria + "」为您推荐以下菜谱：\n" +
                deduped.stream()
                        .map(doc -> {
                            String name = (String) doc.getMetadata().getOrDefault("recipe_name", "未知");
                            String cat = (String) doc.getMetadata().getOrDefault("category_name", "未知");
                            String content = doc.getText() != null
                                    ? doc.getText().substring(0, Math.min(150, doc.getText().length()))
                                    : "";
                            return String.format("- %s【%s】: %s", name, cat, content);
                        })
                        .collect(Collectors.joining("\n"));
    }
}
