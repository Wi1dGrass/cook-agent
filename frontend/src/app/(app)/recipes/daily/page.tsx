"use client";

import * as React from "react";
import { useQuery } from "@tanstack/react-query";
import { CalendarDays, Loader2, Snowflake, Droplet, Flower2, Leaf } from "lucide-react";

import { dailyRecommend } from "@/lib/api/recipes-client";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import Link from "next/link";
import { PageHeader, ErrorState } from "@/components/common/states";
import { friendlyMessage, ApiError } from "@/lib/api/errors";

const SEASON_ICON: Record<string, typeof Snowflake> = {
  春季: Flower2,
  夏季: Leaf,
  秋季: Droplet,
  冬季: Snowflake,
};

export default function DailyPage() {
  const [preference, setPreference] = React.useState("");
  const [comboCount, setComboCount] = React.useState(1);
  const [submitted, setSubmitted] = React.useState<{ preference?: string; comboCount: number }>({
    comboCount: 1,
  });

  const { data, isFetching, error, refetch } = useQuery({
    queryKey: ["daily", submitted],
    queryFn: () => dailyRecommend(submitted),
    retry: false,
  });

  function submit() {
    setSubmitted({ preference: preference.trim() || undefined, comboCount });
  }

  const SeasonIcon = data ? SEASON_ICON[data.season] ?? CalendarDays : CalendarDays;

  return (
    <main className="mx-auto w-full max-w-4xl px-4 py-6">
      <PageHeader
        title="每日推荐"
        description="应季三菜搭配：一荤一素一汤"
        icon={CalendarDays}
      />

      <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:items-center">
        <Input
          value={preference}
          onChange={(e) => setPreference(e.target.value)}
          placeholder="口味偏好（可选），如「清淡、低脂」"
          aria-label="口味偏好"
          className="flex-1"
        />
        <div className="flex items-center gap-2">
          <label htmlFor="comboCount" className="text-sm text-muted-foreground">
            组数
          </label>
          <Input
            id="comboCount"
            type="number"
            min={1}
            max={5}
            value={comboCount}
            onChange={(e) => setComboCount(Math.max(1, Math.min(5, Number(e.target.value) || 1)))}
            className="w-20"
          />
          <Button onClick={submit} className="cursor-pointer" disabled={isFetching}>
            {isFetching ? <Loader2 className="size-4 animate-spin" /> : "生成"}
          </Button>
        </div>
      </div>

      <div className="mt-6">
        {isFetching && (
          <div className="space-y-4">
            {Array.from({ length: comboCount }).map((_, i) => (
              <div key={i} className="h-32 animate-pulse rounded-xl bg-muted" />
            ))}
          </div>
        )}
        {error && <ErrorState message={friendlyMessage(error as ApiError)} />}
        {data && !isFetching && (
          <div className="space-y-4">
            <Card className="bg-accent/40">
              <CardContent className="flex items-start gap-3 py-4">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/15 text-primary">
                  <SeasonIcon className="size-5" />
                </div>
                <div>
                  <p className="flex items-center gap-2 font-medium">
                    {data.season}应季
                    <Badge variant="secondary" className="cursor-default">
                      {data.combos.length} 组搭配
                    </Badge>
                  </p>
                  {data.dietAdvice && (
                    <p className="mt-1 text-sm text-muted-foreground">{data.dietAdvice}</p>
                  )}
                </div>
              </CardContent>
            </Card>

            {data.combos.map((combo, i) => (
              <Card key={i}>
                <CardHeader>
                  <CardTitle className="text-base">搭配 {i + 1}</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="grid gap-3 sm:grid-cols-3">
                    {(["meat", "veggie", "soup"] as const).map((slot) => {
                      const r = combo[slot];
                      const label = slot === "meat" ? "荤菜" : slot === "veggie" ? "素菜" : "汤";
                      if (!r) {
                        return (
                          <div
                            key={slot}
                            className="rounded-lg border border-dashed border-border p-4 text-center"
                          >
                            <p className="text-xs text-muted-foreground">{label}</p>
                            <p className="mt-1 text-sm text-muted-foreground/70">暂无推荐</p>
                          </div>
                        );
                      }
                      return (
                        <Link
                          key={slot}
                          href={`/recipes/${r.id}`}
                          className="block cursor-pointer rounded-lg border border-border p-4 transition-colors hover:border-primary/40"
                        >
                          <p className="text-xs text-muted-foreground">{label}</p>
                          <p className="mt-1 font-medium">{r.name}</p>
                          {r.summary && (
                            <p className="mt-1 line-clamp-2 text-xs text-muted-foreground">
                              {r.summary}
                            </p>
                          )}
                        </Link>
                      );
                    })}
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>
    </main>
  );
}
