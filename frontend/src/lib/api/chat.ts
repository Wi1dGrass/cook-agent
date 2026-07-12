"use client";

import { clientApiFetch } from "./client-fetch";
import type {
  ChatNewResponse,
  ChatSendResponse,
  AgentChatResponse,
  ChatHistory,
  SessionSummary,
  AgentSessionResponse,
  AgentSessionListItem,
} from "./types";

export async function chatNew(message: string) {
  return clientApiFetch<ChatNewResponse>("/chat/new", {
    method: "POST",
    body: { message },
  });
}

export async function chatSend(conversationId: string, message: string) {
  return clientApiFetch<ChatSendResponse>("/chat/send", {
    method: "POST",
    body: { conversationId, message },
  });
}

export async function agentChat(message: string, conversationId?: string) {
  return clientApiFetch<AgentChatResponse>("/agent/chat", {
    method: "POST",
    body: conversationId ? { message, conversationId } : { message },
  });
}

export async function listHistory(limit = 20) {
  return clientApiFetch<ChatHistory[]>("/user/history", {
    query: { limit },
  });
}

export async function getConversation(conversationId: string) {
  return clientApiFetch<ChatHistory[]>(`/user/history/${conversationId}`);
}

export async function deleteConversation(conversationId: string) {
  return clientApiFetch<{ conversationId: string; deleted: boolean }>(
    `/user/history/${conversationId}`,
    { method: "DELETE" }
  );
}

/** 服务端分组的会话列表 */
export async function listSessions() {
  return clientApiFetch<SessionSummary[]>("/user/sessions");
}

/** 加载 Agent 会话完整消息（含工具调用步骤） */
export async function getAgentSession(conversationId: string) {
  return clientApiFetch<AgentSessionResponse>(`/agent/session/${conversationId}`);
}

/** 关闭 Agent 会话并压缩上下文 */
export async function closeAgentSession(conversationId: string) {
  return clientApiFetch<{ conversationId: string; status: string; compressed: boolean }>(
    `/agent/session/${conversationId}/close`,
    { method: "POST" }
  );
}

/** 删除 Agent 会话 */
export async function deleteAgentSession(conversationId: string) {
  return clientApiFetch<{ conversationId: string; deleted: boolean }>(
    `/agent/session/${conversationId}`,
    { method: "DELETE" }
  );
}

/** 列出 Agent 会话 */
export async function listAgentSessions() {
  return clientApiFetch<AgentSessionListItem[]>("/agent/sessions");
}

export interface AgentStreamHandlers {
  onStep: (step: string) => void;
  /** 收到最终总结（后端发送「【最终总结】\n...」格式的 data 事件） */
  onSummary?: (summary: string) => void;
  onError?: (msg: string) => void;
  onClose?: () => void;
}

const SUMMARY_PREFIX = "【最终总结】";

export async function agentStream(
  message: string,
  handlers: AgentStreamHandlers,
  signal?: AbortSignal,
  conversationId?: string
): Promise<void> {
  // 通过 Next.js SSE 代理路由（注入 JWT 认证）
  const url = `/api/agent/stream?message=${encodeURIComponent(message)}${
    conversationId ? `&conversationId=${encodeURIComponent(conversationId)}` : ""
  }`;

  let res: Response;
  try {
    res = await fetch(url, {
      method: "GET",
      headers: { Accept: "text/event-stream" },
      signal,
    });
  } catch (e) {
    handlers.onError?.((e as Error).message ?? "连接失败");
    return;
  }

  if (!res.ok || !res.body) {
    handlers.onError?.(`Agent 流式请求失败：HTTP ${res.status}`);
    return;
  }

  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  let summaryReceived = false;
  let closed = false;
  let dataBuffer: string[] = [];

  const flushData = () => {
    if (dataBuffer.length === 0) return;
    const raw = dataBuffer.join("\n");
    dataBuffer = [];
    if (raw.startsWith(SUMMARY_PREFIX)) {
      summaryReceived = true;
      handlers.onSummary?.(raw.slice(SUMMARY_PREFIX.length).replace(/^\n/, ""));
    } else {
      handlers.onStep(raw);
    }
  };

  const triggerClose = () => {
    if (closed) return;
    closed = true;
    handlers.onClose?.();
  };

  try {
    for (;;) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });

      let idx: number;
      while ((idx = buffer.indexOf("\n")) !== -1) {
        const line = buffer.slice(0, idx);
        buffer = buffer.slice(idx + 1);
        const trimmed = line.trim();

        if (trimmed === "") {
          flushData();
          continue;
        }
        if (trimmed.startsWith("data:")) {
          let data = trimmed.slice(5);
          if (data.startsWith(" ")) data = data.slice(1);
          dataBuffer.push(data);
        } else if (trimmed.startsWith("event:")) {
          flushData();
          const ev = trimmed.slice(6).trim();
          if (ev === "complete") triggerClose();
        }
      }
    }
    if (buffer.trim().startsWith("data:")) {
      let data = buffer.trim().slice(5);
      if (data.startsWith(" ")) data = data.slice(1);
      dataBuffer.push(data);
    }
    flushData();
  } catch (e) {
    if ((e as Error).name !== "AbortError") {
      handlers.onError?.((e as Error).message ?? "流读取中断");
    }
  } finally {
    triggerClose();
    try {
      reader.releaseLock();
    } catch {
      /* ignore */
    }
  }
}
