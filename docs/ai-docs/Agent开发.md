# Agent 核心功能开发文档

> 状态：开发完成，单元测试 23/23 通过。集成测试待 pgvector 环境验证。

---

## 一、模块总览

```
com.fontal.cookagent.app/
├── chat/                                 # 普通对话模块
│   ├── ChatService.java                  # RAG + 记忆 对话服务
│   ├── config/
│   │   ├── ChatServiceConfig.java        # Bean 配置
│   │   └── ChatMemoryProperties.java     # @ConfigurationProperties
│   └── memory/
│       └── FileBasedChatMemory.java      # Kryo 二进制序列化记忆（实现 ChatMemory）
│
├── agent/                                # ReAct Agent 模块
│   ├── AgentService.java                 # 多步推理 + 工具调用
│   ├── config/
│   │   └── AgentServiceConfig.java       # AgentService Bean
│   └── tools/
│       ├── RecipeSearchTool.java         # 菜谱向量搜索（@Tool）
│       ├── RecipeRecommendTool.java      # 菜谱推荐（@Tool）
│       ├── WebSearchTool.java            # DuckDuckGo 联网搜索（@Tool）
│       └── ImageSearchTool.java          # Pexels 图片搜索（@Tool）
│
└── cache/                                # 缓存工具模块
    └── CacheUtils.java                   # Caffeine L1 + Redis L2 统一缓存
```

---

## 二、普通对话 — ChatService

### 2.1 数据流

```
用户消息
  → MessageChatMemoryAdvisor（从 FileBasedChatMemory 加载历史 → 注入上下文）
  → RetrievalAugmentationAdvisor（从 PGVector 检索相关菜谱 → 增强 prompt）
  → DeepSeek ChatModel（生成回答）
  → MessageChatMemoryAdvisor（将本轮对话写入 FileBasedChatMemory）
  → 返回回答
```

### 2.2 核心 API

```java
// 在已有对话中发送消息
String reply = chatService.chat(conversationId, "我想学炖鸡汤");

// 开始新对话（自动生成 conversationId）
String convId = chatService.startNewChat("推荐一道菜");
```

### 2.3 FileBasedChatMemory 设计

| 特性 | 实现 |
|------|------|
| 序列化 | Kryo 5.6.2 二进制，比 JSON 快 5-10x |
| 存储格式 | 一个对话一个 `.kryo` 文件：`{storagePath}/{conversationId}.kryo` |
| 线程安全 | `ConcurrentHashMap<String, ReentrantLock>` 每对话独立锁 |
| 内存缓存 | `ConcurrentHashMap<String, SoftReference<ConversationData>>` |
| 定时淘汰 | `@Scheduled` 每小时清理过期文件（默认 7 天） |
| Kryo 安全 | `KryoPool`（池大小 32）避免线程安全问题 |

**序列化 DTO 结构**：
```java
record ConversationData(String conversationId, Instant updatedAt, List<MessageDTO> messages) {}
record MessageDTO(MessageType messageType, String text, Map<String, Object> metadata,
                  List<ToolCallDTO> toolCalls, List<ToolResponseDTO> toolResponses) {}
record ToolCallDTO(String id, String type, String name, String arguments) {}
record ToolResponseDTO(String id, String name, String responseData) {}
```

DTO 层保证了 Spring AI 原生 Message 实现类（`protected final` 字段、Record 内部类）的可靠序列化。

---

## 三、ReAct Agent — AgentService

### 3.1 数据流

```
用户任务
  → MessageChatMemoryAdvisor（注入对话历史）
  → System Prompt 注入（厨师角色 + 工具列表）
  → DeepSeek ChatModel（分析任务 → 决定调用 Tool）
  → ToolCallAdvisor（自动处理 Tool Calling 循环）
  → 返回最终回答
```

### 3.2 System Prompt

Agent 被设定为"经验丰富的中餐厨师兼AI助手"，工作原则：
1. 分析用户任务，规划步骤
2. 优先使用菜谱知识库 `searchRecipes` / `recommendRecipes`
3. 知识库无答案时使用 `webSearch` 联网搜索
4. 用户需要图片时使用 `searchImages`
5. 综合信息给出专业回答

### 3.3 Tool 工具集

