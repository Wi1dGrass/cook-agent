"use client";

import * as React from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Bot, User, Loader2 } from "lucide-react";
import { Markdown } from "@/components/common/markdown";
import { cn } from "@/lib/utils";
import type { ChatMessage } from "@/lib/store/chat-store";

function TypingDots() {
  return (
    <span className="inline-flex items-center gap-1" aria-label="正在输入">
      <span className="typing-dot size-1.5 rounded-full bg-muted-foreground" />
      <span className="typing-dot size-1.5 rounded-full bg-muted-foreground" />
      <span className="typing-dot size-1.5 rounded-full bg-muted-foreground" />
    </span>
  );
}

export function MessageBubble({ message }: { message: ChatMessage }) {
  const isUser = message.role === "user";
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.2, ease: "easeOut" }}
      className={cn("flex gap-3 px-4 py-5", isUser && "flex-row-reverse")}
    >
      <div
        className={cn(
          "flex size-8 shrink-0 items-center justify-center rounded-full",
          isUser
            ? "bg-secondary text-secondary-foreground"
            : "bg-primary text-primary-foreground"
        )}
        aria-hidden
      >
        {isUser ? <User className="size-4" /> : <Bot className="size-4" />}
      </div>
      <div className={cn("min-w-0 max-w-[min(80%,46rem)]", isUser && "text-right")}>
        <div
          className={cn(
            "rounded-2xl px-4 py-3 text-sm",
            isUser
              ? "bg-primary text-primary-foreground inline-block text-left"
              : "bg-card border border-border"
          )}
        >
          {isUser ? (
            <p className="whitespace-pre-wrap break-words">{message.content}</p>
          ) : message.content ? (
            <div className={cn(message.streaming && "stream-cursor")}>
              <Markdown>{message.content}</Markdown>
            </div>
          ) : message.streaming ? (
            <TypingDots />
          ) : (
            <p className="text-muted-foreground">（无回复）</p>
          )}
        </div>
        {!isUser && message.steps && message.steps.length > 0 && (
          <details className="mt-2 group">
            <summary className="cursor-pointer text-xs text-muted-foreground hover:text-foreground transition-colors select-none">
              Agent 执行步骤（{message.steps.length}）
            </summary>
            <ol className="mt-2 space-y-1.5 text-xs text-muted-foreground font-mono">
              {message.steps.map((step, i) => (
                <li key={i} className="rounded-md bg-muted/50 px-2 py-1.5 break-words">
                  {step}
                </li>
              ))}
            </ol>
          </details>
        )}
      </div>
    </motion.div>
  );
}

export function MessageList({
  messages,
  loading,
}: {
  messages: ChatMessage[];
  loading?: boolean;
}) {
  const endRef = React.useRef<HTMLDivElement>(null);
  React.useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages]);

  return (
    <div className="mx-auto w-full max-w-3xl">
      <AnimatePresence initial={false}>
        {messages.map((m) => (
          <MessageBubble key={m.id} message={m} />
        ))}
      </AnimatePresence>
      {loading && (
        <div className="flex items-center justify-center gap-2 py-6 text-sm text-muted-foreground">
          <Loader2 className="size-4 animate-spin" />
          正在思考…
        </div>
      )}
      <div ref={endRef} className="h-4" />
    </div>
  );
}
