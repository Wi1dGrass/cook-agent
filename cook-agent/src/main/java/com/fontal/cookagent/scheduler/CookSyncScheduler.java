package com.fontal.cookagent.scheduler;

import com.fontal.cookagent.rag.etl.CookEtlPipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * CookLikeHOC 数据定时同步任务。
 *
 * <p>启用开关 {@code cook.sync.enabled=true}；cron 来自 {@code cook.sync.cron}。
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "cook.sync", name = "enabled", havingValue = "true")
public class CookSyncScheduler {

    private final CookEtlPipeline etlPipeline;

    /** 定时同步执行体 — 增量导入 CookLikeHOC 菜谱到向量库 */
    @Scheduled(cron = "${cook.sync.cron:0 0 3 * * MON}")
    public void syncRun() {
        try {
            log.info("[定时任务] 开始 CookLikeHOC 数据同步");
            long start = System.currentTimeMillis();
            etlPipeline.run(false);
            log.info("[定时任务] 同步完成，耗时 {} ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("[定时任务] 同步失败", e);
        }
    }
}