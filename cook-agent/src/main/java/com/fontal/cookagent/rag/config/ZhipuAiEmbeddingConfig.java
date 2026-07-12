package com.fontal.cookagent.rag.config;

import com.fontal.cookagent.rag.embedding.ZhipuAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;
import org.springframework.ai.embedding.EmbeddingModel;

@Configuration
public class ZhipuAiEmbeddingConfig {

    @Bean
    @Primary
    public EmbeddingModel zhipuAiEmbeddingModel(
            @Value("${cook.rag.zhipu.api-key}") String apiKey,
            @Value("${cook.rag.zhipu.embedding-model:embedding-3}") String model,
            @Value("${cook.rag.zhipu.dimensions:1024}") Integer dimensions) {
        return new ZhipuAiEmbeddingModel(apiKey, model, dimensions);
    }
}
