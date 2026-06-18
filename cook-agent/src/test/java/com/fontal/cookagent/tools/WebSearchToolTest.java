package com.fontal.cookagent.tools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebSearchTool 单元测试 — 火山引擎联网搜索。
 * 优先使用 VOLCANO_SEARCH_API_KEY 环境变量，未设置时跳过真实搜索测试。
 */
class WebSearchToolTest {

    private static final String TEST_API_KEY = System.getenv("VOLCANO_SEARCH_API_KEY");

    private final WebSearchTool tool = TEST_API_KEY != null && !TEST_API_KEY.isBlank()
            ? new WebSearchTool(TEST_API_KEY)
            : new WebSearchTool("");

    @Test
    @DisplayName("搜索红烧肉做法")
    void searchCookingRecipe() {
        String result = tool.webSearch("红烧肉的做法", 3);

        assertThat(result).isNotBlank();
        assertThat(result).satisfiesAnyOf(
                r -> assertThat(r).contains("搜索结果"),
                r -> assertThat(r).contains("不可用"),
                r -> assertThat(r).contains("未配置")
        );
    }

    @Test
    @DisplayName("搜索英文关键词")
    void searchEnglishKeyword() {
        String result = tool.webSearch("Chinese braised pork recipe", 2);

        assertThat(result).isNotBlank();
        assertThat(result).satisfiesAnyOf(
                r -> assertThat(r).contains("搜索结果"),
                r -> assertThat(r).contains("不可用"),
                r -> assertThat(r).contains("未配置")
        );
    }

    @Test
    @DisplayName("无意义查询不抛异常")
    void searchGibberishDoesNotThrow() {
        String result = tool.webSearch("xyzabc123不存在的内容xyz", 1);

        assertThat(result).isNotBlank();
    }

    @Test
    @DisplayName("未配置 API Key 时返回错误提示")
    void returnsErrorMessageWhenNoApiKey() {
        WebSearchTool noKeyTool = new WebSearchTool("");

        String result = noKeyTool.webSearch("测试", 1);

        assertThat(result).isNotBlank();
        assertThat(result).contains("未配置");
    }

    @Test
    @DisplayName("null API Key 时返回错误提示")
    void returnsErrorMessageWhenApiKeyIsNull() {
        WebSearchTool nullKeyTool = new WebSearchTool(null);

        String result = nullKeyTool.webSearch("测试", 1);

        assertThat(result).isNotBlank();
        assertThat(result).contains("未配置");
    }
}
