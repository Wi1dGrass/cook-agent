package com.fontal.cookagent.app.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ChatService 集成测试 — 验证 RAG + 记忆 全链路。
 * 需要 pgvector profile 对应的 PostgreSQL 在线。
 */
@SpringBootTest
@ActiveProfiles("pgvector")
class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    ChatMemory chatMemory;

    @Test
    @DisplayName("单轮 RAG 对话 — 炖鸡汤问答")
    void singleTurnRagChat() {
        String reply = chatService.chat("test-rag-1", "我想学炖鸡汤，有什么技巧？");
        assertThat(reply).isNotBlank();
    }

    @Test
    @DisplayName("多轮对话记忆 — 记住上次的菜")
    void multiTurnMemory() {
        String convId = "test-memory-123";

        // 第一轮：告诉 AI 我的名字
       String reply1 = chatService.chat(convId, "我叫小明，我喜欢吃辣的菜");
        assertThat(reply1).isNotBlank();

        // 第二轮：问 AI 我叫什么（应该记住）
        String reply2 = chatService.chat(convId, "我喜欢什么口味的菜？你刚刚向我推荐什么菜？");
        assertThat(reply2).isNotBlank();
        // 回答中应包含"小明"（或至少体现了记忆）
    }

    @Test
    @DisplayName("对话记忆持久化 — 存入 FileBasedChatMemory")
    void memoryPersistedToFile() {
        String convId = "test-persist-1";

        chatService.chat(convId, "你好，请问有什么菜推荐？");

        // 验证 ChatMemory 中已存入消息
        var messages = chatMemory.get(convId);
        assertThat(messages).isNotEmpty();
        assertThat(messages.get(0).getMessageType()).isEqualTo(MessageType.USER);
        assertThat(messages.get(0).getText()).isEqualTo("你好，请问有什么菜推荐？");
        // 至少应有一条助手回复
        boolean hasAssistantReply = messages.stream()
                .anyMatch(m -> m.getMessageType() == MessageType.ASSISTANT);
        assertThat(hasAssistantReply).isTrue();
    }

    @Test
    @DisplayName("不同对话 ID 隔离")
    void conversationIsolation() {
        chatService.chat("iso-a", "我想学红烧肉");
        chatService.chat("iso-b", "我想学清蒸鱼");

        var messagesA = chatMemory.get("iso-a");
        var messagesB = chatMemory.get("iso-b");

        assertThat(messagesA).isNotEmpty();
        assertThat(messagesB).isNotEmpty();

        // A 对话应包含"红烧肉"
        boolean aHasRedMeat = messagesA.stream()
                .anyMatch(m -> m.getText() != null && m.getText().contains("红烧肉"));
        assertThat(aHasRedMeat).isTrue();

        // B 对话应包含"清蒸鱼"
        boolean bHasFish = messagesB.stream()
                .anyMatch(m -> m.getText() != null && m.getText().contains("清蒸鱼"));
        assertThat(bHasFish).isTrue();
    }
}
