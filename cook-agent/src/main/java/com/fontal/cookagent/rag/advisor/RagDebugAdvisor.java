package com.fontal.cookagent.rag.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * RAG 调试 Advisor — 打印检索到的文档相似度和内容摘要。
 * 放在 RetrievalAugmentationAdvisor 之后执行（order 更大），
 * 从响应上下文中读取已检索文档。
 */
@Slf4j
@Component
public class RagDebugAdvisor implements CallAdvisor {

    @Override
    public String getName() {
        return "RagDebugAdvisor";
    }

    /** 比 RetrievalAugmentationAdvisor 大，确保后执行 */
    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);
        logRetrievedDocuments(response);
        return response;
    }

    @SuppressWarnings("unchecked")
    private void logRetrievedDocuments(ChatClientResponse response) {
        Object docsObj = response.context().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT);
        if (!(docsObj instanceof List<?> list) || list.isEmpty()) {
            log.warn("RAG 检索结果为空 — 可能相似度阈值过高或向量库无匹配内容");
            return;
        }

        log.info("RAG 检索到 {} 条相关文档:", list.size());
        for (int i = 0; i < list.size(); i++) {
            if (!(list.get(i) instanceof Document doc)) {
                continue;
            }

            String content = doc.getText();
            String contentPreview = content != null && content.length() > 100
                    ? content.substring(0, 100).replace("\n", " ") + "..."
                    : content;

            log.info("  [{}/{}] score={}, recipe={}, category={}",
                    i + 1, list.size(),
                    doc.getScore() != null ? String.format("%.4f", doc.getScore()) : "N/A",
                    doc.getMetadata().getOrDefault("recipe_name", "N/A"),
                    doc.getMetadata().getOrDefault("category_name", "N/A"));
            log.info("    content: {}", contentPreview);
        }
    }
}
