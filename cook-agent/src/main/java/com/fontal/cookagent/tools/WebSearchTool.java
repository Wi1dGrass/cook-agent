package com.fontal.cookagent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * 火山引擎联网搜索 Tool — 免费额度 500 次/月，需 API Key。
 * API 文档：https://www.volcengine.com/docs/85508/1650263
 */
@Slf4j
@Component
public class WebSearchTool {

    private static final String API_URL = "https://open.feedcoopapi.com/search_api/web_search";
    private static final String TRAFFIC_TAG = "skill_web_search_common";

    private final RestClient restClient;
    private final ObjectMapper mapper;
    private final String apiKey;

    public WebSearchTool(@Value("${cook.search.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(API_URL)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("X-Traffic-Tag", TRAFFIC_TAG)
                .build();
        this.mapper = new ObjectMapper();
    }

    @Tool(description = "在互联网上搜索与烹饪、食材、菜品相关的实时信息。当菜谱知识库中没有答案时使用此工具。")
    public String webSearch(
            @ToolParam(description = "搜索关键词", required = true) String query,
            @ToolParam(description = "最大返回结果数，默认5") Integer maxResults) {

        if (apiKey == null || apiKey.isBlank()) {
            return "联网搜索暂未配置 API Key。请在配置文件中设置 cook.search.api-key。";
        }

        int limit = maxResults != null ? Math.min(maxResults, 10) : 5;

        try {
            // 构建请求体
            var body = mapper.createObjectNode();
            body.put("Query", query);
            body.put("SearchType", "web");
            body.put("Count", limit);
            body.put("NeedSummary", true);

            String response = restClient.post()
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = mapper.readTree(response);

            // 检查 API 错误
            JsonNode error = root.path("ResponseMetadata").path("Error");
            if (!error.isMissingNode()) {
                String code = error.path("Code").asText();
                String msg = error.path("Message").asText();
                log.warn("火山引擎搜索 API 错误 [{}]: {}", code, msg);
                return "联网搜索暂时不可用：" + msg;
            }

            JsonNode result = root.path("Result");
            JsonNode webResults = result.path("WebResults");

            if (webResults.isMissingNode() || !webResults.isArray() || webResults.isEmpty()) {
                return "未找到「" + query + "」的相关搜索结果。";
            }

            List<String> results = new ArrayList<>();
            for (JsonNode item : webResults) {
                String title = item.path("Title").asText();
                String siteName = item.path("SiteName").asText();
                String url = item.path("Url").asText();
                String summary = item.path("Summary").asText();

                StringBuilder sb = new StringBuilder();
                sb.append("【").append(title).append("】");
                if (!siteName.isBlank()) {
                    sb.append(" - ").append(siteName);
                }
                sb.append("\n");
                if (!summary.isBlank()) {
                    sb.append(summary);
                    if (summary.length() >= 200) {
                        sb.append("...");
                    }
                    sb.append("\n");
                }
                if (!url.isBlank()) {
                    sb.append(url).append("\n");
                }
                results.add(sb.toString().strip());
            }

            if (results.isEmpty()) {
                return "未找到「" + query + "」的相关搜索结果。";
            }

            return results.stream()
                    .limit(limit)
                    .map(r -> "- " + r)
                    .reduce("搜索结果：\n", (a, b) -> a + b + "\n");

        } catch (Exception e) {
            log.warn("火山引擎搜索失败: {}", e.getMessage());
            return "联网搜索暂时不可用：" + e.getMessage();
        }
    }
}
