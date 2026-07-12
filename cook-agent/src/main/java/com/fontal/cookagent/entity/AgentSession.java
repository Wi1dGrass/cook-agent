package com.fontal.cookagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent 会话实体 — 支持 Agent 多轮对话 + 上下文压缩。
 * <p>
 * 存储完整 messageList（含工具调用消息）的 JSON，由 AgentSessionService 管理。
 * status：ACTIVE（可继续对话）/ CLOSED（已关闭，上下文已压缩）。
 */
@Data
@TableName("agent_session")
public class AgentSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String conversationId;

    /** 会话标题（LLM 生成，≤10 字） */
    private String title;

    /** ACTIVE / CLOSED */
    private String status;

    /** 完整消息列表 JSON（含工具调用） */
    private String messageList;

    private Integer currentStep;

    /** 是否已压缩上下文 */
    private Integer compressed;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
