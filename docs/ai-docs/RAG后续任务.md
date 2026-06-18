# RAG 后续任务

> 当前状态：ETL 导入完成（724 chunks / 257 recipes），核心 RAG 链路（Embedding → 检索 → 增强 → 生成）已跑通。

---

## 任务一：数据校验 + 覆盖率报告

**目标**：确认 336 个菜谱目录全部导入，无遗漏、无损坏、无空 chunk。

### 1.1 创建 `DataValidationRunner`

**文件**：`src/main/java/com/fontal/cookagent/rag/etl/DataValidationRunner.java`

```
实现 CommandLineRunner，在 EtlPipelineRunner 之后执行
```

**校验维度**：

| 维度 | SQL / 方法 | 说明 |
|------|-----------|------|
| 总记录数 | `SELECT count(*) FROM vector_store` | 基准总量 |
| 唯一菜谱数 | `SELECT count(DISTINCT metadata->>'recipe_name') FROM vector_store` | 应与源文件数一致 |
| 唯一分类数 | `SELECT count(DISTINCT metadata->>'category_name') FROM vector_store` | 分类覆盖率 |
| 每分类菜谱数 | `SELECT metadata->>'category_name', count(DISTINCT metadata->>'recipe_name') FROM vector_store GROUP BY category_name ORDER BY count DESC` | 分类分布 |
| 空内容 chunk | `SELECT count(*) FROM vector_store WHERE content IS NULL OR content = ''` | 损坏检测 |
| 零向量 | `SELECT count(*) FROM vector_store WHERE embedding IS NULL` | 向量化失败检测 |

**chunk 大小分布**：按长度区间统计（0-100, 100-300, 300-500, 500-1000, 1000+）

### 1.2 源文件对比

**方法**：
1. 用 `FilesystemMarkdownDocumentReader.loadAll()` 重新扫描源目录，列出所有 `(category_dir, recipe_name)`
2. 从 `vector_store` metadata 中查出所有 `(category_name, recipe_name)` 组合
3. 对比差异，输出 **未导入列表** 和 **已导入但源文件缺失列表**

**输出格式**：
```
=== 数据覆盖率报告 ===
源文件总数:  336
已导入菜谱:  257
未导入菜谱:  79
覆盖率:      76.5%

--- 按分类统计 ---
  汤类:     25/30 (83.3%)
  肉类:     40/50 (80.0%)
  ...

--- 未导入菜谱 ---
  汤类/酸辣汤.md
  肉类/回锅肉.md
  ...
```

### 1.3 原因排查

对未导入文件检查：
- 是否在 `EXCLUDE_DIRS`（docs, docker_support, images）中？
- 文件名是否为 `README.md`（被排除）？
- 文件内容是否为空？
- `MarkdownDocumentReader.get()` 是否返回空列表？
- 切分后所有 chunk 是否 < `minChunkLengthToEmbed`(10) 被丢弃？

### 1.4 验证

启动应用查看日志：
```
DataValidationRunner: === 数据覆盖率报告 ===
DataValidationRunner: 总 chunks: 724, 菜谱: 257/336, 覆盖率: 76.5%
```

---

## 任务二：Enrich 步骤补全

**目标**：为每个 chunk 用 LLM 生成摘要元数据，提升检索准确性。

### 2.1 背景

`SummaryMetadataEnricher` 为每个 chunk 调用 ChatModel，生成前/中/后三段摘要并写入 metadata。当前 `CookEtlPipeline` 中此步骤被注释跳过。

### 2.2 实施步骤

#### Step 1：优化摘要类型（减少 API 调用）

**文件**：`src/main/java/com/fontal/cookagent/rag/config/RagAutoConfiguration.java`

将三段摘要改为只生成当前段：
```java
@Bean
public SummaryMetadataEnricher summaryMetadataEnricher(ChatModel chatModel) {
    return new SummaryMetadataEnricher(chatModel,
            List.of(SummaryMetadataEnricher.SummaryType.CURRENT));
}
```

724 chunks → 724 次 LLM 调用（之前三段需 2172 次）。

#### Step 2：解除 Enrich 注释

**文件**：`src/main/java/com/fontal/cookagent/rag/etl/CookEtlPipeline.java`

```java
// 3. Enrich — AI 摘要生成（新增，提升检索准确度）
List<Document> enriched = summaryEnricher.apply(chunks);
log.info("[3/4] Enriched {} documents with AI summaries", enriched.size());

// 4. Store
vectorStore.add(enriched);
log.info("[4/4] Stored {} enriched documents into vector store", enriched.size());
```

#### Step 3：处理增量导入

当前 PGVector 已有 724 chunks（无摘要），直接重跑 ETL 会因 `ON CONFLICT (id) DO UPDATE` 而更新已有记录，但 id 是 UUID 每次生成不同，所以会是 INSERT 重复行。

