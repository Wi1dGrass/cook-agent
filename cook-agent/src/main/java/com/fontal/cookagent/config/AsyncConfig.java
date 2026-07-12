package com.fontal.cookagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启用异步任务（会话标题生成等）和定时任务。
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}
