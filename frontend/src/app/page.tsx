import Link from "next/link";
import { Sparkles, MessageSquare, ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function Home() {
  return (
    <main className="relative flex min-h-svh flex-col items-center justify-center px-6">
      <div className="pointer-events-none absolute inset-0 -z-10 bg-[radial-gradient(60%_50%_at_50%_0%,color-mix(in_srgb,var(--ai)_18%,transparent),transparent)]" />
      <div className="flex flex-col items-center text-center max-w-2xl">
        <span className="inline-flex items-center gap-2 rounded-full border border-border bg-card/60 px-4 py-1.5 text-sm text-muted-foreground backdrop-blur">
          <Sparkles className="size-4 text-ai" />
          RAG + Agent 双引擎 · 中餐 AI 厨师
        </span>
        <h1 className="mt-6 text-4xl font-semibold tracking-tight sm:text-6xl">
          CookManus
        </h1>
        <p className="mt-5 text-lg text-muted-foreground">
          问菜谱、查食材、比营养、看每日推荐 —— 与 AI 厨师实时对话。
        </p>
        <div className="mt-8 flex flex-col gap-3 sm:flex-row">
          <Button asChild size="lg" className="cursor-pointer">
            <Link href="/chat">
              <MessageSquare className="size-4" />
              开始对话
            </Link>
          </Button>
          <Button asChild size="lg" variant="outline" className="cursor-pointer">
            <Link href="/recipes">
              浏览菜谱
              <ArrowRight className="size-4" />
            </Link>
          </Button>
        </div>
      </div>
    </main>
  );
}
