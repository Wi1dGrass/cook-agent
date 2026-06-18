package com.fontal.cookagent.rag.factory;

import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

/**
 * RAG Advisor 工厂 — 按场景创建不同参数的 RetrievalAugmentationAdvisor。
 *
 * 场景矩阵：
 * - 通用菜谱搜索：   similarityThreshold=0.50
 * - 食材精确匹配：   similarityThreshold=0.75
 * - 烹饪建议/技巧：  similarityThreshold=0.30
 * - 按分类筛选：    similarityThreshold=0.50 + Filter.Expression
 */
@Component
public class RetrievalAdvisorFactory {

    private final VectorStore vectorStore;

    public RetrievalAdvisorFactory(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /** 通用菜谱搜索（默认） */
    public RetrievalAugmentationAdvisor createRecipeSearchAdvisor() {
        return buildAdvisor(0.50, 5);
    }

    /** 食材精确匹配 — 高阈值确保精确匹配 */
    public RetrievalAugmentationAdvisor createIngredientMatchAdvisor() {
        return buildAdvisor(0.50, 5);
    }

    /** 烹饪建议/技巧 — 低阈值捕获相关知识 */
    public RetrievalAugmentationAdvisor createCookingAdviceAdvisor() {

        return buildAdvisor(0.0, 8);
    }

    /** 按分类筛选 — 仅检索指定分类 */
    public RetrievalAugmentationAdvisor createCategoryFilteredAdvisor(String category) {
        Filter.Expression filter = new FilterExpressionBuilder()
                .eq("category_name", category)
                .build();
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(0.50)
                        .topK(5)
                        .vectorStore(vectorStore)
                        .filterExpression(filter)
                        .build())
                .build();
    }

    private RetrievalAugmentationAdvisor buildAdvisor(double threshold, int topK) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(threshold)
                        .topK(topK)
                        .vectorStore(vectorStore)
                        .build())
                .build();
    }
}
