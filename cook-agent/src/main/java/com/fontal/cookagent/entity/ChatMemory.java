package com.fontal.cookagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天记忆实体 — 存储普通对话的完整 LLM 消息（替代 Kryo 文件）。
 * <p>
 * 由 MysqlChatMemoryRepository 使用，配合 MessageWindowChatMemory 实现滑动窗口记忆。
 * 每行对应一条 Message（USER/SYSTEM/ASSISTANT/TOOL_RESPONSE）。
 */
@Data
@TableName("chat_memory")
public class ChatMemory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String conversationId;

    /** 消息在对话中的顺序索引 */
    private Integer seqId;

    /** USER / SYSTEM / ASSISTANT / TOOL_RESPONSE */
    private String messageType;

    private String content;

    /** AssistantMessage 的工具调用 JSON */
    private String toolCallsJson;

    /** ToolResponseMessage 的工具响应 JSON */
    private String toolResponsesJson;

    private LocalDateTime createdAt;
}
