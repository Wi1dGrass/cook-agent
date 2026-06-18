package com.fontal.cookagent.rag.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "cook.rag")
public class RagProperties {

    /** 菜谱 Markdown 文件根目录 */
    private String documentRoot = "../CookLikeHOC";

    /** TokenTextSplitter 配置 */
    private TokenSplitter tokenSplitter = new TokenSplitter();

    /** 检索配置 */
    private Retrieval retrieval = new Retrieval();

    @Data
    public static class TokenSplitter {
        private int chunkSize = 800;
        private int minChunkSizeChars = 350;
        private int minChunkLengthToEmbed = 10;
        private int maxNumChunks = 5000;
        private boolean keepSeparator = true;
    }

    @Data
    public static class Retrieval {
        private int topK = 5;
        private double similarityThreshold = 0.50;
    }
}
