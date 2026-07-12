import Link from "next/link";
import {
  Sparkles,
  MessageSquare,
  ArrowRight,
  Search,
  GitCompareArrows,
  Carrot,
  Bot,
  ChefHat,
  type LucideIcon,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { HomeNav } from "@/components/home-nav";

const FEATURES: { icon: LucideIcon; title: string; desc: string }[] = [
  { icon: MessageSquare, title: "RAG 对话", desc: "检索增强生成，菜谱知识库秒级问答" },
  { icon: Bot, title: "Agent 自主", desc: "ReAct + 10 工具，逐步思考与调用" },
  { icon: Search, title: "语义搜索", desc: "向量检索，自然语言找菜谱" },
  { icon: GitCompareArrows, title: "菜谱对比", desc: "2-4 道菜配料异同一目了然" },
  { icon: Carrot, title: "食材反查", desc: "输入冰箱里的食材，反向找菜谱" },
  { icon: ChefHat, title: "营养与推荐", desc: "营养成分查询 + 应季三菜搭配" },
];

export default function Home() {
  return (
    <main className="relative flex min-h-svh flex-col overflow-hidden">
      <div className="pointer-events-none absolute inset-0 -z-20 bg-grid opacity-60" />
      <div className="pointer-events-none absolute inset-x-0 top-0 -z-10 h-[60vh] bg-[radial-gradient(70%_60%_at_50%_0%,color-mix(in_srgb,var(--ai)_22%,transparent),transparent)]" />

      <nav className="mx-auto flex w-full max-w-6xl items-center justify-between px-6 py-5">
        <span className="flex items-center gap-2 font-semibold">
          <span className="flex size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <ChefHat className="size-4" />
          </span>
          CookManus
        </span>
        <div className="flex items-center gap-2">
          <HomeNav />
        </div>
      </nav>

      <section className="mx-auto flex w-full max-w-3xl flex-1 flex-col items-center justify-center px-6 py-16 text-center">
        <span className="inline-flex items-center gap-2 rounded-full border border-border bg-card/70 px-4 py-1.5 text-sm text-muted-foreground backdrop-blur animate-fade-in">
          <Sparkles className="size-4 text-ai" />
          RAG + Agent 双引擎 · 中餐 AI 厨师
        </span>
        <h1 className="mt-6 text-5xl font-semibold tracking-tight sm:text-6xl animate-fade-up">
          <span className="text-gradient">CookManus</span>
        </h1>
        <p className="mt-5 max-w-xl text-lg leading-relaxed text-muted-foreground animate-fade-up" style={{ animationDelay: "0.05s" }}>
          问菜谱、查食材、比营养、看每日推荐 —— 与 AI 厨师实时对话。
        </p>
        <div className="mt-9 flex flex-col gap-3 sm:flex-row animate-fade-up" style={{ animationDelay: "0.1s" }}>
          <Button asChild size="lg" className="cursor-pointer glow-ai">
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
      </section>

      <section className="mx-auto w-full max-w-5xl px-6 pb-16">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {FEATURES.map((f, i) => {
            const Icon = f.icon;
            return (
              <div
                key={f.title}
                className="card-hover group flex flex-col gap-2 rounded-2xl border border-border bg-card/60 p-5 backdrop-blur animate-fade-up"
                style={{ animationDelay: `${0.15 + i * 0.05}s` }}
              >
                <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10 text-primary transition-colors group-hover:bg-primary group-hover:text-primary-foreground">
                  <Icon className="size-5" />
                </div>
                <h3 className="mt-1 font-medium">{f.title}</h3>
                <p className="text-sm text-muted-foreground">{f.desc}</p>
              </div>
            );
          })}
        </div>
      </section>

      <footer className="mx-auto w-full max-w-6xl px-6 py-6 text-center text-xs text-muted-foreground">
        CookManus · Spring Boot 3.4 + Next.js 16 · RAG / Agent / PGVector
      </footer>
    </main>
  );
}
