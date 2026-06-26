"use client";

import * as React from "react";
import { useQuery } from "@tanstack/react-query";
import { GitCompareArrows, Plus, X, Loader2 } from "lucide-react";
import { toast } from "sonner";

import { compare } from "@/lib/api/recipes-client";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { PageHeader, EmptyState, ErrorState } from "@/components/common/states";
import { friendlyMessage, ApiError } from "@/lib/api/errors";

export default function ComparePage() {
  const [names, setNames] = React.useState<string[]>(["", ""]);
  const [submitted, setSubmitted] = React.useState<string | null>(null);

  const { data, isFetching, error } = useQuery({
    queryKey: ["compare", submitted],
    queryFn: () => compare(submitted!),
    enabled: !!submitted,
    retry: false,
  });

  function addField() {
    if (names.length >= 4) {
      toast.message("最多对比 4 道菜");
      return;
    }
    setNames([...names, ""]);
  }
  function removeField(i: number) {
    if (names.length <= 2) return;
    setNames(names.filter((_, idx) => idx !== i));
  }
  function setField(i: number, v: string) {
    setNames(names.map((n, idx) => (idx === i ? v : n)));
  }

  function submit() {
    const valid = names.map((n) => n.trim()).filter(Boolean);
    if (valid.length < 2) {
      toast.error("至少输入 2 道菜名");
      return;
    }
    setSubmitted(valid.join(","));
  }

  return (
    <main className="mx-auto w-full max-w-4xl px-4 py-6">
      <PageHeader
        title="菜谱对比"
        description="对比 2-4 道菜的配料异同"
        icon={GitCompareArrows}
      />

      <div className="mt-6 space-y-3">
        {names.map((n, i) => (
          <div key={i} className="flex gap-2">
            <Input
              value={n}
              onChange={(e) => setField(i, e.target.value)}
              placeholder={`第 ${i + 1} 道菜名`}
              aria-label={`第 ${i + 1} 道菜名`}
            />
            {names.length > 2 && (
              <Button
                type="button"
                variant="ghost"
                size="icon"
                className="cursor-pointer"
                onClick={() => removeField(i)}
                aria-label="删除"
              >
                <X className="size-4" />
              </Button>
            )}
          </div>
        ))}
        <div className="flex gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="cursor-pointer"
            onClick={addField}
            disabled={names.length >= 4}
          >
            <Plus className="size-4" />
            添加
          </Button>
          <Button onClick={submit} className="cursor-pointer" disabled={isFetching}>
            {isFetching ? <Loader2 className="size-4 animate-spin" /> : "开始对比"}
          </Button>
        </div>
        <p className="text-xs text-muted-foreground">支持中文逗号，如「宫保鸡丁，鱼香肉丝」</p>
      </div>

      <div className="mt-6">
        {isFetching && (
          <div className="space-y-3">
            <div className="h-32 animate-pulse rounded-xl bg-muted" />
            <div className="h-32 animate-pulse rounded-xl bg-muted" />
          </div>
        )}
        {error && <ErrorState message={friendlyMessage(error as ApiError)} />}
        {data && !isFetching && (
          <div className="space-y-4">
            <div className="flex flex-wrap gap-2">
              {data.recipes.map((r) => (
                <Badge key={r.id} variant="secondary" className="cursor-default">
                  {r.name}
                </Badge>
              ))}
            </div>

            <Card>
              <CardHeader>
                <CardTitle className="text-base">共同配料</CardTitle>
              </CardHeader>
              <CardContent>
                {data.commonIngredients.length === 0 ? (
                  <p className="text-sm text-muted-foreground">无共同配料</p>
                ) : (
                  <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="border-b border-border">
                          <th className="px-3 py-2 text-left font-medium">配料</th>
                          {data.recipes.map((r) => (
                            <th key={r.id} className="px-3 py-2 text-left font-medium">
                              {r.name}
                            </th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {data.commonIngredients.map((ing) => (
                          <tr key={ing.name} className="border-b border-border last:border-0">
                            <td className="px-3 py-2 font-medium">{ing.name}</td>
                            {data.recipes.map((r) => (
                              <td key={r.id} className="px-3 py-2 text-muted-foreground">
                                {ing.quantities[r.name] ?? "—"}
                              </td>
                            ))}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-base">各自独有配料</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                {Object.entries(data.uniqueIngredients).map(([name, ings]) => (
                  <div key={name}>
                    <p className="text-sm font-medium">{name}</p>
                    {ings.length === 0 ? (
                      <p className="mt-0.5 text-xs text-muted-foreground">无独有配料</p>
                    ) : (
                      <div className="mt-1.5 flex flex-wrap gap-1.5">
                        {ings.map((g) => (
                          <Badge key={g} variant="outline" className="cursor-default">
                            {g}
                          </Badge>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </CardContent>
            </Card>
          </div>
        )}
        {!submitted && !isFetching && (
          <EmptyState
            icon={GitCompareArrows}
            title="输入菜名开始对比"
            description="对比结果会展示共同配料与各自独有的配料"
          />
        )}
      </div>
    </main>
  );
}
