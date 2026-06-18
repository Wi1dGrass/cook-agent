package com.fontal.cookagent.rag.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.*;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 智谱 AI Embedding-3 模型适配器。
 * 调用 open.bigmodel.cn 的 /api/paas/v4/embeddings 端点。
 * 响应格式与 OpenAI 兼容，但 URL 路径不同，故自定义实现。
 */
public class ZhipuAiEmbeddingModel implements EmbeddingModel {

    private static final Logger log = LoggerFactory.getLogger(ZhipuAiEmbeddingModel.class);

    private static final String DEFAULT_BASE_URL = "https://open.bigmodel.cn/api/paas/v4";

    /** ZhipuAI Embedding-3 单次最大输入条数 */
    private static final int MAX_BATCH_SIZE = 64;

    private final RestClient restClient;
    private final String model;
    private final Integer dimensions;

    public ZhipuAiEmbeddingModel(String apiKey, String model, Integer dimensions) {
        this(apiKey, DEFAULT_BASE_URL, model, dimensions);
    }

    public ZhipuAiEmbeddingModel(String apiKey, String baseUrl, String model, Integer dimensions) {
        Assert.hasText(apiKey, "apiKey must not be empty");
        this.model = model;
        this.dimensions = dimensions;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> inputs = request.getInstructions();

        // 超 64 条则分批调用
        if (inputs.size() > MAX_BATCH_SIZE) {
            return callInBatches(inputs);
        }

        return doCall(inputs);
    }

    private EmbeddingResponse callInBatches(List<String> allInputs) {
        List<Embedding> allEmbeddings = new ArrayList<>();
        int totalBatches = (allInputs.size() + MAX_BATCH_SIZE - 1) / MAX_BATCH_SIZE;

        for (int i = 0; i < allInputs.size(); i += MAX_BATCH_SIZE) {
            int batchNum = i / MAX_BATCH_SIZE + 1;
            List<String> batch = allInputs.subList(i, Math.min(i + MAX_BATCH_SIZE, allInputs.size()));
            log.info("ZhipuAI embedding batch {}/{}: {} inputs", batchNum, totalBatches, batch.size());

            EmbeddingResponse batchResp = doCall(batch);

            // 修正分批后的 index，保持全局递增
            for (Embedding emb : batchResp.getResults()) {
                allEmbeddings.add(new Embedding(emb.getOutput(), allEmbeddings.size()));
            }
        }

        return new EmbeddingResponse(allEmbeddings, new EmbeddingResponseMetadata());
    }

    private EmbeddingResponse doCall(List<String> inputs) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("model", this.model);
        body.put("input", inputs.size() == 1 ? inputs.get(0) : inputs);
        if (this.dimensions != null) {
            body.put("dimensions", this.dimensions);
        }

        log.info("ZhipuAI embedding: model={}, inputs={}, dim={}", model, inputs.size(), dimensions);

        @SuppressWarnings("unchecked")
        Map<String, Object> resp = restClient.post()
                .uri("/embeddings")
                .body(body)
                .retrieve()
                .body(Map.class);

        log.info("ZhipuAI embedding done: usage={}", resp.get("usage"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) resp.get("data");
        List<Embedding> embeddings = new ArrayList<>();

        if (dataList != null) {
            for (Map<String, Object> item : dataList) {
                @SuppressWarnings("unchecked")
                List<Number> embList = (List<Number>) item.get("embedding");
                int index = ((Number) item.get("index")).intValue();
                float[] vector = new float[embList.size()];
                for (int i = 0; i < embList.size(); i++) {
                    vector[i] = embList.get(i).floatValue();
                }
                embeddings.add(new Embedding(vector, index));
            }
        }

        return new EmbeddingResponse(embeddings, new EmbeddingResponseMetadata());
    }

    @Override
    public float[] embed(String text) {
        return EmbeddingModel.super.embed(text);
    }

    @Override
    public float[] embed(Document document) {
        return new float[0];
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        return EmbeddingModel.super.embed(texts);
    }

    @Override
    public List<float[]> embed(List<Document> documents, EmbeddingOptions options, BatchingStrategy batchingStrategy) {
        return EmbeddingModel.super.embed(documents, options, batchingStrategy);
    }

    @Override
    public EmbeddingResponse embedForResponse(List<String> texts) {
        return EmbeddingModel.super.embedForResponse(texts);
    }

    @Override
    public int dimensions() {
        return EmbeddingModel.super.dimensions();
    }
}
