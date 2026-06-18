package com.fontal.cookagent.tools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ImageSearchTool 测试 — 包含无 Key 降级路径 和 真实 Pexels API 调用。
 */
class ImageSearchToolTest {

    // ========== 单元测试：无 API Key 降级 ==========

    @Test
    @DisplayName("未配置 API Key 时返回错误提示")
    void returnsErrorMessageWhenNoApiKey() {
        ImageSearchTool tool = new ImageSearchTool("");

        String result = tool.searchImages("红烧肉", 2);

        assertThat(result).isNotBlank();
        assertThat(result).contains("未配置");
    }

    @Test
    @DisplayName("null API Key 时返回错误提示")
    void returnsErrorMessageWhenApiKeyIsNull() {
        ImageSearchTool tool = new ImageSearchTool(null);

        String result = tool.searchImages("清蒸鱼", 1);

        assertThat(result).isNotBlank();
        assertThat(result).contains("未配置");
    }

    @Test
    @DisplayName("count 参数超过 10 时自动截断不抛异常")
    void capsCountAtMax10() {
        ImageSearchTool tool = new ImageSearchTool("");

        String result = tool.searchImages("麻婆豆腐", 50);

        assertThat(result).isNotBlank();
    }

    // ========== 集成测试：真实 Pexels API（使用 application-local.yml 中的 Key）==========

    @Nested
    @SpringBootTest
    @ActiveProfiles("local")
    @DisplayName("真实 Pexels API 调用")
    class RealApiTest {

        @Autowired
        private ImageSearchTool imageSearchTool;

        @Test
        @DisplayName("搜索红烧肉图片返回有效结果")
        void searchRealImages() {
            String result = imageSearchTool.searchImages("红烧肉", 2);

            assertThat(result).isNotBlank();
            assertThat(result).contains("图片搜索");
            // 确认返回了图片URL
            assertThat(result).contains("http");
        }

        @Test
        @DisplayName("搜索英文关键词")
        void searchEnglishQuery() {
            String result = imageSearchTool.searchImages("braised pork", 1);

            assertThat(result).isNotBlank();
            assertThat(result).contains("图片搜索");
        }
    }
}
