"use client";

import Link from "next/link";
import { UtensilsCrossed } from "lucide-react";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import { truncate } from "@/lib/utils/format";
import type { RecipeSummary } from "@/lib/api/types";

export function RecipeCard({ recipe }: { recipe: RecipeSummary }) {
  return (
    <Link href={`/recipes/${recipe.id}`} className="block cursor-pointer">
      <Card className="group h-full overflow-hidden p-0 transition-colors hover:border-primary/40 hover:shadow-sm">
        <div className="relative aspect-[4/3] w-full overflow-hidden bg-muted">
          {recipe.imageUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={recipe.imageUrl}
              alt={recipe.name}
              loading="lazy"
              className="size-full object-cover transition-transform duration-300 group-hover:scale-[1.03]"
            />
          ) : (
            <div className="flex size-full items-center justify-center text-muted-foreground">
              <UtensilsCrossed className="size-8" />
            </div>
          )}
        </div>
        <div className="space-y-1.5 p-3">
          <h3 className="line-clamp-1 font-medium leading-snug">{recipe.name}</h3>
          {recipe.alias && (
            <p className="line-clamp-1 text-xs text-muted-foreground">{recipe.alias}</p>
          )}
          {recipe.summary && (
            <p className={cn("line-clamp-2 text-xs text-muted-foreground")}>
              {truncate(recipe.summary, 60)}
            </p>
          )}
        </div>
      </Card>
    </Link>
  );
}

export function RecipeCardGrid({ recipes }: { recipes: RecipeSummary[] }) {
  if (recipes.length === 0) return null;
  return (
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
      {recipes.map((r) => (
        <RecipeCard key={r.id} recipe={r} />
      ))}
    </div>
  );
}
