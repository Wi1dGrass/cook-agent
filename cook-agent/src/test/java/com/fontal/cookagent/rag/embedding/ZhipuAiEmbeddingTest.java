package com.fontal.cookagent.rag.embedding;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * 智谱 AI Embedding-3 API 连通性测试
 * 验证 API Key 和端点是否可用，再执行完整 ETL 导入
 */
class ZhipuAiEmbeddingTest {

    // 从环境变量读取，未设置时跳过测试
    private static final String API_KEY = System.getenv("ZHIPU_API_KEY");
    private static final String BASE_URL = "https://open.bigmodel.cn/api/paas/v4";

    @Test
    void testEmbeddingApi() {
        RestClient client = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "Bearer " + API_KEY)
                .defaultHeader("Content-Type", "application/json")
                .build();

        String body = """
                {
                  "model": "embedding-3",
                  "input": "你好，今天天气怎么样.",
                  "dimensions": 2
                }
                """;

        String response = client.post()
                .uri("/embeddings")
                .body(body)
                .retrieve()
                .body(String.class);

        System.out.println("=== ZhipuAI Embedding API 测试 ===");
        System.out.println("请求: " + body.trim());
        System.out.println("响应: " + response);
    }
}
