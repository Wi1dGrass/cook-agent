package com.fontal.cookagent.rag.config;

import com.fontal.cookagent.rag.etl.ConcurrentSummaryEnricher;
import com.fontal.cookagent.rag.properties.RagProperties;
import com.fontal.cookagent.rag.query.CookRewriteQueryTransformer;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagAutoConfiguration {

    /** Token 文本切分器 — 中文标点语义切分 */
    @Bean
    public TokenTextSplitter tokenTextSplitter(RagProperties ragProperties) {
        RagProperties.TokenSplitter cfg = ragProperties.getTokenSplitter();
        return new TokenTextSplitter(
                cfg.getChunkSize(),
                cfg.getMinChunkSizeChars(),
                cfg.getMinChunkLengthToEmbed(),
                cfg.getMaxNumChunks(),
                cfg.isKeepSeparator());
    }

    /** 并发 AI 摘要增强器 — 线程池并行为每个 chunk 生成摘要 */
    @Bean
    public ConcurrentSummaryEnricher summaryMetadataEnricher(
            ChatModel chatModel,
            @Value("${cook.rag.etl.enrich-concurrency:16}") int concurrency) {
        return new ConcurrentSummaryEnricher(chatModel, concurrency);
    }

    /** 查询重写器 */
    @Bean
    public CookRewriteQueryTransformer rewriteQueryTransformer(ChatModel chatModel) {
        return new CookRewriteQueryTransformer(chatModel);
    }

    /** RAG Advisor — 检索增强顾问，允许空上下文时 AI 用自身知识作答 */
    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(
            VectorStore vectorStore, RagProperties ragProperties) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(ragProperties.getRetrieval().getSimilarityThreshold())
                        .topK(ragProperties.getRetrieval().getTopK())
                        .vectorStore(vectorStore)
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();
    }
}
