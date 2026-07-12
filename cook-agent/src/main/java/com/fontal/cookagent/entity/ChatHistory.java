package com.fontal.cookagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_history")
public class ChatHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String conversationId;

    private String query;

    private String reply;

    /** 来源：CHAT / AGENT */
    private String channel;

    /** 会话标题（仅首条记录写入，LLM 生成） */
    private String title;

    private LocalDateTime createdAt;
}