"use client";

import * as React from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Bot, User, Loader2, SkipForward, Copy, Check } from "lucide-react";
import { Markdown } from "@/components/common/markdown";
import { useTypewriter } from "@/hooks/use-typewriter";
import { useChatStore } from "@/lib/store/chat-store";
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

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = React.useState(false);

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      /* ignore */
    }
  }

  return (
    <button
      onClick={handleCopy}
      className={cn(
        "inline-flex items-center gap-1 text-xs text-muted-foreground/60 hover:text-foreground transition-colors cursor-pointer",
        "opacity-0 group-hover:opacity-100"
      )}
      aria-label="复制"
    >
      {copied ? <Check className="size-3" /> : <Copy className="size-3" />}
      {copied ? "已复制" : "复制"}
    </button>
  );
}

function AssistantContent({ message }: { message: ChatMessage }) {
  const finishStreaming = useChatStore((s) => s.finishStreaming);
  const isStreaming = !!message.streaming;

  const { displayed, done, skip } = useTypewriter(message.content, {
    enabled: isStreaming,
    speed: 16,
    onDone: () => {
      if (isStreaming) finishStreaming();
    },
  });

  const showText = isStreaming ? displayed : message.content;

  if (!showText) {
    if (message.pending) return <TypingDots />;
    return <p className="text-muted-foreground">（无回复）</p>;
  }

  return (
    <div className="relative">
      <div className={cn(isStreaming && !done && "stream-cursor")}>
        <Markdown>{showText}</Markdown>
      </div>
      {!isStreaming && (
        <div className="mt-1">
          <CopyButton text={message.content} />
        </div>
      )}
      {isStreaming && !done && (
        <button
          onClick={skip}
          className="mt-1 inline-flex items-center gap-1 text-xs text-muted-foreground/70 hover:text-foreground transition-colors cursor-pointer"
          aria-label="跳过打字效果"
        >
          <SkipForward className="size-3" />
          跳过
        </button>
      )}
    </div>
  );
}

export function MessageBubble({ message }: { message: ChatMessage }) {
  const isUser = message.role === "user";
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.2, ease: "easeOut" }}
      className={cn("group flex gap-3 px-4 py-4", isUser && "flex-row-reverse")}
    >
      <div
        className={cn(
          "flex size-8 shrink-0 items-center justify-center rounded-full shadow-sm",
          isUser
            ? "bg-secondary text-secondary-foreground ring-1 ring-border"
            : "bg-gradient-to-br from-primary to-primary/75 text-primary-foreground"
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
              ? "bg-primary text-primary-foreground inline-block text-left rounded-tr-md shadow-sm"
              : "bg-card border border-border rounded-tl-md"
          )}
        >
          {isUser ? (
            <div className="relative">
              <p className="whitespace-pre-wrap break-words">{message.content}</p>
              <div className="mt-1">
                <CopyButton text={message.content} />
              </div>
            </div>
          ) : (
            <AssistantContent message={message} />
          )}
        </div>
        {!isUser && message.steps && message.steps.length > 0 && (
          <details className="mt-2 group/details">
            <summary className="inline-flex cursor-pointer items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground transition-colors select-none">
              <span className="flex size-1.5 rounded-full bg-primary/60" />
              Agent 执行步骤（{message.steps.length}）
            </summary>
            <ol className="mt-2 space-y-1.5 border-l-2 border-primary/20 pl-3 text-xs text-muted-foreground font-mono">
              {message.steps.map((step, i) => (
                <li key={i} className="rounded-md bg-muted/40 px-2.5 py-1.5 break-words whitespace-pre-wrap">
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
