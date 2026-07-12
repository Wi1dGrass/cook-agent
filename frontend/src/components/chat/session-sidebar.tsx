"use client";

import * as React from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { Plus, MessageSquare, Loader2, Bot, Trash2 } from "lucide-react";
import { toast } from "sonner";

import { listSessions, deleteConversation, deleteAgentSession } from "@/lib/api/chat";
import { useChatStore, uid, type ChatMessage } from "@/lib/store/chat-store";
import { getConversation } from "@/lib/api/chat";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { formatDate, truncate } from "@/lib/utils/format";
import { friendlyMessage, ApiError } from "@/lib/api/errors";
import type { SessionSummary } from "@/lib/api/types";

interface SessionSidebarProps {
  /** "CHAT" 或 "AGENT"，决定筛选哪些会话 */
  channel: "CHAT" | "AGENT";
  /** 当前会话 ID（高亮显示） */
  activeConversationId?: string | null;
  /** 选择会话后的回调（若不传则自动跳转到对应页面） */
  onSelect?: (conversationId: string, messages: ChatMessage[]) => void;
}

/**
 * 会话侧边栏 — 展示用户的历史会话列表，支持点击续聊。
 * 用于聊天页和 Agent 页的左侧面板。
 */
export function SessionSidebar({ channel, activeConversationId, onSelect }: SessionSidebarProps) {
  const router = useRouter();
  const qc = useQueryClient();
  const { loadConversation, clear } = useChatStore();

  const { data, isFetching, error } = useQuery({
    queryKey: ["sessions"],
    queryFn: () => listSessions(),
    staleTime: 10_000,
  });

  const sessions = React.useMemo(() => {
    if (!data) return [] as SessionSummary[];
    return data.filter((s) => s.channel === channel);
  }, [data, channel]);

  async function handleSelect(s: SessionSummary) {
    try {
      const full = await getConversation(s.conversationId);
      const messages: ChatMessage[] = [];
      for (const h of full) {
        messages.push({
          id: uid(),
          role: "user" as const,
          content: h.query,
          createdAt: new Date(h.createdAt).getTime(),
        });
        if (h.reply) {
          messages.push({
            id: uid(),
            role: "assistant" as const,
            content: h.reply,
            createdAt: new Date(h.createdAt).getTime() + 1,
          });
        }
      }

      if (onSelect) {
        onSelect(s.conversationId, messages);
      } else {
        loadConversation(s.conversationId, messages);
        router.push(channel === "AGENT" ? "/agent" : "/chat");
      }
    } catch (e) {
      toast.error(e instanceof ApiError ? friendlyMessage(e) : "加载会话失败");
    }
  }

  async function handleDelete(e: React.MouseEvent, s: SessionSummary) {
    e.stopPropagation();
    try {
      if (channel === "AGENT") {
        await deleteAgentSession(s.conversationId);
      }
      await deleteConversation(s.conversationId);
      qc.invalidateQueries({ queryKey: ["sessions"] });
      if (s.conversationId === activeConversationId) {
        clear();
      }
      toast.success("对话已删除");
    } catch (e2) {
      toast.error(e2 instanceof ApiError ? friendlyMessage(e2) : "删除失败");
    }
  }

  function handleNew() {
    clear();
    if (!onSelect) {
      router.push(channel === "AGENT" ? "/agent" : "/chat");
    }
  }

  return (
    <div className="flex h-full flex-col border-r border-border/60 bg-card/30">
      <div className="flex items-center justify-between px-3 py-2.5">
        <span className="text-xs font-medium text-muted-foreground">
          {channel === "AGENT" ? "Agent 会话" : "对话历史"}
        </span>
        <Button
          variant="ghost"
          size="sm"
          className="h-6 cursor-pointer gap-1 px-2 text-xs text-muted-foreground hover:text-foreground"
          onClick={handleNew}
        >
          <Plus className="size-3" />
          新对话
        </Button>
      </div>

      <ScrollArea className="flex-1">
        <div className="space-y-1 px-2 pb-4">
          {isFetching && (
            <div className="flex items-center justify-center py-6">
              <Loader2 className="size-4 animate-spin text-muted-foreground" />
            </div>
          )}
          {error && (
            <p className="px-2 py-4 text-xs text-muted-foreground">
              {friendlyMessage(error as ApiError)}
            </p>
          )}
          {!isFetching && sessions.length === 0 && (
            <p className="px-2 py-4 text-xs text-muted-foreground">暂无历史会话</p>
          )}
          {sessions.map((s) => {
            const isActive = s.conversationId === activeConversationId;
            return (
              <button
                key={s.conversationId}
                onClick={() => handleSelect(s)}
                className={`group flex w-full items-start gap-2 rounded-lg px-2.5 py-2 text-left transition-colors cursor-pointer ${
                  isActive
                    ? "bg-primary/10 text-foreground"
                    : "hover:bg-accent/50 text-muted-foreground"
                }`}
              >
                {channel === "AGENT" ? (
                  <Bot className="mt-0.5 size-3.5 shrink-0" />
                ) : (
                  <MessageSquare className="mt-0.5 size-3.5 shrink-0" />
                )}
                <div className="min-w-0 flex-1">
                  <p className="line-clamp-1 text-xs font-medium">
                    {s.title || truncate(s.firstQuery ?? "", 20) || "新对话"}
                  </p>
                  <p className="mt-0.5 line-clamp-1 text-[10px] text-muted-foreground/70">
                    {formatDate(s.lastAt)} · {s.messageCount} 条
                  </p>
                </div>
                <Trash2
                  className="size-3 shrink-0 opacity-0 transition-opacity group-hover:opacity-60 hover:!opacity-100 hover:text-destructive"
                  onClick={(e) => handleDelete(e, s)}
                />
              </button>
            );
          })}
        </div>
      </ScrollArea>
    </div>
  );
}
