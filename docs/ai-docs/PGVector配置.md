# PGVector 安装与配置指南

---

## 一、概述

PGVector 是 PostgreSQL 的向量扩展，允许在 PostgreSQL 中存储和检索向量嵌入（embedding）。Spring AI 通过 `spring-ai-starter-vector-store-pgvector` 提供开箱即用的支持。

### 为什么选 PGVector？

| 对比维度 | PGVector | Milvus/Qdrant | 内存 SimpleVectorStore |
|---------|----------|---------------|----------------------|
| 部署复杂度 | 低（PostgreSQL 扩展） | 高（独立服务） | 零 |
| 数据持久化 | 是 | 是 | 否（重启丢失） |
| 生产可用 | 是 | 是 | 否（仅开发测试） |
| 与现有 DB 整合 | 共用 PostgreSQL | 独立服务 | 不适用 |

**推荐策略**：开发环境用 `SimpleVectorStore`，生产环境用 `PGVector`，通过 Spring Profile 切换。

---

## 二、安装 PostgreSQL + PGVector

### 方式 1：Docker（推荐）

```bash
docker run -it --rm --name postgres-pgvector \
  -p 5432:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=cook_like_hoc \
  pgvector/pgvector
```

### 方式 2：Windows 本地安装

#### 2.1 安装 PostgreSQL

1. 下载 PostgreSQL 16+：https://www.postgresql.org/download/windows/
2. 安装时记住 `postgres` 用户的密码
3. 默认端口 5432

#### 2.2 安装 PGVector 扩展

PGVector 在 Windows 上需要编译或使用预编译版本。

**推荐**：使用 Stack Builder（PostgreSQL 安装包自带）安装 PGVector。

或手动：

```cmd
# 1. 下载预编译的 vector.dll
# https://github.com/pgvector/pgvector/releases

# 2. 将 vector.dll 复制到 PostgreSQL lib 目录
copy vector.dll "C:\Program Files\PostgreSQL\16\lib\"

# 3. 复制扩展 SQL 文件
copy vector.control "C:\Program Files\PostgreSQL\16\share\extension\"
copy sql\vector--*.sql "C:\Program Files\PostgreSQL\16\share\extension\"
```

#### 2.3 创建数据库并启用扩展

```sql
-- 连接 PostgreSQL
psql -U postgres

-- 创建数据库
CREATE DATABASE cook_like_hoc_vector;

-- 切换到新数据库
\c cook_like_hoc_vector

-- 启用 PGVector 扩展
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 验证
SELECT * FROM pg_extension WHERE extname = 'vector';
```

### 方式 3：使用 phpstudy_pro 集成

如果已有 PostgreSQL 服务，只需添加 PGVector 扩展即可：

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

---

## 三、Spring AI 集成配置

### 3.1 Maven 依赖

```xml
<!-- PGVector 向量存储 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-pgvector</artifactId>
</dependency>

<!-- PostgreSQL 驱动 -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 3.2 application-pgvector.yml（生产环境用）

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/cook_like_hoc_vector
    username: ${PG_USER:postgres}
    password: ${PG_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
  ai:
    vectorstore:
      pgvector:
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: 1536
        initialize-schema: true
        schema-name: public
        table-name: vector_store
        schema-validation: true
        max-document-batch-size: 10000
```

### 3.3 Profile 切换策略

| Profile | 向量存储 | 数据源 | 用途 |
|---------|---------|--------|------|
| `local` | SimpleVectorStore (内存) | MySQL | 日常开发 |
| `pgvector` | PgVectorStore | MySQL (recipe) + PostgreSQL (vectors) | 生产部署 |

注意：`pgvector` profile 需要**同时**配置两个数据源：
- MySQL（菜品结构化数据）
- PostgreSQL（向量嵌入数据）

---

## 四、Java Bean 配置

### 4.1 PGVector 配置类

```java
@Configuration
@Profile("pgvector")
public class PgVectorConfig {

    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
            .dimensions(1536)
            .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
            .indexType(PgVectorStore.PgIndexType.HNSW)
            .initializeSchema(true)
            .schemaName("public")
            .vectorTableName("vector_store")
            .maxDocumentBatchSize(10000)
            .build();
    }
}
```

### 4.2 内存 VectorStore 配置类

```java
@Configuration
@Profile("!pgvector")  // 非 pgvector profile 时使用
public class SimpleVectorStoreConfig {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
```

---

## 五、完整建表 SQL

```sql
-- 1. 创建扩展
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. 创建向量存储表（Spring AI 自动创建时可省略）
CREATE TABLE IF NOT EXISTS vector_store (
    id          uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content     text,
    metadata    json,
    embedding   vector(1536)
);

-- 3. 创建 HNSW 索引（加速相似搜索）
CREATE INDEX IF NOT EXISTS idx_vector_store_embedding
    ON vector_store
    USING HNSW (embedding vector_cosine_ops);
```

### 索引类型对比

| 索引类型 | 构建速度 | 查询速度 | 适用场景 |
|---------|:---:|:---:|------|
| **HNSW** | 慢 | 快 | 生产环境（推荐） |
| **IVFFlat** | 快 | 较慢 | 快速原型 |
| **NONE** | 即时 | 慢（全表扫描） | < 1000 条数据 |

---

## 六、配置属性速查表

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `spring.ai.vectorstore.pgvector.index-type` | `HNSW` | 索引类型：NONE / IVFFlat / HNSW |
| `spring.ai.vectorstore.pgvector.distance-type` | `COSINE_DISTANCE` | 距离算法：COSINE / EUCLIDEAN / NEGATIVE_INNER_PRODUCT |
| `spring.ai.vectorstore.pgvector.dimensions` | 自动检测 | 向量维度，需与 Embedding 模型一致 |
| `spring.ai.vectorstore.pgvector.initialize-schema` | `false` | 是否自动建表 |
| `spring.ai.vectorstore.pgvector.schema-name` | `public` | Schema 名称 |
| `spring.ai.vectorstore.pgvector.table-name` | `vector_store` | 表名 |
| `spring.ai.vectorstore.pgvector.schema-validation` | `false` | 是否校验表结构 |
| `spring.ai.vectorstore.pgvector.remove-existing-vector-store-table` | `false` | 启动时删除旧表 |
| `spring.ai.vectorstore.pgvector.max-document-batch-size` | `10000` | 单批次最大文档数 |

---

## 七、验证安装

```sql
-- 1. 检查 PGVector 扩展
SELECT * FROM pg_extension WHERE extname = 'vector';

-- 2. 测试向量存储
CREATE TABLE test_vector (id serial, v vector(3));
INSERT INTO test_vector (v) VALUES ('[1,2,3]'), ('[4,5,6]');
SELECT * FROM test_vector ORDER BY v <-> '[3,1,2]' LIMIT 1;
DROP TABLE test_vector;

-- 3. 检查 Spring AI 创建的表
\d vector_store
SELECT count(*) FROM vector_store;
```
