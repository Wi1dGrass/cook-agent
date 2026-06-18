package com.fontal.cookagent.rag.etl;

import com.fontal.cookagent.rag.document.DocumentMetadataKeys;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Component
public class CookRecipeMetadataExtractor {

    /**
     * 从 MD 文件路径提取烹饪元数据。
     * 例：../CookLikeHOC/炒菜/什锦蛋炒饭.md
     * → { recipe_name: "什锦蛋炒饭", category_name: "炒菜", category_dir: "炒菜" }
     */
    public Map<String, String> extract(Path filePath) {
        Map<String, String> metadata = new HashMap<>();

        Path fileName = filePath.getFileName();
        if (fileName != null) {
            String name = fileName.toString().replace(".md", "");
            metadata.put(DocumentMetadataKeys.RECIPE_NAME, name);
        }

        Path parent = filePath.getParent();
        if (parent != null) {
            String categoryDir = parent.getFileName().toString();
            metadata.put(DocumentMetadataKeys.CATEGORY_DIR, categoryDir);
            metadata.put(DocumentMetadataKeys.CATEGORY_NAME, categoryDir);
        }

        metadata.put(DocumentMetadataKeys.SOURCE_FILE, filePath.toString());

        return metadata;
    }
}
