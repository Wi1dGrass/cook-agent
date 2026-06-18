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
        log.info("=== RAG ETL Pipeline Started ===");

        // 1. Load — 从文件系统加载 Markdown 文档
        List<Document> rawDocuments = documentReader.loadAll();
        log.info("[1/4] Loaded {} raw documents", rawDocuments.size());

        // 2. Split — Token 文本切分
        List<Document> chunks = textSplitter.apply(rawDocuments);
        log.info("[2/4] Split into {} chunks", chunks.size());

//        // 3. Enrich — AI 摘要生成（耗时长，暂时跳过）
//        List<Document> enriched = summaryEnricher.apply(chunks);
//        log.info("[3/4] Enriched {} documents with AI summaries", enriched.size());

        // 4. Store — 向量化并写入 VectorStore
        vectorStore.add(chunks);
        log.info("[4/4] Stored {} documents into vector store (enrich skipped)", chunks.size());

        log.info("=== RAG ETL Pipeline Completed ===");
    }
}
