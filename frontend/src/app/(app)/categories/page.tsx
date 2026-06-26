import Link from "next/link";
import { FolderTree, ArrowRight } from "lucide-react";
import { Card } from "@/components/ui/card";
import { listCategories } from "@/lib/api/recipes";
import { PageHeader, EmptyState } from "@/components/common/states";

export const dynamic = "force-dynamic";

export default async function CategoriesPage() {
  const categories = await listCategories().catch(() => []);

  return (
    <main className="mx-auto w-full max-w-6xl px-4 py-6">
      <PageHeader
        title="菜谱分类"
        description="按类别浏览中餐菜谱"
        icon={FolderTree}
      />
      <div className="mt-6">
        {!categories || categories.length === 0 ? (
          <EmptyState icon={FolderTree} title="暂无分类" />
        ) : (
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
            {categories.map((c) => (
              <Link key={c.id} href={`/categories/${c.id}`} className="block cursor-pointer">
                <Card className="group flex items-center justify-between p-4 transition-colors hover:border-primary/40">
                  <div>
                    <h3 className="font-medium">{c.name}</h3>
                    {c.recipeCount !== undefined && (
                      <p className="mt-0.5 text-xs text-muted-foreground">
                        {c.recipeCount} 道菜
                      </p>
                    )}
                  </div>
                  <ArrowRight className="size-4 text-muted-foreground transition-transform group-hover:translate-x-0.5" />
                </Card>
              </Link>
            ))}
          </div>
        )}
      </div>
    </main>
  );
}
