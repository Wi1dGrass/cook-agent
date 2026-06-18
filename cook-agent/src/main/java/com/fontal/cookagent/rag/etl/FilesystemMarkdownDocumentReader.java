package com.fontal.cookagent.rag.etl;

import com.fontal.cookagent.rag.properties.RagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Component
public class FilesystemMarkdownDocumentReader {

    private static final Logger log = LoggerFactory.getLogger(FilesystemMarkdownDocumentReader.class);

    private static final Set<String> EXCLUDE_DIRS = Set.of("docs", "docker_support", "images");
    private static final String README = "README.md";

    private final RagProperties ragProperties;
    private final CookRecipeMetadataExtractor metadataExtractor;

    public FilesystemMarkdownDocumentReader(RagProperties ragProperties,
                                            CookRecipeMetadataExtractor metadataExtractor) {
        this.ragProperties = ragProperties;
        this.metadataExtractor = metadataExtractor;
    }

    /** 加载所有菜谱 MD 文件为 Document 列表 */
    public List<Document> loadAll() {
        List<Document> allDocuments = new ArrayList<>();
        Path root = Paths.get(ragProperties.getDocumentRoot());

        if (!Files.exists(root)) {
            log.warn("文档根目录不存在: {}", root.toAbsolutePath());
            return allDocuments;
        }

        try (var dirs = Files.list(root)) {
            var categoryDirs = dirs
                    .filter(Files::isDirectory)
                    .filter(p -> !EXCLUDE_DIRS.contains(p.getFileName().toString()))
                    .toList();

            for (Path categoryDir : categoryDirs) {
                allDocuments.addAll(loadCategoryDirectory(categoryDir));
            }

        } catch (IOException e) {
            log.error("扫描文档目录失败: {}", root, e);
        }

        log.info("Loaded {} raw documents from filesystem", allDocuments.size());
        return allDocuments;
    }

    private List<Document> loadCategoryDirectory(Path categoryDir) throws IOException {
        List<Document> documents = new ArrayList<>();

        try (var files = Files.list(categoryDir)) {
            var mdFiles = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".md"))
                    .filter(p -> !README.equals(p.getFileName().toString()))
                    .toList();

            for (Path mdFile : mdFiles) {
                documents.addAll(loadMarkdownFile(mdFile));
            }
        }

        return documents;
    }

    private List<Document> loadMarkdownFile(Path mdFile) {
        Map<String, Object> fileMetadata = new HashMap<>();
        metadataExtractor.extract(mdFile).forEach(fileMetadata::put);

        MarkdownDocumentReaderConfig readerConfig = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(false)
                .withIncludeCodeBlock(false)
                .withIncludeBlockquote(false)
                .withAdditionalMetadata(fileMetadata)
                .build();

        MarkdownDocumentReader reader = new MarkdownDocumentReader(
                new FileSystemResource(mdFile.toFile()), readerConfig);

        List<Document> documents = reader.get();

        // 确保每个 Document 都带上元数据
        for (Document doc : documents) {
            fileMetadata.forEach(doc.getMetadata()::putIfAbsent);
        }

        return documents;
    }
}
