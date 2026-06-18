# CookLikeHOC RAG 配置文档

---

## 一、RAG 架构概述

### 1.1 什么是 RAG

RAG（Retrieval Augmented Generation，检索增强生成）是在 LLM 生成回答前，先从知识库检索相关文档，将文档内容注入 LLM 上下文，从而产生基于真实数据的准确回答。

### 1.2 为什么需要 RAG

| 问题 | 无 RAG | 有 RAG |
|------|--------|--------|
| 厨师问"阳春面怎么做" | LLM 可能编造配方 | 从菜谱库检索真实配方再回答 |
| 厨师问"鸡腿能做什么菜" | LLM 泛泛而谈 | 精确匹配含鸡腿的菜品 |
| 数据更新 | 需要重新训练/微调 | 只需重建向量索引 |

### 1.3 技术选型

| 组件 | 技术 | 选型理由 |
|------|------|---------|
| **向量存储(生产)** | PGVector (PostgreSQL 扩展) | 与 MySQL 共用基础设施，Spring AI 原生支持 |
| **向量存储(开发)** | SimpleVectorStore (内存) | 零依赖，开发测试即开即用 |
| **Embedding 模型** | OpenAI text-embedding-3-small | 1536 维，中文效果好；或 DeepSeek 兼容端点 |
| **文档读取** | FilesystemMarkdownDocumentReader | 从文件系统加载 CookLikeHOC/*.md |
| **文本切分** | TokenTextSplitter | 按 token 数切分，支持中文标点 |
| **元数据增强** | SummaryMetadataEnricher | LLM 自动生成段落摘要 |
| **查询重写** | RewriteQueryTransformer | LLM 优化查询词以提升检索精度 |
| **RAG Advisor** | RetrievalAugmentationAdvisor | Spring AI 开箱即用的 RAG 流程 |
| **LLM 框架** | Spring AI 1.0.0 | Java 生态，与 Spring Boot 深度集成 |

### 1.4 整体架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                        R A G   架 构 图                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  用户查询: "怎么炖鸡汤好喝？"                                          │
│       │                                                              │
│       ▼                                                              │
│  ┌──────────────────────┐                                            │
│  │ QueryEnhancerFactory  │  工厂：按场景选择查询增强器                   │
│  └──────────┬───────────┘                                            │
│             ▼                                                         │
│  ┌──────────────────────┐                                            │
│  │ CookContextQueryEnhancer │  增强：注入烹饪领域上下文                 │
│  └──────────┬───────────┘                                            │
│             ▼                                                         │
│  ┌──────────────────────┐                                            │
│  │ RewriteQueryTransformer │  重写：LLM 优化查询为检索友好形式          │
│  └──────────┬───────────┘                                            │
│             ▼                                                         │
│  ┌──────────────────────┐                                            │
│  │ VectorStoreDocumentRetriever │  检索：语义相似度搜索 topK=5         │
│  └──────────┬───────────┘                                            │
│             ▼                                                         │
│  ┌──────────────────────┐     ┌─────────────────────┐                │
│  │ RetrievalAugmentation │────▶│    ChatModel        │                │
│  │ Advisor (注入上下文)   │     │ (DeepSeek 生成回答)  │                │
│  └──────────────────────┘     └─────────────────────┘                │
│                                                                      │
│  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │
│                        离线 ETL 管线                                   │
│  ┌────────────┐   ┌──────────────┐   ┌───────────────────┐           │
│  │ Markdown   │──▶│ TokenText    │──▶│ SummaryMetadata   │           │
│  │ Reader     │   │ Splitter     │   │ Enricher (AI)     │           │
│  │ 加载336个md │   │ 切分为chunks │   │ 生成段落摘要       │           │
│  └────────────┘   └──────────────┘   └───────┬───────────┘           │
│                                               │                       │
│                                               ▼                       │
│                                    ┌───────────────────┐             │
│                                    │  VectorStore      │             │
│                                    │  (PGVector/内存)   │             │
│                                    └───────────────────┘             │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 二、新增 Maven 依赖

在 `pom.xml` 的 `<dependencies>` 中添加：

```xml
<!-- Spring AI RAG 模块（RetrievalAugmentationAdvisor、QueryTransformer） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-rag</artifactId>
</dependency>

<!-- Spring AI PGVector 向量存储 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-pgvector</artifactId>
</dependency>

<!-- Spring AI Markdown 文档读取器 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-markdown-document-reader</artifactId>
</dependency>

<!-- Spring JDBC（PGVector 需要 JdbcTemplate） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>

<!-- PostgreSQL 驱动（PGVector 需要） -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

> **注意**：所有 Spring AI 依赖版本由 `spring-ai-bom:1.0.0` 统一管理。

---

## 三、Profile 切换策略

| Profile | VectorStore Bean | 数据源 | 持久化 | 用途 |
|---------|-----------------|--------|:---:|------|
| `local` | `SimpleVectorStore` | MySQL(菜品) | 否 | 日常开发 |
| `pgvector` | `PgVectorStore` | MySQL(菜品) + PostgreSQL(向量) | 是 | 生产环境 |

切换方式：
```bash
# 开发（内存向量库）
--spring.profiles.active=local

# 生产（PGVector）
--spring.profiles.active=pgvector
```

---

## 四、配置文件

### 4.1 application-rag.yml（RAG 公共配置）

```yaml
# RAG 管线配置（所有 profile 共享）
cook:
  rag:
    # 菜谱 Markdown 文件根目录
    document-root: ${COOK_DOC_ROOT:../CookLikeHOC}

    # Token 文本切分配置
    token-splitter:
      chunk-size: 800
      min-chunk-size-chars: 350
      min-chunk-length-to-embed: 10
      max-num-chunks: 5000
      keep-separator: true

    # 检索配置
    retrieval:
      top-k: 5
      similarity-threshold: 0.50
```

### 4.2 application-local.yml 扩展（开发环境）

```yaml
spring:
  # MySQL 数据源（已有）
  datasource:
    url: jdbc:mysql://localhost:3306/cook_like_hoc?...
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:root}

  ai:
    openai:
      api-key: ${DEEPSEEK_API_KEY:your-deepseek-api-key}
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat
          temperature: 0.8
      # Embedding 配置（DeepSeek 如不支持则换 OpenAI）
      embedding:
        enabled: true
        options:
          model: text-embedding-3-small
        # 如 DeepSeek 无 embedding 端点，单独配置：
        # base-url: https://api.openai.com
        # api-key: ${OPENAI_API_KEY:}
```

### 4.3 application-pgvector.yml（生产环境）

```yaml
spring:
  # PostgreSQL 数据源（PGVector 专用）
  datasource:
    url: jdbc:postgresql://localhost:5432/cook_like_hoc_vectors
    username: ${PG_USER:postgres}
    password: ${PG_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver

  ai:
    openai:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat
      embedding:
        options:
          model: text-embedding-3-small

    vectorstore:
      pgvector:
        enabled: true
        index-type: HNSW                    # 高性能索引
        distance-type: COSINE_DISTANCE      # 余弦相似度
        dimensions: 1536                    # embedding 维度
        max-document-batch-size: 10000      # 批量写入上限
        initialize-schema: true             # 自动建表
        remove-existing-vector-store-table: false
```

---

## 五、Java 类设计

### 5.1 包结构

```
com.fontal.cookagent
├── rag
│   ├── config
│   │   ├── RagAutoConfiguration.java      # RAG 核心 Bean 配置
│   │   ├── VectorStoreConfig.java         # Profile 切换 VectorStore
│   │   └── ChatClientConfig.java          # RAG 专用 ChatClient
│   │
│   ├── etl
│   │   ├── FilesystemMarkdownDocumentReader.java  # 文件系统 MD 读取
│   │   ├── CookRecipeMetadataExtractor.java       # 元数据提取
│   │   ├── CookEtlPipeline.java                   # ETL 管线编排
│   │   └── EtlPipelineRunner.java                 # 启动时自动执行
│   │
│   ├── enhancer
│   │   ├── QueryEnhancer.java             # 查询增强接口
│   │   └── CookContextQueryEnhancer.java  # 烹饪领域增强实现
│   │
│   ├── query
│   │   └── CookRewriteQueryTransformer.java  # 查询重写器
│   │
│   ├── factory
│   │   ├── QueryEnhancerFactory.java      # 按 domain 创建增强器
│   │   └── RetrievalAdvisorFactory.java   # 按场景创建 RAG Advisor
│   │
│   ├── properties
│   │   └── RagProperties.java             # @ConfigurationProperties
│   │
│   └── document
│       └── DocumentMetadataKeys.java      # 元数据 Key 常量
```

### 5.2 核心类职责

| 类 | 职责 |
|------|------|
| **RagProperties** | 绑定 `cook.rag.*` 配置：文档根路径、切分参数、检索参数 |
| **VectorStoreConfig** | `@Profile("local")` → SimpleVectorStore / `@Profile("pgvector")` → PgVectorStore |
| **RagAutoConfiguration** | 创建 TokenTextSplitter、SummaryMetadataEnricher、RewriteQueryTransformer、RetrievalAugmentationAdvisor |
| **FilesystemMarkdownDocumentReader** | 遍历 `../CookLikeHOC/` 下所有 .md 文件，排除 README.md，为每个文档附加 category/recipe 元数据 |
| **CookEtlPipeline** | 编排 ETL：Load → Split → Enrich → Store |
| **EtlPipelineRunner** | ApplicationRunner，启动时自动执行 ETL |
| **QueryEnhancer** | 接口 `enhance(String query, Map<String,Object> ctx)` |
| **CookContextQueryEnhancer** | 烹饪查询增强："你是厨师助手...请重点关注 X 类菜品" |
| **QueryEnhancerFactory** | 按 domain 返回对应 QueryEnhancer，默认 `cooking` |
| **RetrievalAdvisorFactory** | 按场景创建 Advisor：`createRecipeSearchAdvisor()` / `createIngredientMatchAdvisor()` / `createCookingAdviceAdvisor()` |

---

## 六、ETL 管线详解

### 6.1 数据流

```
CookLikeHOC/ (15 个分类目录，336 个 .md 文件，排除 15 个 README.md)
        │
        ▼
┌───────────────────────────────────────┐
│ 1. LOAD — FilesystemMarkdownDocumentReader
│   遍历文件系统 → 排除 README/docs/docker_support/
│   每个文件附加 metadata:
│     - recipe_name: "什锦蛋炒饭"
│     - category_name: "炒菜"
│     - category_dir: "炒菜"
│     - source_file: "../CookLikeHOC/炒菜/什锦蛋炒饭.md"
│   输出: ~1000 个 Document (按 Markdown 标题分节)
└───────────────┬───────────────────────┘
                ▼
┌───────────────────────────────────────┐
│ 2. SPLIT — TokenTextSplitter
│   chunkSize=800, minChunkSizeChars=350
│   keepSeparator=true
│   输出: ~1500-2500 个 chunk
└───────────────┬───────────────────────┘
                ▼
┌───────────────────────────────────────┐
│ 3. ENRICH — SummaryMetadataEnricher
│   使用 ChatModel 为每个 chunk 生成摘要
│   写入 metadata: section_summary
│   输出: 带 AI 摘要的 Document
└───────────────┬───────────────────────┘
                ▼
┌───────────────────────────────────────┐
│ 4. STORE — VectorStore.add()
│   EmbeddingModel 生成向量
│   写入 VectorStore (内存或 PGVector)
│   输出: 可检索的向量索引
└───────────────────────────────────────┘
```

### 6.2 关键参数说明

| 参数 | 值 | 说明 |
|------|-----|------|
| chunkSize | 800 tokens | 中文菜谱平均 10-20 行，一个菜品约 500-1000 tokens |
| minChunkSizeChars | 350 | 忽略过短的段落 |
| topK | 5 | 每次检索返回最相关的 5 个文档 |
| similarityThreshold | 0.50 | 余弦相似度 ≥ 0.5 视为相关 |

---

## 七、查询增强流程

### 7.1 流程

```
用户输入: "怎么炖鸡汤好喝？"
    │
    ▼
QueryEnhancerFactory.getEnhancer("cooking")
    → 返回 CookContextQueryEnhancer
    │
    ▼
CookContextQueryEnhancer.enhance(query, context)
    → "你是厨师助手。以下是一个关于烹饪的问题：怎么炖鸡汤好喝？
       请重点关注 汤 类菜品。请从菜谱库中检索最匹配的菜品。"
    │
    ▼
RewriteQueryTransformer.transform(enhancedQuery)
    → LLM 优化: "寻找炖鸡汤的菜谱，关注汤品类下的鸡汤制作方法和调料使用技巧"
    │
    ▼
VectorStoreDocumentRetriever.retrieve(transformedQuery)
    → 语义相似度搜索，topK=5，threshold=0.50
    → 返回: [老鸡汤.md, 肥西老母鸡汤.md, 菌菇鸡汤.md, ...]
    │
    ▼
RetrievalAugmentationAdvisor
    → 将检索到的文档注入 LLM 上下文
    │
    ▼
ChatModel (DeepSeek) → 生成最终回答
```

### 7.2 Advisor 场景矩阵

| 场景 | Advisor 创建方法 | similarityThreshold | 说明 |
|------|-----------------|:---:|------|
| 通用菜谱搜索 | `createRecipeSearchAdvisor()` | 0.50 | 默认场景 |
| 食材精确匹配 | `createIngredientMatchAdvisor()` | 0.75 | 高阈值确保精确匹配 |
| 烹饪建议/技巧 | `createCookingAdviceAdvisor()` | 0.30 | 低阈值捕获相关知识 |
| 按分类筛选 | `createCategoryFilteredAdvisor("汤")` | 0.50 | 仅检索指定分类 |

---

## 八、Embedding 策略

### 8.1 方案对比

| 方案 | Embedding 模型 | 优点 | 缺点 |
|------|--------------|------|------|
| **A** | DeepSeek（如支持） | 统一 API Key，便宜 | DeepSeek 可能不支持 /v1/embeddings |
| **B** | OpenAI text-embedding-3-small | 中文效果好，1536 维 | 需要额外 API Key |
| **C** | 本地 Ollama (nomic-embed-text) | 免费，离线可用 | 需要 GPU，维度不同 |

### 8.2 推荐方案

**默认方案 B**：使用 OpenAI text-embedding-3-small，DeepSeek 用于 Chat。

配置示例：
```yaml
spring:
  ai:
    openai:
      # Chat 走 DeepSeek
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat
      # Embedding 走 OpenAI
      embedding:
        api-key: ${OPENAI_API_KEY}
        base-url: https://api.openai.com
        options:
          model: text-embedding-3-small
```

> 如果 DeepSeek 后续支持 embedding，只需修改 `embedding.base-url` 即可。

---

## 九、实施顺序

| 步骤 | 内容 | 新增文件数 |
|:---:|------|:---:|
| 1 | pom.xml 添加 RAG 依赖 | 0（修改） |
| 2 | RagProperties + application-rag.yml | 2 |
| 3 | VectorStoreConfig（Profile Bean）+ ChatClientConfig | 2 |
| 4 | FilesystemMarkdownDocumentReader + MetadataExtractor | 2 |
| 5 | RagAutoConfiguration（splitter/enricher/transformer/advisor） | 1 |
| 6 | CookEtlPipeline + EtlPipelineRunner | 2 |
| 7 | QueryEnhancer + CookContextQueryEnhancer + QueryEnhancerFactory | 3 |
| 8 | RetrievalAdvisorFactory | 1 |
| 9 | ETL 集成验证（启动测试） | 0 |
| 10 | PGVector 环境验证 | 0 |

---

## 十、验证方式

### 10.1 开发环境（local profile）

1. 启动应用，观察日志：
```
[EtlPipelineRunner] Starting RAG ETL pipeline...
[FilesystemMarkdownDocumentReader] Loaded 336 raw documents from filesystem
[CookEtlPipeline] Split into 1847 chunks
[CookEtlPipeline] Enriched 1847 documents with AI summaries
[CookEtlPipeline] Stored 1847 documents into vector store
[EtlPipelineRunner] RAG ETL pipeline completed successfully.
```

2. 调用检索 API 验证。

### 10.2 生产环境（pgvector profile）

1. 启动 PostgreSQL + PGVector
2. 启动应用，检查 PGVector 表：
```sql
SELECT count(*) FROM vector_store;  -- 应 > 0
```
3. 调用检索 API 验证。

---

## 十一、关键风险

| 风险 | 应对 |
|------|------|
| DeepSeek 不支持 embedding | 分离 chat/embedding 端点，embedding 走 OpenAI |
| 每次启动重复 ETL（PGVector） | 加 `if (count > 0) return` 跳过已索引数据 |
| SummaryMetadataEnricher LLM 调用成本 | 可选关闭（skip 跳过 enrich 步骤） |
| MarkdownDocumentReader 切分过细 | 调大 `chunk-size` 到 1200 |
