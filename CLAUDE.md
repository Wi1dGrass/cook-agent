# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Prerequisites

- **JDK 21** at `C:\Users\11695\.jdks\ms-21.0.10`
- Set `JAVA_HOME` before any Maven commands:
  ```bash
  export JAVA_HOME="C:\Users\11695\.jdks\ms-21.0.10"
  ```

## Build & Run

All commands run from `cook-agent/` directory. Use `./mvnw` (Unix) or `mvnw.cmd` (Windows).

```bash
export JAVA_HOME="C:\Users\11695\.jdks\ms-21.0.10"          # Set JDK 21 first

./mvnw clean compile                    # Compile only
./mvnw clean package -DskipTests        # Package without tests
./mvnw test                             # Run all tests
./mvnw test -Dtest=ClassName            # Run single test class
./mvnw test -Dtest=ClassName#methodName # Run single test method
./mvnw spring-boot:run                  # Start server (default profile: local)
./mvnw spring-boot:run -Dspring-boot.run.profiles=pgvector  # Start with PGVector
```

Server: `http://localhost:8088/api`

## API 端点

| 端点 | 方法 | 说明 |
|---|---|---|
| `/api/health` | GET | 健康检查 |
| `/api/chat/new` | POST | 新对话（RAG + JDBC 记忆） |
| `/api/chat/send` | POST | 继续对话 |
| `/api/agent/chat` | POST | Agent 同步对话 |
| `/api/agent/chat/stream?message=...` | GET | Agent SSE 流式对话 |

API docs: `/api/swagger-ui.html` (Knife4j)

## Profiles

| Profile | Database | VectorStore | Embedding | Purpose |
|---|---|---|---|---|
| `local` (default) | MySQL `cook_like_hoc` | SimpleVectorStore (in-memory) | ZhipuAI | Local dev |
| `pgvector` | MySQL + PostgreSQL `cook_like_hoc` | PGVector | ZhipuAI | Production RAG |

Profiles are activated via `spring.profiles.active` in `application.yml`. The `rag` profile is always included. `application-local.yml` is git-ignored (contains API keys); `application-pgvector.yml` is tracked.

Most integration tests require `@ActiveProfiles("pgvector")` — they need PGVector + DeepSeek + ZhipuAI all online.

## Architecture

This is a Spring Boot 3.4.3 / Java 21 application — an AI Chinese cuisine chef named "CookManus". Three major subsystems:

### 1. Agent (ReAct + Tool Calling)
```
BaseAgent → ReActAgent → ToolCallAgent → CookManus
```

`ToolCallAgent` manages the think/act loop: sends conversation + tools to DeepSeek, gets tool call decisions, executes them via Spring AI's `ToolCallingManager`. CookManus has 10 tools (see below) and a max of 20 steps. SSE streaming is supported via `runStream()` returning `SseEmitter`.

### 2. RAG (Retrieval-Augmented Generation)
- **Vector Store**: PGVector (production) or SimpleVectorStore (dev)
- **Embedding**: Custom `ZhipuAiEmbeddingModel` calling ZhipuAI Embedding-3 (1024 dims), auto-batches 64 docs at a time
- **ETL Pipeline**: Loads markdown recipes from `../CookLikeHOC/` → splits via `TokenTextSplitter` (800 char chunks) → stores in VectorStore. Runs as `ApplicationRunner` on startup in non-pgvector profiles.
- **Retrieval**: `RetrievalAugmentationAdvisor` with `ContextualQueryAugmenter(allowEmptyContext=true)` — ensures LLM can answer non-cooking questions from memory. A `CookRewriteQueryTransformer` uses LLM to optimize queries before vector search.

### 3. Structured Data (MySQL + MyBatis-Plus)
7 tables: `category`, `recipe`, `ingredient`, `recipe_step`, `user`, `user_favorite`, `daily_recommend`. Populated from `CookLikeHOC/` markdown files via `sql/import_data.py`.

### Chat Memory
`DeepSeekConfig` creates `ChatClient` with `MessageChatMemoryAdvisor` + `MessageWindowChatMemory` (max 20 messages). `FileBasedChatMemory` (Kryo serialization to `.kryo` files with Caffeine L1 cache) is available via `@Profile("file-memory")` but disabled by default — JDBC/MySQL memory is the default.

## The 10 Agent Tools

Tools use `@Tool` annotation, registered as `ToolCallback[]` bean in `ToolRegistration`:

| Tool | Data Source |
|---|---|
| `TerminateTool` | Signals agent loop completion |
| `RecipeSearchTool` | PGVector semantic search |
| `RecipeRecommendTool` | PGVector recommendations |
| `WebSearchTool` | DuckDuckGo API |
| `ImageSearchTool` | Pexels API |
| `ListCategoryTool` | MySQL categories |
| `IngredientSearchTool` | MySQL reverse ingredient lookup |
| `RecipeCompareTool` | MySQL recipe comparison |
| `NutritionQueryTool` | MySQL nutrition JSON |
| `DailyRecommendTool` | MySQL daily picks |

## Key Config Files

| File | Purpose |
|---|---|
| `application.yml` | Root config, server port 8088, context-path /api, active profiles |
| `application-local.yml` | Local dev (MySQL + DeepSeek + Pexels). Git-ignored. |
| `application-pgvector.yml` | Production (dual datasource + PGVector + ZhipuAI). Tracked. |
| `application-rag.yml` | Shared RAG settings (chunk size, topK, similarity threshold) |

## Knowledge Base

`CookLikeHOC/` contains 336+ Chinese recipe markdown files organized in 15 category directories (炒菜, 汤, 卤菜, 主食, etc.). Each file is structured: title → ingredients list → numbered steps. The `docs/` subdirectory is excluded from ETL ingestion.
