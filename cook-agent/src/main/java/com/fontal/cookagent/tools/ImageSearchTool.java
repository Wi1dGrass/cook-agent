package com.fontal.cookagent.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Pexels 图片搜索 Tool — 搜索菜品相关图片。
 */
@Slf4j
@Component
public class ImageSearchTool {

    private final RestClient restClient;
    private final ObjectMapper mapper;
    private final String apiKey;

    public ImageSearchTool(@Value("${cook.pexels.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.pexels.com/v1")
                .build();
        this.mapper = new ObjectMapper();
    }

    @Tool(description = "搜索菜品、食材、烹饪相关的图片。返回图片URL和摄影师信息。")
    public String searchImages(
            @ToolParam(description = "搜索关键词，例如'红烧肉'、'清蒸鱼'、'麻婆豆腐'", required = true) String query,
            @ToolParam(description = "返回图片数量，默认3，最大10") Integer count) {

        if (apiKey == null || apiKey.isBlank()) {
            return "Pexels API Key 未配置，无法搜索图片。请在配置中设置 cook.pexels.api-key。";
        }

        int perPage = Math.min(count != null ? count : 3, 10);
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);

        try {
            String response = restClient.get()
                    .uri("/search?query={q}&per_page={n}&locale=zh-CN", encoded, perPage)
                    .header("Authorization", apiKey)
                    .retrieve()
                    .body(String.class);

            JsonNode root = mapper.readTree(response);
            JsonNode photos = root.get("photos");
            if (photos == null || photos.isEmpty()) {
                return "未找到「" + query + "」的相关图片。";
            }

            List<String> results = new ArrayList<>();
            for (JsonNode photo : photos) {
                String url = photo.get("src").get("medium").asText();
                String photographer = photo.get("photographer").asText();
                String alt = photo.has("alt") ? photo.get("alt").asText() : query;
                results.add(String.format("%s (摄影: %s) %s", alt, photographer, url));
            }

            return "「" + query + "」的图片搜索结果：\n" + String.join("\n", results);

        } catch (Exception e) {
            log.warn("Pexels 图片搜索失败: {}", e.getMessage());
            return "图片搜索暂时不可用：" + e.getMessage();
        }
    }
}
