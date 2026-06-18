package com.fontal.cookagent.rag;

import com.fontal.cookagent.rag.advisor.MyLoggerAdvisor;
import com.fontal.cookagent.rag.advisor.RagDebugAdvisor;
import com.fontal.cookagent.rag.factory.RetrievalAdvisorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RAG 效果测试 — 直接测试 Embedding → 检索 → 增强 → 生成 全链路。
 * 不依赖 Controller 层，直接注入 Spring Bean 验证。
 *
 * 使用 pgvector profile：ZhipuAI Embedding-3 + PGVector 持久化存储。
 * 如果 ETL 已完成导入则直接使用已有数据，否则自动加载测试菜谱。
 */
@SpringBootTest
@ActiveProfiles("pgvector")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RagEffectivenessTest {

    private static final Logger log = LoggerFactory.getLogger(RagEffectivenessTest.class);

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private ChatClient ragChatClient;

    @Autowired
    private RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    @Autowired
    private RetrievalAdvisorFactory advisorFactory;


    // ==================== 一、Embedding 模型测试 ====================

    @Test
    @DisplayName("1. Embedding 单文本向量化")
    void testSingleEmbedding() {
        float[] vector = embeddingModel.embed("鸡汤怎么炖才好喝");
        assertThat(vector).isNotNull();
        assertThat(vector.length).isEqualTo(1024);
        log.info("单文本 Embedding 成功，维度: {}", vector.length);
    }

    @Test
    @DisplayName("2. Embedding 批量向量化")
    void testBatchEmbedding() {
        List<String> texts = List.of("鸡汤怎么炖", "红烧肉的做法", "番茄炒蛋步骤");
        EmbeddingResponse response = embeddingModel.call(new EmbeddingRequest(texts, null));
        assertThat(response.getResults()).hasSize(3);
        assertThat(response.getResults().get(0).getOutput()).hasSize(1024);
        log.info("批量 Embedding 成功，共 {} 条", response.getResults().size());
    }


    // ==================== 二、向量检索测试 ====================

    @Test
    @DisplayName("4. 向量相似检索 — 搜菜谱名称")
    void testSimilaritySearchByRecipeName() {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("怎么炖鸡汤")
                        .topK(3)
                        .similarityThreshold(0.5)
                        .build());
        assertThat(results).isNotEmpty();
        log.info("检索到 {} 条相关菜谱:", results.size());
        for (Document doc : results) {
            log.info("  - {} (score={})", doc.getMetadata().get("recipe_name"), doc.getScore());
        }
    }

    @Test
    @DisplayName("5. 向量相似检索 — 按食材搜")
    void testSimilaritySearchByIngredient() {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("豆腐怎么做")
                        .topK(3)
                        .similarityThreshold(0.5)
                        .build());
        assertThat(results).isNotEmpty();
        log.info("食材搜索 '豆腐' 命中 {} 条, 第一条: {}",
                results.size(), results.get(0).getMetadata().get("recipe_name"));
    }

    // ==================== 三、RAG Advisor 工厂测试 ====================

    @Test
    @DisplayName("6. RAG Advisor 工厂 — 通用菜谱搜索")
    void testAdvisorFactoryRecipeSearch() {
        RetrievalAugmentationAdvisor advisor = advisorFactory.createRecipeSearchAdvisor();
        assertThat(advisor).isNotNull();
        log.info("通用菜谱搜索 Advisor 创建成功");
    }

    @Test
    @DisplayName("7. RAG Advisor 工厂 — 分类筛选")
    void testAdvisorFactoryCategoryFilter() {
        RetrievalAugmentationAdvisor advisor = advisorFactory.createCategoryFilteredAdvisor("汤类");
        assertThat(advisor).isNotNull();
        log.info("分类筛选 Advisor (category=汤类) 创建成功");
    }

    // ==================== 四、RAG 全链路测试 ====================

    @Test
    @DisplayName("8. RAG Chat — 单轮问答（默认 Advisor）")
    void testRagSingleTurnChat() {
        String answer = ragChatClient.prompt()
                .user("我想学炖鸡汤，有什么技巧？")
                .advisors(retrievalAugmentationAdvisor)
                .call()
                .content();

        assertThat(answer).isNotBlank();
        log.info("问题: 我想学炖鸡汤，有什么技巧？");
        log.info("AI 回答: {}", answer);
    }

    @Test
    @DisplayName("9. RAG Chat — 食材查询（高阈值 Advisor）")
    void testRagIngredientQuery() {
        String answer = ragChatClient.prompt()
                .user("家里有排骨，教我做一道菜")
                .advisors(advisorFactory.createIngredientMatchAdvisor())
                .advisors(new RagDebugAdvisor())
                //.advisors(new MyLoggerAdvisor())
                //.advisors(retrievalAugmentationAdvisor)
                .call()
                .content();

        assertThat(answer).isNotBlank();
        log.info("问题: 家里有排骨，教我做一道菜");
        log.info("AI 回答: {}", answer);
    }

    @Test
    @DisplayName("10. RAG Chat — 烹饪建议（低阈值 Advisor）")
    void testRagCookingAdvice() {
        String answer = ragChatClient.prompt()
                .user("炖菜有什么通用的技巧？")
                .advisors(advisorFactory.createCookingAdviceAdvisor())
                //.advisors(new MyLoggerAdvisor())
                .advisors(new RagDebugAdvisor())
                .call()
                .content();

        assertThat(answer).isNotBlank();
        log.info("问题: 炖菜有什么通用的技巧？");
        log.info("AI 回答: {}", answer);
    }

}
