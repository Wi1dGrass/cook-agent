package com.fontal.cookagent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DeepSeek + JDBC 聊天记忆配置。
 *
 * ChatMemoryRepository 由 Spring AI JdbcChatMemoryRepositoryAutoConfiguration 自动配置，
 * 数据源使用当前活跃 profile 对应的 DataSource（local → MySQL, pgvector → PostgreSQL）。
 * Schema 初始化由 spring.ai.chat.memory.repository.jdbc.initialize-schema=always 控制。
 */
@Configuration
@EnableConfigurationProperties(ChatMemoryProperties.class)
public class DeepSeekConfig {

    /**
     * 滑动窗口记忆（maxMessages=20），底层由 JDBC 自动配置的 ChatMemoryRepository 持久化。
     */
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder().temperature(0.0).build())
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }
}
