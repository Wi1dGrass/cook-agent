"use client";

import * as React from "react";
import { ArrowUp, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export function ChatInput({
  onSend,
  disabled,
  loading,
  placeholder = "输入你的问题…",
  defaultValue = "",
}: {
  onSend: (text: string) => void;
  disabled?: boolean;
  loading?: boolean;
  placeholder?: string;
  defaultValue?: string;
}) {
  const [value, setValue] = React.useState(defaultValue);
  const ref = React.useRef<HTMLTextAreaElement>(null);

  React.useEffect(() => {
    const el = ref.current;
    if (!el) return;
    el.style.height = "auto";
    el.style.height = Math.min(el.scrollHeight, 200) + "px";
  }, [value]);

  function submit() {
    const text = value.trim();
    if (!text || disabled || loading) return;
    onSend(text);
    setValue("");
  }

  function onKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === "Enter" && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault();
      submit();
    }
  }

  return (
    <div className="border-t border-border bg-background/80 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="mx-auto w-full max-w-3xl px-4 py-3">
        <div
          className={cn(
            "relative flex items-end gap-2 rounded-2xl border border-border bg-card p-2 transition-colors focus-within:border-primary/50 focus-within:ring-2 focus-within:ring-primary/15"
          )}
        >
          <textarea
            ref={ref}
            value={value}
            onChange={(e) => setValue(e.target.value)}
            onKeyDown={onKeyDown}
            rows={1}
            placeholder={placeholder}
            disabled={disabled}
            aria-label="消息输入框"
            className="max-h-[200px] min-h-[40px] flex-1 resize-none bg-transparent px-2 py-1.5 text-sm outline-none placeholder:text-muted-foreground disabled:opacity-60 scrollbar-thin"
          />
          <Button
            size="icon"
            className="size-9 shrink-0 cursor-pointer rounded-xl"
            onClick={submit}
            disabled={!value.trim() || disabled || loading}
            aria-label="发送"
          >
            {loading ? <Loader2 className="size-4 animate-spin" /> : <ArrowUp className="size-4" />}
          </Button>
        </div>
        <p className="mt-1.5 text-center text-xs text-muted-foreground">
          Enter 发送 · Shift+Enter 换行
        </p>
      </div>
    </div>
  );
}
