package com.fontal.cookagent.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话摘要 DTO — 用于会话列表 API（侧边栏 / 历史页）。
 */
@Data
public class SessionSummary {

    private String conversationId;

    private String title;

    /** 首条用户消息（title 为空时的回退显示） */
    private String firstQuery;

    /** 来源：CHAT / AGENT */
    private String channel;

    /** 消息条数 */
    private Integer messageCount;

    /** Agent 会话状态：ACTIVE / CLOSED（仅 AGENT 有值） */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime lastAt;
}
