"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";

export function RecipePagination({
  page,
  pages,
}: {
  page: number;
  pages: number;
}) {
  const router = useRouter();
  const params = useSearchParams();
  if (pages <= 1) return null;

  function go(p: number) {
    const sp = new URLSearchParams(params.toString());
    sp.set("pageNum", String(p));
    router.push(`/recipes?${sp.toString()}`);
  }

  return (
    <div className="flex items-center justify-center gap-2 py-6">
      <Button
        variant="outline"
        size="sm"
        className="cursor-pointer"
        disabled={page <= 1}
        onClick={() => go(page - 1)}
      >
        <ChevronLeft className="size-4" />
        上一页
      </Button>
      <span className="text-sm text-muted-foreground">
        {page} / {pages}
      </span>
      <Button
        variant="outline"
        size="sm"
        className="cursor-pointer"
        disabled={page >= pages}
        onClick={() => go(page + 1)}
      >
        下一页
        <ChevronRight className="size-4" />
      </Button>
    </div>
  );
}