| Tool | 方法 | 数据源 | 说明 |
|------|------|--------|------|
| `searchRecipes` | `(query, topK?, category?)` | PGVector | 语义搜索菜谱，支持分类筛选 |
| `recommendRecipes` | `(criteria, count?, category?)` | PGVector | 推荐菜谱，自动 recipe_name 去重 |
| `webSearch` | `(query, maxResults?)` | DuckDuckGo API | 免费联网搜索，无需 API Key |
| `searchImages` | `(query, count?)` | Pexels API | 搜索菜品图片，需 API Key |

---

## 四、缓存工具 — CacheUtils

### 4.1 两级缓存架构

```
读路径：L1 (Caffeine) → L2 (Redis) → Loader 回源
写路径：Redis → Caffeine（先写远端再写本地）
删路径：Caffeine + Redis 同步删除
```

### 4.2 核心 API

```java
// 自动 L1→L2→loader 回源
T value = cacheUtils.get(key, Type.class, () -> fetchFromDb());

// 写入（默认 TTL）
cacheUtils.put(key, value);

// 写入（指定 TTL）
cacheUtils.put(key, value, 30, TimeUnit.SECONDS);

// 删除
cacheUtils.evict(key);

// 清空
cacheUtils.evictAll();
```

### 4.3 Redis 降级

Redis 不可用时自动降级为纯 Caffeine 模式，不影响主流程。通过 `isRedisAvailable()` 查询降级状态。

---

## 五、配置项

```yaml
cook:
  chat:
    memory:
      storage-path: ${COOK_MEMORY_PATH:./chat-memory}
      eviction-days: 7
  pexels:
    api-key: ${PEXELS_API_KEY:}
  cache:
    redis:
      enable: true
      ttl-seconds: 3600
    caffeine:
      max-size: 1000
      expire-after-write-seconds: 3600
```

---

## 六、新增 Maven 依赖

```xml
<!-- Kryo 高性能二进制序列化（FileBasedChatMemory） -->
<dependency>
    <groupId>com.esotericsoftware</groupId>
    <artifactId>kryo</artifactId>
    <version>5.6.2</version>
</dependency>

<!-- Caffeine 本地缓存 -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>

<!-- Redis 分布式缓存 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

---

## 七、测试

### 7.1 单元测试（23/23 通过）

| 测试类 | 测试数 | 覆盖 |
|--------|:---:|------|
| `FileBasedChatMemoryTest` | 12 | Kryo 序列化往返、追加消息、多对话隔离、线程安全、中文、持久化 |
| `CacheUtilsTest` | 11 | L1 命中/miss、loader 回源、Redis L2 命中、Redis 降级、TTL、多类型 |

### 7.2 集成测试（需 pgvector）

| 测试类 | 测试数 | 覆盖 |
|--------|:---:|------|
| `ChatServiceTest` | 4 | 单轮 RAG、多轮记忆、持久化验证、对话隔离 |
| `AgentServiceTest` | 3 | Tool 调用、菜谱推荐、记忆保留 |

### 7.3 运行方式

```bash
# 单元测试（无需数据库）
mvn test -Dtest="FileBasedChatMemoryTest,CacheUtilsTest"

# 集成测试（需 PostgreSQL + pgvector）
mvn test -Dtest="ChatServiceTest,AgentServiceTest"
```

---

## 八、关键风险

| 风险 | 严重度 | 应对 |
|------|:--:|------|
| DeepSeek 不支持 function calling | 高 | 需实测验证；如不支持则改为 rule-based 路由 |
| Pexels API Key 未配置 | 中 | ImageSearchTool 返回明确提示，不影响其他 Tool |
| FileBasedChatMemory 并发写 | 低 | 每对话独立 ReentrantLock + Kryo ThreadLocal |
| Redis 不可用 | 低 | CacheUtils 自动降级为纯 Caffeine |
| Kryo 序列化 Message | 低 | 已通过 DTO 层转换规避 Spring AI 原生类序列化问题 |

---

## 九、下一步

1. **DeepSeek function calling 验证**：启动 pgvector 环境，运行 `AgentServiceTest` 确认 Tool 调用是否正常工作
2. **RAG 后续任务**（参考 `docs/ai-docs/RAG后续任务.md`）：数据校验 → Enrich 补全 → 检索质量评估 → 查询缓存
3. **Controller 层**：为 `ChatService` 和 `AgentService` 添加 REST API
