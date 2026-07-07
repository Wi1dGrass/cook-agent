"use client";

import { create } from "zustand";

export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  steps?: string[];
  /** 是否正在打字机输出中 */
  streaming?: boolean;
  /** 是否正在等待响应（尚未拿到内容） */
  pending?: boolean;
  createdAt: number;
}

interface ChatState {
  conversationId: string | null;
  messages: ChatMessage[];
  sending: boolean;
  setConversationId: (id: string | null) => void;
  addMessage: (m: ChatMessage) => void;
  /** 把完整内容写入最后一条 assistant 消息，并标记为打字机输出中 */
  setFinalContent: (content: string) => void;
  /** 追加文本到最后一条 assistant 消息（用于错误等场景） */
  appendToLast: (delta: string) => void;
  /** 标记最后一条消息打字机结束 */
  finishStreaming: () => void;
  setStepsToLast: (steps: string[]) => void;
  setSending: (v: boolean) => void;
  clear: () => void;
  loadConversation: (id: string, messages: ChatMessage[]) => void;
}

let seq = 0;
function uid() {
  seq += 1;
  return `m_${Date.now()}_${seq}`;
}

export const useChatStore = create<ChatState>((set) => ({
  conversationId: null,
  messages: [],
  sending: false,
  setConversationId: (id) => set({ conversationId: id }),
  addMessage: (m) => set((s) => ({ messages: [...s.messages, m] })),
  setFinalContent: (content) =>
    set((s) => {
      if (s.messages.length === 0) return s;
      const last = s.messages[s.messages.length - 1];
      if (last.role !== "assistant") return s;
      const msgs = s.messages.slice();
      msgs[msgs.length - 1] = {
        ...last,
        content,
        streaming: true,
        pending: false,
      };
      return { messages: msgs };
    }),
  appendToLast: (delta) =>
    set((s) => {
      if (s.messages.length === 0) return s;
      const last = s.messages[s.messages.length - 1];
      if (last.role !== "assistant") return s;
      const msgs = s.messages.slice();
      msgs[msgs.length - 1] = { ...last, content: last.content + delta };
      return { messages: msgs };
    }),
  finishStreaming: () =>
    set((s) => {
      if (s.messages.length === 0) return s;
      const msgs = s.messages.slice();
      const last = msgs[msgs.length - 1];
      msgs[msgs.length - 1] = { ...last, streaming: false, pending: false };
      return { messages: msgs };
    }),
  setStepsToLast: (steps) =>
    set((s) => {
      if (s.messages.length === 0) return s;
      const msgs = s.messages.slice();
      const last = msgs[msgs.length - 1];
      msgs[msgs.length - 1] = { ...last, steps, streaming: false };
      return { messages: msgs };
    }),
  setSending: (v) => set({ sending: v }),
  clear: () => set({ conversationId: null, messages: [] }),
  loadConversation: (id, messages) => set({ conversationId: id, messages }),
}));

export { uid };
