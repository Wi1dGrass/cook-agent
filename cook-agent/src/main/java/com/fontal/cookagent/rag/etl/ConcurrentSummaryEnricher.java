package com.fontal.cookagent.rag.etl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 并发 AI 摘要增强器 — 为每个 chunk 生成当前段摘要并写入 metadata。
 * <p>
 * 使用固定线程池并发调用 ChatModel，大幅加速 ETL Enrich 阶段。
 * 默认并发度 16，可通过构造参数调整。
 */
public class ConcurrentSummaryEnricher {

    private static final Logger log = LoggerFactory.getLogger(ConcurrentSummaryEnricher.class);

    private static final String SYSTEM_PROMPT = """
            你是一个专业的菜谱内容摘要助手。请为以下菜谱文本片段生成一段简短的中文摘要（50-100字），\
            概括其核心内容（菜品名称、关键食材、烹饪要点等）。\
            只输出摘要正文，不要输出任何额外解释、前缀或标记。""";

    private final ChatModel chatModel;
    private final int concurrency;

    public ConcurrentSummaryEnricher(ChatModel chatModel) {
        this(chatModel, 16);
    }

    public ConcurrentSummaryEnricher(ChatModel chatModel, int concurrency) {
        this.chatModel = chatModel;
        this.concurrency = concurrency;
    }

    /**
     * 并发为每个 Document 生成 AI 摘要，写入 metadata 的 "section_summary" 键。
     *
     * @param documents 待增强的文档列表
     * @return 增强后的文档列表（同一批对象，metadata 已更新）
     */
    public List<Document> apply(List<Document> documents) {
        if (documents.isEmpty()) return documents;

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(concurrency, documents.size()));
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = new ArrayList<>(documents.size());

        for (Document doc : documents) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    String summary = generateSummary(doc.getText());
                    if (summary != null && !summary.isBlank()) {
                        doc.getMetadata().put("section_summary", summary);
                        success.incrementAndGet();
                    }
                } catch (Exception e) {
                    failed.incrementAndGet();
                    log.debug("Enrich failed for doc id={}: {}", doc.getId(), e.getMessage());
                }
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        log.info("Concurrent enrichment done: {}/{} succeeded, {} failed (concurrency={})",
                success.get(), documents.size(), failed.get(), concurrency);

        return documents;
    }

    private String generateSummary(String text) {
        if (text == null || text.isBlank()) return null;

        List<Message> messages = List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage(text)
        );

        ChatResponse response = chatModel.call(new Prompt(messages));
        if (response == null || response.getResult() == null) return null;
        String content = response.getResult().getOutput().getText();
        return content != null ? content.trim() : null;
    }
}
