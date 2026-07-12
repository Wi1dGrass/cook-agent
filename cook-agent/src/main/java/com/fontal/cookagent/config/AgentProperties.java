package com.fontal.cookagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Agent 相关配置（多轮会话 + 上下文压缩）。
 */
@Data
@ConfigurationProperties(prefix = "cook.agent")
public class AgentProperties {

    /** 上下文压缩阈值（token 估算值），超过则自动压缩。默认 200k。 */
    private int contextMaxTokens = 200_000;

    /** 自动压缩时保留的最近轮数（不压缩最近 N 轮的完整上下文）。 */
    private int keepRecentTurns = 1;

    /** 会话标题最大长度。 */
    private int titleMaxLength = 10;
}
