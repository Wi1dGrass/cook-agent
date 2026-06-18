package com.fontal.cookagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "cook.chat.memory")
public class ChatMemoryProperties {

    /** 对话记忆文件存储路径 */
    private String storagePath = "./chat-memory";

    /** 过期对话清理天数 */
    private int evictionDays = 7;
}
