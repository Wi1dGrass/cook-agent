package com.fontal.cookagent.rag.etl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!pgvector")  // pgvector 下数据已在 PGVector 中，启动时无需重新导入
public class EtlPipelineRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EtlPipelineRunner.class);

    private final CookEtlPipeline etlPipeline;

    @Value("${cook.rag.etl.auto-startup:true}")
    private boolean autoStartup;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    public EtlPipelineRunner(CookEtlPipeline etlPipeline) {
        this.etlPipeline = etlPipeline;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!autoStartup) {
            log.info("RAG ETL auto-startup disabled (cook.rag.etl.auto-startup=false), skipping. Use POST /api/rag/etl/run to import manually.");
            return;
        }

        // PGVector 已有数据则跳过导入
        if (jdbcTemplate != null && hasExistingData()) {
            log.info("Vector store already has data, skipping ETL. Use POST /api/rag/etl/run to re-import.");
            return;
        }

        log.info("Starting RAG ETL pipeline on application startup...");
        try {
            etlPipeline.run();
        } catch (Exception e) {
            log.error("RAG ETL pipeline failed", e);
        }
    }

    private boolean hasExistingData() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM vector_store", Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            log.debug("Cannot check vector_store table (may not exist yet): {}", e.getMessage());
            return false;
        }
    }
}
