import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowLeft, FolderTree } from "lucide-react";
import { recipesByCategory } from "@/lib/api/recipes";
import { RecipeCardGrid } from "@/components/recipe/recipe-card";
import { EmptyState } from "@/components/common/states";
import { ApiError } from "@/lib/api/errors";

export const dynamic = "force-dynamic";

interface PageProps {
  params: Promise<{ id: string }>;
}

export default async function CategoryDetailPage({ params }: PageProps) {
  const { id } = await params;
  const numId = Number(id);
  if (!numId) notFound();

  let data;
  try {
    data = await recipesByCategory(numId);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  }

  return (
    <main className="mx-auto w-full max-w-6xl px-4 py-6">
      <Link
        href="/categories"
        className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors cursor-pointer"
      >
        <ArrowLeft className="size-4" />
        返回分类
      </Link>
      <h1 className="mt-4 flex items-center gap-2 text-xl font-semibold tracking-tight">
        <FolderTree className="size-5 text-primary" />
        {data.categoryName}
        <span className="text-sm font-normal text-muted-foreground">
          · {data.count} 道菜
        </span>
      </h1>
      <div className="mt-6">
        {data.recipes.length === 0 ? (
          <EmptyState icon={FolderTree} title="该分类下暂无菜谱" />
        ) : (
          <RecipeCardGrid recipes={data.recipes} />
        )}
      </div>
    </main>
  );
}
