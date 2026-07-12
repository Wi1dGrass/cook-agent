package com.fontal.cookagent.rag.etl;

import com.fontal.cookagent.entity.Recipe;
import com.fontal.cookagent.mapper.RecipeMapper;
import com.fontal.cookagent.rag.document.DocumentMetadataKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CookEtlPipeline {

    private static final Logger log = LoggerFactory.getLogger(CookEtlPipeline.class);

    private final FilesystemMarkdownDocumentReader documentReader;
    private final TokenTextSplitter textSplitter;
    private final ConcurrentSummaryEnricher summaryEnricher;
    private final VectorStore vectorStore;
    private final RecipeMapper recipeMapper;

    @Autowired(required = false)
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate pgJdbcTemplate;

    public CookEtlPipeline(FilesystemMarkdownDocumentReader documentReader,
                           TokenTextSplitter textSplitter,
                           ConcurrentSummaryEnricher summaryEnricher,
                           VectorStore vectorStore,
                           RecipeMapper recipeMapper) {
        this.documentReader = documentReader;
        this.textSplitter = textSplitter;
        this.summaryEnricher = summaryEnricher;
        this.vectorStore = vectorStore;
        this.recipeMapper = recipeMapper;
    }

    /** 执行完整 ETL 管线：Load → Split → Enrich → Store */
    public void run() {
        run(false);
    }

    /**
     * 执行 ETL 管线；{@code rebuild=true} 时先清空向量库再做全量导入。
     */
    public void run(boolean rebuild) {
        log.info("=== RAG ETL Pipeline Started (rebuild={}) ===", rebuild);

        if (rebuild) {
            try {
                if (pgJdbcTemplate != null) {
                    int deleted = pgJdbcTemplate.update("DELETE FROM vector_store");
                    log.info("已清空向量库: 删除 {} 行", deleted);
                } else {
                    vectorStore.delete(java.util.List.of());
                    log.info("已清空向量库（VectorStore.delete）");
                }
            } catch (Exception e) {
                log.warn("清空向量库失败，将直接 add: {}", e.getMessage());
            }
        }

        // 1. Load — 从文件系统加载 Markdown 文档
        List<Document> rawDocuments = documentReader.loadAll();
        log.info("[1/4] Loaded {} raw documents", rawDocuments.size());

        // 1.5 Enrich — 为每个文档注入 recipe_id（从 MySQL 查询）
        enrichWithRecipeId(rawDocuments);

        // 2. Split — Token 文本切分
        List<Document> chunks = textSplitter.apply(rawDocuments);
        log.info("[2/4] Split into {} chunks", chunks.size());

        // 3. Enrich — AI 摘要生成（提升检索准确度）
        List<Document> enrichedChunks = chunks;
        try {
            enrichedChunks = summaryEnricher.apply(chunks);
            log.info("[3/4] Enriched {} documents with AI summaries", enrichedChunks.size());
        } catch (Exception e) {
            log.warn("[3/4] AI summary enrichment failed, storing without summaries: {}", e.getMessage());
        }

        // 4. Store — 向量化并写入 VectorStore
        vectorStore.add(enrichedChunks);
        log.info("[4/4] Stored {} documents into vector store", enrichedChunks.size());

        log.info("=== RAG ETL Pipeline Completed ===");
    }

    /**
     * 批量查询 MySQL，为每个 Document 注入 recipe_id 元数据。
     * 优先按 recipe_name 匹配，未命中则按 source_file 路径后缀匹配。
     * RecipeService.search() 依赖 recipe_id 进行过滤，缺失则结果被丢弃。
     */
    private void enrichWithRecipeId(List<Document> documents) {
        if (documents.isEmpty()) return;

        List<Recipe> allRecipes = recipeMapper.selectList(null);
        Map<String, Long> nameToId = allRecipes.stream()
                .collect(Collectors.toMap(Recipe::getName, Recipe::getId, (a, b) -> a));
        Map<String, Long> sourceFileToId = allRecipes.stream()
                .filter(r -> r.getSourceFile() != null)
                .collect(Collectors.toMap(
                        r -> r.getSourceFile().replace("\\", "/"),
                        Recipe::getId, (a, b) -> a));

        int matched = 0;
        int fallback = 0;
        for (Document doc : documents) {
            Object recipeNameObj = doc.getMetadata().get(DocumentMetadataKeys.RECIPE_NAME);
            if (recipeNameObj == null) continue;

            Long recipeId = nameToId.get(recipeNameObj.toString());
            if (recipeId == null) {
                String sourceFile = doc.getMetadata().get(DocumentMetadataKeys.SOURCE_FILE) != null
                        ? doc.getMetadata().get(DocumentMetadataKeys.SOURCE_FILE).toString().replace("\\", "/")
                        : null;
                if (sourceFile != null) {
                    for (Map.Entry<String, Long> entry : sourceFileToId.entrySet()) {
                        if (sourceFile.endsWith(entry.getKey())) {
                            recipeId = entry.getValue();
                            fallback++;
                            break;
                        }
                    }
                }
            }

            if (recipeId != null) {
                doc.getMetadata().put(DocumentMetadataKeys.RECIPE_ID, recipeId);
                matched++;
            }
        }
        log.info("[1.5/4] Injected recipe_id into {}/{} documents (name-match={}, file-fallback={})",
                matched, documents.size(), matched - fallback, fallback);
    }
}
