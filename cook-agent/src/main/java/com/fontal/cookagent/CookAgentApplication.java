package com.fontal.cookagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.ai.model.chat.memory.autoconfigure.ChatMemoryAutoConfiguration;
import org.springframework.ai.model.chat.memory.repository.jdbc.autoconfigure.JdbcChatMemoryRepositoryAutoConfiguration;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 排除 Spring AI 自带自动配置：
 * - ChatMemoryAutoConfiguration：会创建 InMemoryChatMemoryRepository + ChatMemory bean，与 FileBasedChatMemory 冲突
 * - JdbcChatMemoryRepositoryAutoConfiguration：双数据源场景下 driver 识别失败，我们使用 Kryo 文件记忆
 * - PgVectorStoreAutoConfiguration：我们使用 PgVectorConfig 手动管理 PostgreSQL 数据源，与主 MySQL 数据源隔离
 */
@SpringBootApplication(exclude = {
        ChatMemoryAutoConfiguration.class,
        JdbcChatMemoryRepositoryAutoConfiguration.class,
        PgVectorStoreAutoConfiguration.class
})
@MapperScan("com.fontal.cookagent.mapper")
public class CookAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CookAgentApplication.class, args);
    }
}
