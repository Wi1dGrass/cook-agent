"use client";

import * as React from "react";
import { useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { Search, Sparkles, Loader2 } from "lucide-react";

import { semanticSearch, recommend } from "@/lib/api/recipes-client";
import { RecipeCardGrid } from "@/components/recipe/recipe-card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { PageHeader, EmptyState, ErrorState } from "@/components/common/states";
import { friendlyMessage, ApiError } from "@/lib/api/errors";

export default function SearchPage() {
  return (
    <React.Suspense fallback={null}>
      <SearchPageInner />
    </React.Suspense>
  );
}

function SearchPageInner() {
  const sp = useSearchParams();
  const initialTab = (sp.get("tab") as "search" | "recommend") ?? "search";
  const [tab, setTab] = React.useState<"search" | "recommend">(initialTab);

  const [query, setQuery] = React.useState("");
  const [submitted, setSubmitted] = React.useState<string | null>(null);

  const [criteria, setCriteria] = React.useState("");
  const [recSubmitted, setRecSubmitted] = React.useState<string | null>(null);

  const searchQ = useQuery({
    queryKey: ["semantic-search", submitted],
    queryFn: () => semanticSearch({ keyword: submitted!, topK: 10 }),
    enabled: !!submitted,
    retry: false,
  });

  const recQ = useQuery({
    queryKey: ["recommend", recSubmitted],
    queryFn: () => recommend({ criteria: recSubmitted!, count: 6 }),
    enabled: !!recSubmitted,
    retry: false,
  });

  return (
    <main className="mx-auto w-full max-w-5xl px-4 py-6">
      <PageHeader
        title="语义搜索与推荐"
        description="基于向量检索的智能菜谱发现"
        icon={Search}
      />

      <Tabs
        value={tab}
        onValueChange={(v) => setTab(v as "search" | "recommend")}
        className="mt-6"
      >
        <TabsList>
          <TabsTrigger value="search" className="cursor-pointer">
            <Search className="size-4" />
            语义搜索
          </TabsTrigger>
          <TabsTrigger value="recommend" className="cursor-pointer">
            <Sparkles className="size-4" />
            智能推荐
          </TabsTrigger>
        </TabsList>

        <TabsContent value="search" className="mt-4 space-y-4">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              if (query.trim()) setSubmitted(query.trim());
            }}
            className="flex gap-2"
          >
            <Input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="用自然语言描述想要的菜，如「下饭的辣菜」"
              aria-label="语义搜索"
            />
            <Button type="submit" className="cursor-pointer" disabled={searchQ.isFetching}>
              {searchQ.isFetching ? <Loader2 className="size-4 animate-spin" /> : "搜索"}
            </Button>
          </form>

          {searchQ.isFetching && (
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
              {Array.from({ length: 5 }).map((_, i) => (
                <div key={i} className="aspect-[4/3] animate-pulse rounded-xl bg-muted" />
              ))}
            </div>
          )}
          {searchQ.isError && (
            <ErrorState message={friendlyMessage(searchQ.error as ApiError)} />
          )}
          {searchQ.data && !searchQ.isFetching && (
            searchQ.data.count > 0 ? (
              <>
                <p className="text-sm text-muted-foreground">
                  找到 {searchQ.data.count} 个相关菜谱
                </p>
                <RecipeCardGrid recipes={searchQ.data.recipes} />
              </>
            ) : (
              <EmptyState icon={Search} title="未找到相关菜谱" description="试试换一种描述" />
            )
          )}
          {!submitted && !searchQ.isFetching && (
            <EmptyState
              icon={Search}
              title="输入关键词开始搜索"
              description="语义搜索会理解你的意图，无需精确匹配菜名"
            />
          )}
        </TabsContent>

        <TabsContent value="recommend" className="mt-4 space-y-4">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              if (criteria.trim()) setRecSubmitted(criteria.trim());
            }}
            className="flex gap-2"
          >
            <Input
              value={criteria}
              onChange={(e) => setCriteria(e.target.value)}
              placeholder="描述你的需求，如「清淡的素食、低卡」"
              aria-label="推荐条件"
            />
            <Button type="submit" className="cursor-pointer" disabled={recQ.isFetching}>
              {recQ.isFetching ? <Loader2 className="size-4 animate-spin" /> : "推荐"}
            </Button>
          </form>

          {recQ.isError && <ErrorState message={friendlyMessage(recQ.error as ApiError)} />}
          {recQ.data && !recQ.isFetching && (
            recQ.data.count > 0 ? (
              <>
                <p className="text-sm text-muted-foreground">为你推荐 {recQ.data.count} 道菜</p>
                <RecipeCardGrid recipes={recQ.data.recipes} />
              </>
            ) : (
              <EmptyState icon={Sparkles} title="暂无推荐" description="尝试调整你的需求描述" />
            )
          )}
          {!recSubmitted && !recQ.isFetching && (
            <EmptyState
              icon={Sparkles}
              title="描述你的口味，获取推荐"
              description="例如：想做快手菜、有鸡蛋和西红柿"
            />
          )}
        </TabsContent>
      </Tabs>
    </main>
  );
}
