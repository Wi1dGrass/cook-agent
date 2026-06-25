package com.fontal.cookagent.rag.etl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CookEtlPipeline {

    private static final Logger log = LoggerFactory.getLogger(CookEtlPipeline.class);

    private final FilesystemMarkdownDocumentReader documentReader;
    private final TokenTextSplitter textSplitter;
    private final SummaryMetadataEnricher summaryEnricher;
    private final VectorStore vectorStore;

    public CookEtlPipeline(FilesystemMarkdownDocumentReader documentReader,
                           TokenTextSplitter textSplitter,
                           SummaryMetadataEnricher summaryEnricher,
                           VectorStore vectorStore) {
        this.documentReader = documentReader;
        this.textSplitter = textSplitter;
        this.summaryEnricher = summaryEnricher;
        this.vectorStore = vectorStore;
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
                vectorStore.delete(java.util.List.of());
                log.info("已清空向量库");
            } catch (Exception e) {
                log.warn("清空向量库失败，将直接 add: {}", e.getMessage());
            }
        }

        // 1. Load — 从文件系统加载 Markdown 文档
        List<Document> rawDocuments = documentReader.loadAll();
        log.info("[1/4] Loaded {} raw documents", rawDocuments.size());

        // 2. Split — Token 文本切分
        List<Document> chunks = textSplitter.apply(rawDocuments);
        log.info("[2/4] Split into {} chunks", chunks.size());

        // 3. Store — 向量化并写入 VectorStore
        vectorStore.add(chunks);
        log.info("[3/4] Stored {} documents into vector store", chunks.size());

        log.info("=== RAG ETL Pipeline Completed ===");
    }
}
