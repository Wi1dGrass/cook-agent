"use client";

import * as React from "react";
import { toast } from "sonner";
import { Bot, Loader2, Terminal, RotateCcw } from "lucide-react";

import { useChatStore, uid, type ChatMessage } from "@/lib/store/chat-store";
import { agentStream } from "@/lib/api/chat";
import { ApiError, friendlyMessage } from "@/lib/api/errors";
import { MessageList } from "@/components/chat/message-bubble";
import { ChatInput } from "@/components/chat/chat-input";

const AGENT_SUGGESTIONS = [
  "帮我查一下宫保鸡丁的做法，并对比它和鱼香肉丝的营养",
  "我想要一份冬季三菜一汤的搭配建议",
  "搜索含有牛肉和土豆的菜谱，并给我看图片",
];

function parseFinalReply(steps: string[]): string {
  for (let i = steps.length - 1; i >= 0; i--) {
    const s = steps[i];
    if (s.includes("无需行动") || s.startsWith("Step")) {
      const m = s.match(/[-—:]\s*(.+)$/s);
      if (m && !s.includes("工具") && !s.includes("执行失败")) return m[1].trim();
    }
  }
  return "";
}

export default function AgentPage() {
  const { messages, sending, addMessage, setSending, clear } = useChatStore();
  const [steps, setSteps] = React.useState<string[]>([]);
  const abortRef = React.useRef<AbortController | null>(null);
  const lastAssistantRef = React.useRef<string | null>(null);

  async function handleSend(text: string) {
    addMessage({
      id: uid(),
      role: "user",
      content: text,
      createdAt: Date.now(),
    });

    const assistantId = uid();
    addMessage({
      id: assistantId,
      role: "assistant",
      content: "",
      streaming: true,
      steps: [],
      createdAt: Date.now(),
    } as ChatMessage);
    lastAssistantRef.current = assistantId;

    const collectedSteps: string[] = [];
    setSteps(collectedSteps);
    setSending(true);

    const ctrl = new AbortController();
    abortRef.current = ctrl;

    const updateSteps = () => {
      useChatStore.getState().setStepsToLast([...collectedSteps]);
    };

    await agentStream(
      text,
      {
        onStep: (step) => {
          collectedSteps.push(step);
          updateSteps();
        },
        onError: (msg) => {
          toast.error(msg);
          useChatStore.getState().appendToLast(`\n\n> **Agent 出错：** ${msg}`);
        },
        onClose: () => {
          const reply = parseFinalReply(collectedSteps);
          if (reply) {
            useChatStore.getState().appendToLast(reply);
          } else if (collectedSteps.length > 0) {
            useChatStore.getState().appendToLast(
              "Agent 已完成执行，详见下方步骤。"
            );
          }
          setSending(false);
          abortRef.current = null;
        },
      },
      ctrl.signal
    );
  }

  function handleStop() {
    abortRef.current?.abort();
    setSending(false);
  }

  function handleReset() {
    if (sending) handleStop();
    clear();
    setSteps([]);
  }

  return (
    <div className="flex h-[calc(100svh-3.5rem)] flex-col">
      <div className="flex items-center justify-between border-b border-border/70 bg-background/40 px-4 py-2.5 backdrop-blur">
        <h1 className="flex items-center gap-2 text-sm font-medium">
          <span className="flex size-1.5 rounded-full bg-primary/70" />
          <Bot className="size-4 text-primary" />
          Agent 模式
          <span className="hidden text-muted-foreground sm:inline">· ReAct + 工具调用 · SSE 流式</span>
        </h1>
        <div className="flex items-center gap-3">
          {sending && (
            <span className="inline-flex items-center gap-1.5 rounded-full border border-primary/30 bg-primary/10 px-2.5 py-1 text-xs text-primary">
              <Loader2 className="size-3 animate-spin" />
              执行中（最多 20 步）
            </span>
          )}
          {messages.length > 0 && (
            <button
              onClick={handleReset}
              className="inline-flex items-center gap-1 rounded-full border border-border/60 bg-card/50 px-2.5 py-1 text-xs text-muted-foreground hover:text-foreground hover:border-primary/40 transition-colors cursor-pointer"
            >
              <RotateCcw className="size-3" />
              重置
            </button>
          )}
        </div>
      </div>

      <div className="flex-1 overflow-y-auto scrollbar-thin">
        {messages.length === 0 ? (
          <div className="mx-auto flex h-full max-w-3xl flex-col items-center justify-center px-4 py-10 text-center">
            <div className="relative flex size-16 items-center justify-center">
              <span className="absolute inset-0 rounded-2xl bg-primary/20 blur-xl" />
              <div className="relative flex size-16 items-center justify-center rounded-2xl bg-gradient-to-br from-primary to-primary/70 text-primary-foreground">
                <Bot className="size-8" />
              </div>
            </div>
            <h2 className="mt-6 text-2xl font-semibold tracking-tight">Agent 自主烹饪助手</h2>
            <p className="mt-2 max-w-md text-sm text-muted-foreground">
              Agent 会自主思考、调用 10 个工具（菜谱搜索、食材反查、营养、图片、网页搜索…）并逐步返回结果。
            </p>
            <div className="mt-8 flex w-full max-w-xl flex-col gap-2">
              {AGENT_SUGGESTIONS.map((s) => (
                <button
                  key={s}
                  onClick={() => handleSend(s)}
                  disabled={sending}
                  className="card-hover group flex items-start gap-3 rounded-xl border border-border bg-card/60 p-4 text-left backdrop-blur hover:border-primary/50 cursor-pointer disabled:opacity-50"
                >
                  <Terminal className="mt-0.5 size-4 shrink-0 text-primary transition-transform group-hover:scale-110" />
                  <span className="text-sm">{s}</span>
                </button>
              ))}
            </div>
          </div>
        ) : (
          <MessageList messages={messages} />
        )}
      </div>

      <ChatInput
        onSend={handleSend}
        loading={sending}
        disabled={sending}
        placeholder={sending ? "Agent 执行中，请等待完成…" : "给 Agent 一个任务…"}
      />
    </div>
  );
}
