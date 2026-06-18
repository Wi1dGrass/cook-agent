package com.fontal.cookagent.rag.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

/**
 * 双数据源配置（pgvector profile）：
 * - MySQL @Primary → MyBatis-Plus 业务数据
 * - PostgreSQL → PgVectorStore 向量检索
 *
 * 每个 @Bean 方法的参数都使用 @Qualifier 指定 bean 名称，
 * 因为 @Primary 会使参数名匹配机制失效。
 */
@Configuration
@Profile("pgvector")
public class PgVectorConfig {

    // ===== PostgreSQL（不暴露 JdbcTemplate 为 bean，避免干扰 JdbcTemplateAutoConfiguration）=====

    @Bean
    @ConfigurationProperties("spring.ai.vectorstore.pgvector.datasource")
    public DataSourceProperties pgDsProps() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource pgDataSource(@Qualifier("pgDsProps") DataSourceProperties dsProps) {
        return dsProps.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel,
                                   @Qualifier("pgDataSource") DataSource pgDataSource) {
        JdbcTemplate jt = new JdbcTemplate(pgDataSource);
        return PgVectorStore.builder(jt, embeddingModel)
                .dimensions(1024)
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW)
                .initializeSchema(false)
                .schemaName("public")
                .vectorTableName("vector_store")
                .build();
    }

    // ===== MySQL @Primary（由 JdbcTemplateAutoConfiguration 自动创建 JdbcTemplate）=====

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties mysqlDsProps() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource mysqlDataSource(@Qualifier("mysqlDsProps") DataSourceProperties dsProps) {
        return dsProps.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }
}
