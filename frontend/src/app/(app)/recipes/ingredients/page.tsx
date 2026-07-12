"use client";

import * as React from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { Carrot, Loader2 } from "lucide-react";

import { ingredientSearch } from "@/lib/api/recipes-client";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { PageHeader, EmptyState, ErrorState } from "@/components/common/states";
import { friendlyMessage, ApiError } from "@/lib/api/errors";

export default function IngredientsPage() {
  const [names, setNames] = React.useState("");
  const [mode, setMode] = React.useState<"any" | "all">("any");
  const [submitted, setSubmitted] = React.useState<{ names: string; mode: "any" | "all" } | null>(
    null
  );

  const { data, isFetching, error } = useQuery({
    queryKey: ["ingredient-search", submitted],
    queryFn: () => ingredientSearch(submitted!),
    enabled: !!submitted,
    retry: false,
  });

  function submit() {
    if (!names.trim()) return;
    setSubmitted({ names: names.trim(), mode });
  }

  return (
    <main className="mx-auto w-full max-w-5xl px-4 py-6">
      <PageHeader
        title="食材反查"
        description="输入手头的食材，找出能做的菜"
        icon={Carrot}
      />

      <div className="mt-6 space-y-3">
        <Input
          value={names}
          onChange={(e) => setNames(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && submit()}
          placeholder="多个食材用逗号分隔，如「鸡肉,土豆,胡萝卜」"
          aria-label="食材名称"
        />
        <div className="flex flex-wrap items-center gap-3">
          <div className="inline-flex rounded-lg border border-border p-0.5">
            <button
              onClick={() => setMode("any")}
              className={`cursor-pointer rounded-md px-3 py-1 text-sm transition-colors ${
                mode === "any" ? "bg-primary text-primary-foreground" : "text-muted-foreground"
              }`}
            >
              任一匹配
            </button>
            <button
              onClick={() => setMode("all")}
              className={`cursor-pointer rounded-md px-3 py-1 text-sm transition-colors ${
                mode === "all" ? "bg-primary text-primary-foreground" : "text-muted-foreground"
              }`}
            >
              全部匹配
            </button>
          </div>
          <Button onClick={submit} className="cursor-pointer" disabled={isFetching}>
            {isFetching ? <Loader2 className="size-4 animate-spin" /> : "查询"}
          </Button>
          <span className="text-xs text-muted-foreground">
            {mode === "any" ? "包含任意一个食材的菜谱" : "同时包含全部食材的菜谱"}
          </span>
        </div>
      </div>

      <div className="mt-6">
        {isFetching && (
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="aspect-[4/3] animate-pulse rounded-xl bg-muted" />
            ))}
          </div>
        )}
        {error && <ErrorState message={friendlyMessage(error as ApiError)} />}
        {data && !isFetching && (
          data.count > 0 ? (
            <div className="space-y-4">
              <p className="text-sm text-muted-foreground">
                找到 {data.count} 道菜（{data.matchMode === "all" ? "全部" : "任一"}匹配）
              </p>
              {data.results.map((r) => (
                <Link key={r.recipe.id} href={`/recipes/${r.recipe.id}`} className="block cursor-pointer">
                  <Card className="card-hover">
                    <CardContent className="flex items-center justify-between gap-3 py-3">
                      <div>
                        <p className="font-medium">{r.recipe.name}</p>
                        {r.recipe.summary && (
                          <p className="mt-0.5 line-clamp-1 text-xs text-muted-foreground">
                            {r.recipe.summary}
                          </p>
                        )}
                      </div>
                      <div className="flex flex-wrap justify-end gap-1.5">
                        {r.matchedIngredients.map((g) => (
                          <Badge key={g} variant="secondary" className="cursor-default">
                            {g}
                          </Badge>
                        ))}
                      </div>
                    </CardContent>
                  </Card>
                </Link>
              ))}
            </div>
          ) : (
            <EmptyState icon={Carrot} title="未找到匹配菜谱" />
          )
        )}
        {!submitted && !isFetching && (
          <EmptyState
            icon={Carrot}
            title="输入食材名称开始查询"
            description="系统会找出包含这些食材的菜谱"
          />
        )}
      </div>
    </main>
  );
}
