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
 * DeepSeek + MySQL 聊天记忆配置。
 *
 * ChatMemoryRepository 由 {@link com.fontal.cookagent.app.memory.MysqlChatMemoryRepository} 提供，
 * 数据源使用主 MySQL（cook_like_hoc 库），schema 见 sql/schema.sql 中的 chat_memory 表。
 * 已替代原 FileBasedChatMemory（Kryo 文件）方案，统一使用 MySQL 持久化。
 */
@Configuration
@EnableConfigurationProperties({ChatMemoryProperties.class, AgentProperties.class})
public class DeepSeekConfig {

    /**
     * 滑动窗口记忆（maxMessages=20），底层由 MySQL 持久化。
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
