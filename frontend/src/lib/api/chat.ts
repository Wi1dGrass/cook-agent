"use client";

import { clientApiFetch } from "./client-fetch";
import type {
  ChatNewResponse,
  ChatSendResponse,
  AgentChatResponse,
  ChatHistory,
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

export async function agentChat(message: string) {
  return clientApiFetch<AgentChatResponse>("/agent/chat", {
    method: "POST",
    body: { message },
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
  signal?: AbortSignal
): Promise<void> {
  const base = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8088/api";
  const url = `${base}/agent/chat/stream?message=${encodeURIComponent(message)}`;

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
