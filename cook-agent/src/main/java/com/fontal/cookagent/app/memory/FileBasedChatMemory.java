package com.fontal.cookagent.app.memory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.fontal.cookagent.config.ChatMemoryProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 基于 Kryo 二进制序列化的文件持久化 ChatMemoryRepository（已停用，保留备查）。
 * <p>
 * 默认由 {@link MysqlChatMemoryRepository} 替代。若需启用本实现，设置
 * {@code cook.chat.memory.backend=file}。
 */
@ConditionalOnProperty(name = "cook.chat.memory.backend", havingValue = "file")
public class FileBasedChatMemory implements ChatMemoryRepository {

    private final Path storageDir;

    /** L1 内存缓存 — 减少 Kryo 反序列化开销 */
    private final Cache<String, List<Message>> cache;

    private static final Kryo kryo = new Kryo();

    static {
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    public FileBasedChatMemory(ChatMemoryProperties properties) {
        this.storageDir = Path.of(properties.getStoragePath());
        this.cache = Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterAccess(Duration.ofHours(1))
                .build();
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new UncheckedIOException("无法创建聊天记忆目录: " + storageDir, e);
        }
    }

    @Override
    public List<String> findConversationIds() {
        File[] files = storageDir.toFile().listFiles((d, name) -> name.endsWith(".kryo"));
        if (files == null) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (File file : files) {
            ids.add(file.getName().replace(".kryo", ""));
        }
        return ids;
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        // L1 缓存命中
        List<Message> cached = cache.getIfPresent(conversationId);
        if (cached != null) {
            return cached;
        }

        File file = getConversationFile(conversationId);
        if (!file.exists()) {
            return List.of();
        }
        try (Input input = new Input(new FileInputStream(file))) {
            List<Message> messages = kryo.readObject(input, ArrayList.class);
            cache.put(conversationId, messages);
            return messages;
        } catch (IOException e) {
            return List.of();
        }
    }

    /**
     * 全量覆盖保存 —— 窗口裁剪由 MessageWindowChatMemory 负责。
     * MessageWindowChatMemory 调用此方法时传入的已经是裁剪后的消息列表。
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        try (Output output = new Output(new FileOutputStream(file))) {
            kryo.writeObject(output, new ArrayList<>(messages));
        } catch (IOException e) {
            throw new UncheckedIOException("保存对话记忆失败: " + conversationId, e);
        }
        // 同步更新 L1 缓存
        cache.put(conversationId, new ArrayList<>(messages));
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        File file = getConversationFile(conversationId);
        if (file.exists()) {
            file.delete();
        }
        cache.invalidate(conversationId);
    }

    /**
     * 清理过期对话文件。
     * 由外部调度触发（非自动），遍历存储目录，删除超过 evictionDays 天未修改的文件。
     */
    public int evictExpired(int evictionDays) {
        File[] files = storageDir.toFile().listFiles((d, name) -> name.endsWith(".kryo"));
        if (files == null) {
            return 0;
        }
        long cutoff = System.currentTimeMillis() - (long) evictionDays * 24 * 3600 * 1000;
        int count = 0;
        for (File file : files) {
            if (file.lastModified() < cutoff) {
                String convId = file.getName().replace(".kryo", "");
                cache.invalidate(convId);
                file.delete();
                count++;
            }
        }
        return count;
    }

    private File getConversationFile(String conversationId) {
        return storageDir.resolve(conversationId + ".kryo").toFile();
    }
}
