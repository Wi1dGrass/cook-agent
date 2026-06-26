"use client";

import * as React from "react";
import { useQuery } from "@tanstack/react-query";
import { Apple, Loader2 } from "lucide-react";

import { nutrition } from "@/lib/api/recipes-client";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { PageHeader, EmptyState, ErrorState } from "@/components/common/states";
import { parseNutrition } from "@/lib/utils/format";
import { friendlyMessage, ApiError } from "@/lib/api/errors";

export default function NutritionPage() {
  const [name, setName] = React.useState("");
  const [submitted, setSubmitted] = React.useState<string | null>(null);

  const { data, isFetching, error } = useQuery({
    queryKey: ["nutrition", submitted],
    queryFn: () => nutrition(submitted!),
    enabled: !!submitted,
    retry: false,
  });

  const nutritionData = data ? parseNutrition(data.nutritionJson) : null;

  function submit() {
    if (name.trim()) setSubmitted(name.trim());
  }

  return (
    <main className="mx-auto w-full max-w-3xl px-4 py-6">
      <PageHeader
        title="营养查询"
        description="查询菜品的营养成分（每 100g）"
        icon={Apple}
      />

      <div className="mt-6 flex gap-2">
        <Input
          value={name}
          onChange={(e) => setName(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && submit()}
          placeholder="输入菜名，如「番茄炒蛋」"
          aria-label="菜名"
        />
        <Button onClick={submit} className="cursor-pointer" disabled={isFetching}>
          {isFetching ? <Loader2 className="size-4 animate-spin" /> : "查询"}
        </Button>
      </div>

      <div className="mt-6">
        {isFetching && <div className="h-48 animate-pulse rounded-xl bg-muted" />}
        {error && <ErrorState message={friendlyMessage(error as ApiError)} />}
        {data && !isFetching && (
          <div className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">{data.name}</CardTitle>
              </CardHeader>
              <CardContent>
                {data.summary && (
                  <p className="mb-3 text-sm text-muted-foreground">{data.summary}</p>
                )}
                {nutritionData ? (
                  <div className="overflow-hidden rounded-lg border border-border">
                    <table className="w-full text-sm">
                      <tbody>
                        {Object.entries(nutritionData).map(([k, v]) => (
                          <tr key={k} className="border-b border-border last:border-0">
                            <td className="bg-muted/50 px-4 py-2 font-medium">{k}</td>
                            <td className="px-4 py-2 text-muted-foreground">{v}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground">暂无营养数据</p>
                )}
              </CardContent>
            </Card>
          </div>
        )}
        {!submitted && !isFetching && (
          <EmptyState icon={Apple} title="输入菜名查询营养" />
        )}
      </div>
    </main>
  );
}
