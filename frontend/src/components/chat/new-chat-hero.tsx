"use client";

import { motion } from "framer-motion";
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
      <motion.div
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.4, ease: "easeOut" }}
        className="relative flex size-16 items-center justify-center"
      >
        <span className="absolute inset-0 rounded-2xl bg-primary/20 blur-xl" />
        <div className="relative flex size-16 items-center justify-center rounded-2xl bg-gradient-to-br from-primary to-primary/70 text-primary-foreground">
          <Sparkles className="size-8" />
        </div>
      </motion.div>

      <motion.h2
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1, duration: 0.3 }}
        className="mt-6 text-2xl font-semibold tracking-tight"
      >
        今天想吃点什么？
      </motion.h2>
      <motion.p
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.15, duration: 0.3 }}
        className="mt-2 text-sm text-muted-foreground"
      >
        我是 CookManus，基于 RAG 检索的中餐 AI 厨师。试试下面的问题，或直接输入你想做的菜。
      </motion.p>

      <div className="mt-8 grid w-full gap-3 sm:grid-cols-2">
        {suggestions.map((s, i) => {
          const Icon = s.icon;
          return (
            <motion.button
              key={s.text}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 + i * 0.06, duration: 0.3 }}
              onClick={() => onPick(s.text)}
              className="card-hover group flex items-start gap-3 rounded-xl border border-border bg-card/60 p-4 text-left backdrop-blur hover:border-primary/50 cursor-pointer"
            >
              <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary transition-colors group-hover:bg-primary group-hover:text-primary-foreground">
                <Icon className="size-4.5" />
              </span>
              <span className="pt-1.5 text-sm text-foreground/90 group-hover:text-foreground">
                {s.text}
              </span>
            </motion.button>
          );
        })}
      </div>
    </div>
  );
}
