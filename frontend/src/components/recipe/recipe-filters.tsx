"use client";

import * as React from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Search } from "lucide-react";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";

export function RecipeFilters({
  categories,
  currentKeyword,
  currentCategoryId,
}: {
  categories: { id: number; name: string }[];
  currentKeyword?: string;
  currentCategoryId?: number;
}) {
  const router = useRouter();
  const params = useSearchParams();
  const [keyword, setKeyword] = React.useState(currentKeyword ?? "");

  React.useEffect(() => setKeyword(currentKeyword ?? ""), [currentKeyword]);

  function update(next: { keyword?: string; categoryId?: string }) {
    const sp = new URLSearchParams(params.toString());
    if (next.keyword !== undefined) {
      if (next.keyword) sp.set("keyword", next.keyword);
      else sp.delete("keyword");
    }
    if (next.categoryId !== undefined) {
      if (next.categoryId && next.categoryId !== "all") sp.set("categoryId", next.categoryId);
      else sp.delete("categoryId");
    }
    sp.delete("pageNum");
    router.push(`/recipes?${sp.toString()}`);
  }

  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
      <div className="relative flex-1">
        <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") update({ keyword });
          }}
          placeholder="按菜名搜索…"
          className="pl-9"
          aria-label="按菜名搜索"
        />
      </div>
      <Select
        value={currentCategoryId ? String(currentCategoryId) : "all"}
        onValueChange={(v) => update({ categoryId: v })}
      >
        <SelectTrigger className="sm:w-40" aria-label="分类筛选">
          <SelectValue placeholder="全部分类" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="all">全部分类</SelectItem>
          {categories.map((c) => (
            <SelectItem key={c.id} value={String(c.id)}>
              {c.name}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <Button
        variant="secondary"
        className="cursor-pointer"
        onClick={() => update({ keyword })}
      >
        搜索
      </Button>
    </div>
  );
}
