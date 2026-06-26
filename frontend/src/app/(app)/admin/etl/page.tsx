"use client";

import * as React from "react";
import { Database, RefreshCw, Loader2, CheckCircle2 } from "lucide-react";
import { toast } from "sonner";

import { etlSync, etlRebuildIndex } from "@/lib/api/admin";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { PageHeader } from "@/components/common/states";
import { friendlyMessage, ApiError } from "@/lib/api/errors";

export default function EtlPage() {
  const [syncing, setSyncing] = React.useState(false);
  const [rebuilding, setRebuilding] = React.useState(false);
  const [lastResult, setLastResult] = React.useState<string | null>(null);

  async function handleSync() {
    setSyncing(true);
    setLastResult(null);
    try {
      const res = await etlSync();
      setLastResult(res.message ?? "同步完成");
      toast.success(res.message ?? "ETL 同步完成");
    } catch (e) {
      const msg = e instanceof ApiError ? friendlyMessage(e) : "同步失败";
      toast.error(msg);
      setLastResult(`失败：${msg}`);
    } finally {
      setSyncing(false);
    }
  }

  async function handleRebuild() {
    setRebuilding(true);
    setLastResult(null);
    try {
      const res = await etlRebuildIndex();
      setLastResult(res.message ?? "索引重建完成");
      toast.success(res.message ?? "向量索引重建完成");
    } catch (e) {
      const msg = e instanceof ApiError ? friendlyMessage(e) : "重建失败";
      toast.error(msg);
      setLastResult(`失败：${msg}`);
    } finally {
      setRebuilding(false);
    }
  }

  return (
    <main className="mx-auto w-full max-w-3xl px-4 py-6">
      <PageHeader
        title="ETL 同步"
        description="知识库与向量索引管理"
        icon={Database}
      />

      <div className="mt-6 space-y-4">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <RefreshCw className="size-4 text-primary" />
              同步知识库
            </CardTitle>
            <CardDescription>
              从 CookLikeHOC/ 加载菜谱 Markdown 并写入向量库（限速：每分钟 2 次）
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button onClick={handleSync} className="cursor-pointer" disabled={syncing}>
              {syncing ? <Loader2 className="size-4 animate-spin" /> : <RefreshCw className="size-4" />}
              开始同步
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <Database className="size-4 text-primary" />
              重建向量索引
            </CardTitle>
            <CardDescription>
              完全重建 PGVector 索引（限速：每分钟 1 次，耗时较长）
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button onClick={handleRebuild} className="cursor-pointer" disabled={rebuilding}>
              {rebuilding ? <Loader2 className="size-4 animate-spin" /> : <Database className="size-4" />}
              重建索引
            </Button>
          </CardContent>
        </Card>

        {lastResult && (
          <Card className="bg-accent/30">
            <CardContent className="flex items-start gap-2 py-4 text-sm">
              <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-primary" />
              <p className="font-mono">{lastResult}</p>
            </CardContent>
          </Card>
        )}
      </div>
    </main>
  );
}
