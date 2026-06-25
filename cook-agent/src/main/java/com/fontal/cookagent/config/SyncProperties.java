package com.fontal.cookagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "cook.sync")
public class SyncProperties {

    /** 是否启用定时同步 */
    private boolean enabled = false;

    /** 定时 cron 表达式，默认每周一凌晨3点 */
    private String cron = "0 0 3 * * MON";
}