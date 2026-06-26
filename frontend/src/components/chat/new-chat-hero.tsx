"use client";

import type { LucideIcon } from "lucide-react";
import { Sparkles } from "lucide-react";

interface Suggestion {
  icon: LucideIcon;
  text: string;
}

export function NewChatHero({
  onPick,
  suggestions,
}: {
  onPick: (text: string) => void;
  suggestions: Suggestion[];
}) {
  return (
    <div className="mx-auto flex h-full max-w-3xl flex-col items-center justify-center px-4 py-10 text-center">
      <div className="flex size-14 items-center justify-center rounded-2xl bg-primary/10 text-primary">
        <Sparkles className="size-7" />
      </div>
      <h2 className="mt-5 text-2xl font-semibold tracking-tight">
        今天想吃点什么？
      </h2>
      <p className="mt-2 text-sm text-muted-foreground">
        我是 CookManus，基于 RAG 检索的中餐 AI 厨师。试试下面的问题，或直接输入你想做的菜。
      </p>

      <div className="mt-8 grid w-full gap-3 sm:grid-cols-2">
        {suggestions.map((s) => {
          const Icon = s.icon;
          return (
            <button
              key={s.text}
              onClick={() => onPick(s.text)}
              className="group flex items-start gap-3 rounded-xl border border-border bg-card p-4 text-left transition-colors hover:border-primary/40 hover:bg-accent/40 cursor-pointer"
            >
              <Icon className="mt-0.5 size-5 shrink-0 text-primary" />
              <span className="text-sm text-foreground/90 group-hover:text-foreground">
                {s.text}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
