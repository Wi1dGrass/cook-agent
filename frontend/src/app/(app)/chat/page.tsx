"use client";

import * as React from "react";
import { toast } from "sonner";
import { Flame, Carrot, CalendarDays, Apple, Plus } from "lucide-react";

import { useChatStore, uid } from "@/lib/store/chat-store";
import { chatNew, chatSend } from "@/lib/api/chat";
import { ApiError, friendlyMessage } from "@/lib/api/errors";
import { MessageList } from "@/components/chat/message-bubble";
import { ChatInput } from "@/components/chat/chat-input";
import { NewChatHero } from "@/components/chat/new-chat-hero";

const SUGGESTIONS = [
  { icon: Flame, text: "红烧肉怎么做？请给出详细步骤" },
  { icon: Carrot, text: "我冰箱里有鸡肉和土豆，能做什么菜？" },
  { icon: CalendarDays, text: "今天冬天，推荐几道应季暖身汤" },
  { icon: Apple, text: "番茄炒蛋的营养成分是什么？" },
];

export default function ChatPage() {
  const {
    conversationId,
    messages,
    sending,
    setConversationId,
    addMessage,
    setSending,
    clear,
  } = useChatStore();

  async function handleSend(text: string) {
    addMessage({
      id: uid(),
      role: "user",
      content: text,
      createdAt: Date.now(),
    });
    setSending(true);

    const assistantId = uid();
    addMessage({
      id: assistantId,
      role: "assistant",
      content: "",
      streaming: true,
      createdAt: Date.now(),
    });

    try {
      if (conversationId) {
        const res = await chatSend(conversationId, text);
        useChatStore.getState().appendToLast(res.reply);
      } else {
        const res = await chatNew(text);
        setConversationId(res.conversationId);
        useChatStore.getState().appendToLast(res.reply);
      }
    } catch (e) {
      const msg = e instanceof ApiError ? friendlyMessage(e) : "请求失败";
      useChatStore.getState().appendToLast(`\n\n> **出错：** ${msg}`);
      toast.error(msg);
    } finally {
      setSending(false);
    }
  }

  function handleNewChat() {
    clear();
  }

  const isEmpty = messages.length === 0;

  return (
    <div className="flex h-[calc(100svh-3.5rem)] flex-col">
      <div className="flex items-center justify-between border-b border-border/70 bg-background/40 px-4 py-2.5 backdrop-blur">
        <h1 className="flex items-center gap-2 text-sm font-medium">
          <span className="flex size-1.5 rounded-full bg-primary/70" />
          AI 对话
          {conversationId && <span className="text-muted-foreground">· RAG 检索增强</span>}
        </h1>
        {messages.length > 0 && (
          <button
            onClick={handleNewChat}
            className="inline-flex items-center gap-1 rounded-full border border-border/60 bg-card/50 px-2.5 py-1 text-xs text-muted-foreground hover:text-foreground hover:border-primary/40 transition-colors cursor-pointer"
          >
            <Plus className="size-3" />
            新对话
          </button>
        )}
      </div>

      <div className="flex-1 overflow-y-auto scrollbar-thin">
        {isEmpty ? (
          <NewChatHero onPick={handleSend} suggestions={SUGGESTIONS} />
        ) : (
          <MessageList messages={messages} />
        )}
      </div>

      <ChatInput onSend={handleSend} loading={sending} placeholder="问菜谱、查食材、聊烹饪…" />
    </div>
  );
}
