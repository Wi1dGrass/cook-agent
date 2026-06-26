"use client";

import * as React from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Heart, Loader2, Trash2 } from "lucide-react";
import { toast } from "sonner";

import { listFavorites, removeFavorite } from "@/lib/api/favorites";
import { RecipeCardGrid } from "@/components/recipe/recipe-card";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { PageHeader, EmptyState, ErrorState } from "@/components/common/states";
import { friendlyMessage, ApiError } from "@/lib/api/errors";
import type { RecipeSummary } from "@/lib/api/types";

export default function FavoritesPage() {
  const qc = useQueryClient();
  const { data, isFetching, error } = useQuery({
    queryKey: ["favorites"],
    queryFn: listFavorites,
    retry: false,
  });

  async function remove(id: number) {
    try {
      await removeFavorite(id);
      qc.setQueryData<RecipeSummary[]>(["favorites"], (old) =>
        (old ?? []).filter((r) => r.id !== id)
      );
      toast.success("已取消收藏");
    } catch (e) {
      toast.error(e instanceof ApiError ? friendlyMessage(e) : "操作失败");
    }
  }

  return (
    <main className="mx-auto w-full max-w-6xl px-4 py-6">
      <PageHeader
        title="我的收藏"
        description="你收藏的菜谱"
        icon={Heart}
      />

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
          data.length > 0 ? (
            <div className="space-y-4">
              <p className="text-sm text-muted-foreground">共 {data.length} 道收藏</p>
              <RecipeCardGrid recipes={data} />
              <div className="space-y-2">
                {data.map((r) => (
                  <Card key={r.id}>
                    <CardContent className="flex items-center justify-between gap-3 py-3">
                      <div>
                        <p className="font-medium">{r.name}</p>
                        {r.summary && (
                          <p className="mt-0.5 line-clamp-1 text-xs text-muted-foreground">
                            {r.summary}
                          </p>
                        )}
                      </div>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="cursor-pointer text-muted-foreground hover:text-destructive"
                        onClick={() => remove(r.id)}
                        aria-label={`取消收藏 ${r.name}`}
                      >
                        <Trash2 className="size-4" />
                      </Button>
                    </CardContent>
                  </Card>
                ))}
              </div>
            </div>
          ) : (
            <EmptyState
              icon={Heart}
              title="还没有收藏"
              description="浏览菜谱时点击收藏按钮，菜品会出现在这里"
            />
          )
        )}
      </div>
    </main>
  );
}
