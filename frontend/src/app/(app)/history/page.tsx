"use client";

import * as React from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { History, Loader2, Trash2, Eye, MessageSquare, RotateCcw } from "lucide-react";
import { toast } from "sonner";

import { listHistory, getConversation, deleteConversation } from "@/lib/api/chat";
import { useChatStore, uid, type ChatMessage } from "@/lib/store/chat-store";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Markdown } from "@/components/common/markdown";
import { PageHeader, EmptyState, ErrorState } from "@/components/common/states";
import { formatDate, truncate } from "@/lib/utils/format";
import { friendlyMessage, ApiError } from "@/lib/api/errors";
import type { ChatHistory } from "@/lib/api/types";

export default function HistoryPage() {
  const router = useRouter();
  const qc = useQueryClient();
  const { data, isFetching, error } = useQuery({
    queryKey: ["history"],
    queryFn: () => listHistory(50),
    retry: false,
  });

  async function remove(convId: string) {
    try {
      await deleteConversation(convId);
      qc.setQueryData<ChatHistory[]>(["history"], (old) =>
        (old ?? []).filter((h) => h.conversationId !== convId)
      );
      qc.invalidateQueries({ queryKey: ["sessions"] });
      toast.success("对话已删除");
    } catch (e) {
      toast.error(e instanceof ApiError ? friendlyMessage(e) : "删除失败");
    }
  }

  const grouped = React.useMemo(() => {
    if (!data) return [] as { conversationId: string; items: ChatHistory[] }[];
    const map = new Map<string, ChatHistory[]>();
    for (const h of data) {
      const arr = map.get(h.conversationId) ?? [];
      arr.push(h);
      map.set(h.conversationId, arr);
    }
    return Array.from(map.entries()).map(([conversationId, items]) => ({
      conversationId,
      items: items.sort((a, b) => a.id - b.id),
    }));
  }, [data]);

  return (
    <main className="mx-auto w-full max-w-4xl px-4 py-6">
      <PageHeader
        title="对话历史"
        description="你的 AI 对话记录"
        icon={History}
      />

      <div className="mt-6">
        {isFetching && (
          <div className="space-y-3">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="h-20 animate-pulse rounded-xl bg-muted" />
            ))}
          </div>
        )}
        {error && <ErrorState message={friendlyMessage(error as ApiError)} />}
        {data && !isFetching && (
          grouped.length > 0 ? (
            <div className="space-y-3">
              <p className="text-sm text-muted-foreground">共 {grouped.length} 个对话</p>
              {grouped.map((g) => (
                <Card key={g.conversationId}>
                  <CardContent className="flex items-start justify-between gap-3 py-4">
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <MessageSquare className="size-4 text-primary" />
                        <Badge
                          variant={g.items[0].channel === "AGENT" ? "default" : "secondary"}
                          className="cursor-default"
                        >
                          {g.items[0].channel === "AGENT" ? "Agent" : "对话"}
                        </Badge>
                        <span className="text-xs text-muted-foreground">
                          {formatDate(g.items[0].createdAt)}
                        </span>
                      </div>
                      <p className="mt-1.5 line-clamp-1 text-sm font-medium">
                        {g.items[0].title || g.items[0].query}
                      </p>
                      <p className="mt-0.5 line-clamp-1 text-xs text-muted-foreground">
                        {truncate(g.items[0].reply, 80)} · 共 {g.items.length} 条
                      </p>
                    </div>
                    <div className="flex shrink-0 gap-1">
                      <ConversationDialog
                        conversationId={g.conversationId}
                        title={g.items[0].title || g.items[0].query}
                        channel={g.items[0].channel}
                      >
                        {g.items}
                      </ConversationDialog>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="cursor-pointer text-muted-foreground hover:text-destructive"
                        onClick={() => remove(g.conversationId)}
                        aria-label="删除对话"
                      >
                        <Trash2 className="size-4" />
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          ) : (
            <EmptyState
              icon={History}
              title="还没有对话记录"
              description="登录后使用 AI 对话，记录会保存在这里"
            />
          )
        )}
      </div>
    </main>
  );
}

function ConversationDialog({
  conversationId,
  title,
  channel,
  children,
}: {
  conversationId: string;
  title: string;
  channel: string;
  children: ChatHistory[];
}) {
  const router = useRouter();
  const [open, setOpen] = React.useState(false);
  const [items, setItems] = React.useState<ChatHistory[]>(children);
  const [loading, setLoading] = React.useState(false);
  const [resuming, setResuming] = React.useState(false);

  async function loadFull() {
    if (children.length >= 2) {
      setItems(children);
      return;
    }
    setLoading(true);
    try {
      const full = await getConversation(conversationId);
      setItems(full);
    } catch {
      setItems(children);
    } finally {
      setLoading(false);
    }
  }

  async function handleContinue() {
    setResuming(true);
    try {
      const full = items.length >= 2 ? items : await getConversation(conversationId);
      const messages: ChatMessage[] = [];
      for (const h of full) {
        messages.push({
          id: uid(),
          role: "user",
          content: h.query,
          createdAt: new Date(h.createdAt).getTime(),
        });
        if (h.reply) {
          messages.push({
            id: uid(),
            role: "assistant",
            content: h.reply,
            createdAt: new Date(h.createdAt).getTime() + 1,
          });
        }
      }
      useChatStore.getState().loadConversation(conversationId, messages);
      setOpen(false);
      router.push(channel === "AGENT" ? "/agent" : "/chat");
    } catch (e) {
      toast.error(e instanceof ApiError ? friendlyMessage(e) : "加载失败");
    } finally {
      setResuming(false);
    }
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(v) => {
        setOpen(v);
        if (v) loadFull();
      }}
    >
      <DialogTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          className="cursor-pointer text-muted-foreground"
          aria-label="查看完整对话"
        >
          <Eye className="size-4" />
        </Button>
      </DialogTrigger>
      <DialogContent className="max-h-[80vh] max-w-2xl overflow-y-auto scrollbar-thin">
        <DialogHeader>
          <DialogTitle className="line-clamp-1">{title}</DialogTitle>
        </DialogHeader>
        {loading ? (
          <div className="flex items-center justify-center py-8">
            <Loader2 className="size-5 animate-spin text-muted-foreground" />
          </div>
        ) : (
          <>
            <div className="space-y-4 py-2">
              {items.map((h) => (
                <div key={h.id} className="space-y-1.5">
                  <div className="rounded-lg bg-secondary px-3 py-2">
                    <p className="text-xs font-medium text-muted-foreground">你</p>
                    <p className="mt-0.5 text-sm">{h.query}</p>
                  </div>
                  <div className="rounded-lg border border-border px-3 py-2">
                    <p className="text-xs font-medium text-muted-foreground">CookManus</p>
                    <div className="mt-0.5">
                      <Markdown>{h.reply}</Markdown>
                    </div>
                  </div>
                  <p className="text-xs text-muted-foreground">{formatDate(h.createdAt)}</p>
                </div>
              ))}
            </div>
            <div className="sticky bottom-0 -mx-6 -mb-4 flex justify-end gap-2 border-t border-border/60 bg-background/80 px-6 py-3 backdrop-blur">
              <Button
                onClick={handleContinue}
                disabled={resuming}
                className="cursor-pointer gap-1.5"
                size="sm"
              >
                {resuming ? (
                  <Loader2 className="size-3.5 animate-spin" />
                ) : (
                  <RotateCcw className="size-3.5" />
                )}
                继续这段对话
              </Button>
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
