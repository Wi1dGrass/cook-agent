package com.fontal.cookagent.rag.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 向量存储配置。
 *
 * Profile 策略：
 * - pgvector profile：由 Spring AI 的 PgVectorStoreAutoConfiguration 自动创建 PgVectorStore，
 *                     使用 spring.ai.vectorstore.pgvector.datasource.* 配置的独立 PostgreSQL 数据源。
 *                     （此处不手动创建，避免与主 MySQL 数据源冲突）
 * - 其他 profile：使用内存 SimpleVectorStore（开发/测试用）
 */
@Configuration
public class VectorStoreConfig {

    /** 开发环境：内存向量存储（pgvector profile 下由自动配置提供 PgVectorStore） */
    @Bean
    @Profile("!pgvector")
    @ConditionalOnMissingBean(VectorStore.class)
    public VectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