**方案**：先 DROP TABLE，再全量重跑。

修改 `EtlPipelineRunner`：
```java
// 检测是否需要重新 Enrich
if (hasExistingData() && needsReEnrich()) {
    log.info("清空旧数据，启动完整 ETL（含 Enrich）");
    jdbcTemplate.execute("DROP TABLE IF EXISTS vector_store");
}
```

`needsReEnrich()` 检查逻辑：
```java
private boolean needsReEnrich() {
    Integer noSummary = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM vector_store WHERE metadata->>'section_summary' IS NULL",
        Integer.class);
    return noSummary != null && noSummary > 0;
}
```

#### Step 4：验证

```sql
-- 检查摘要覆盖率
SELECT 
    count(*) AS total,
    count(*) FILTER (WHERE metadata->>'section_summary' IS NOT NULL) AS with_summary
FROM vector_store;
```

---

## 任务三：检索质量评估

**目标**：量化评估 RAG 检索效果，为调参提供数据支撑。

### 3.1 构建测试集

**文件**：`src/test/resources/rag-eval/query-answer-pairs.json`

准备 20-30 组人工标注：

```json
[
  {"query": "怎么炖鸡汤", "expected": ["清炖鸡汤", "老母鸡汤"]},
  {"query": "排骨的做法", "expected": ["糖醋排骨", "红烧排骨"]},
  {"query": "凉拌菜有哪些", "expected": ["凉拌黄瓜", "凉拌木耳", "柠檬凤爪"]},
  {"query": "豆腐怎么做", "expected": ["麻婆豆腐", "家常豆腐"]},
  {"query": "炒饭的做法", "expected": ["蛋炒饭", "扬州炒饭"]}
]
```

### 3.2 评估指标

| 指标 | 计算 | 说明 |
|------|------|------|
| Top-K 命中率 | `命中数 / 总查询数` | 期望菜谱是否在 Top-K 结果中 |
| MRR | `Σ(1/rank_of_first_hit) / N` | 第一个相关结果排名越靠前越好 |

### 3.3 实现

**文件**：`src/test/java/com/fontal/cookagent/rag/RagEvaluationTest.java`

```java
@SpringBootTest
@ActiveProfiles("pgvector")
class RagEvaluationTest {

    @Autowired private VectorStore vectorStore;

    @Test
    @DisplayName("检索质量 — Top-5 命中率 + MRR")
    void evaluateRetrieval() {
        List<QueryPair> pairs = loadTestPairs();
        int hits = 0;
        double mrr = 0;

        for (var pair : pairs) {
            var results = vectorStore.similaritySearch(
                SearchRequest.builder()
                    .query(pair.query).topK(5)
                    .similarityThreshold(0.5).build());

            var names = results.stream()
                    .map(d -> (String) d.getMetadata().get("recipe_name"))
                    .filter(Objects::nonNull).toList();

            if (pair.expected.stream().anyMatch(names::contains)) hits++;
            for (int i = 0; i < names.size(); i++) {
                if (pair.expected.contains(names.get(i))) {
                    mrr += 1.0 / (i + 1);
                    break;
                }
            }
        }

        log.info("Top-5 命中率: {}/{} = {:.1f}%", hits, pairs.size(), 100.0 * hits / pairs.size());
        log.info("MRR: {:.4f}", mrr / pairs.size());
    }
}
```

### 3.4 参数对比

| 阈值 | topK | 命中率 | MRR |
|------|------|--------|-----|
| 0.30 | 5    |        |     |
| 0.50 | 5    |        |     |
| 0.70 | 5    |        |     |

---

## 任务四：查询缓存

**目标**：减少重复查询的 Embedding API 调用。

### 4.1 依赖

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

### 4.2 实现

**文件**：`src/main/java/com/fontal/cookagent/rag/cache/QueryCacheManager.java`

```java
@Component
public class QueryCacheManager {

    private final Cache<String, float[]> embeddingCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    public Optional<float[]> getEmbedding(String query) {
        return Optional.ofNullable(embeddingCache.getIfPresent(query));
    }

    public void putEmbedding(String query, float[] vector) {
        embeddingCache.put(query, vector);
    }
}
```

### 4.3 集成

在 `ZhipuAiEmbeddingModel.call()` 中先查缓存再调 API，调用后写入缓存。

---

## 执行顺序

```
任务一（数据校验） → 任务二（Enrich 补全） → 任务三（检索质量评估） → 任务四（查询缓存）
   1-2h                2-4h                   1-2h                   1h
```

- 任务一先做：确认数据完整性
- 任务二在数据确认后做：Enrich 需要重跑 ETL
- 任务三在 Enrich 后做：评估含摘要的检索质量
- 任务四看情况：API 调用量大时再做
