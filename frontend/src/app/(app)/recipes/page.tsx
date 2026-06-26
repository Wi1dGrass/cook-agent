import * as React from "react";
import { UtensilsCrossed } from "lucide-react";

import { listRecipes, listCategories } from "@/lib/api/recipes";
import { RecipeCardGrid } from "@/components/recipe/recipe-card";
import { RecipeFilters } from "@/components/recipe/recipe-filters";
import { RecipePagination } from "@/components/recipe/recipe-pagination";
import { PageHeader, EmptyState } from "@/components/common/states";

export const dynamic = "force-dynamic";

interface PageProps {
  searchParams: Promise<{ [k: string]: string | string[] | undefined }>;
}

export default async function RecipesPage({ searchParams }: PageProps) {
  const sp = await searchParams;
  const pageNum = Number(sp.pageNum ?? 1) || 1;
  const pageSize = 20;
  const keyword = typeof sp.keyword === "string" ? sp.keyword : undefined;
  const categoryId = sp.categoryId ? Number(sp.categoryId) : undefined;

  const [page, categories] = await Promise.all([
    listRecipes({ pageNum, pageSize, keyword, categoryId }).catch(() => null),
    listCategories().catch(() => []),
  ]);

  return (
    <main className="mx-auto w-full max-w-6xl px-4 py-6">
      <PageHeader
        title="菜谱浏览"
        description="中餐菜谱库 · 按名称或分类浏览"
        icon={UtensilsCrossed}
      />

      <React.Suspense fallback={null}>
        <div className="mt-6">
          <RecipeFilters
            categories={(categories ?? []).map((c) => ({ id: c.id, name: c.name }))}
            currentKeyword={keyword}
            currentCategoryId={categoryId}
          />
        </div>
      </React.Suspense>

      <div className="mt-6">
        {!page || page.records.length === 0 ? (
          <EmptyState
            icon={UtensilsCrossed}
            title={keyword ? `未找到与「${keyword}」相关的菜谱` : "暂无菜谱"}
            description="试试调整关键词或分类筛选"
          />
        ) : (
          <>
            <RecipeCardGrid recipes={page.records} />
            <RecipePagination page={page.page} pages={page.pages} />
          </>
        )}
      </div>
    </main>
  );
}
