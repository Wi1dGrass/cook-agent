"use client";

import Link from "next/link";
import { UtensilsCrossed } from "lucide-react";
import { Card } from "@/components/ui/card";
import { truncate } from "@/lib/utils/format";
import type { RecipeSummary } from "@/lib/api/types";

export function RecipeCard({ recipe }: { recipe: RecipeSummary }) {
  return (
    <Link href={`/recipes/${recipe.id}`} className="block cursor-pointer">
      <Card className="card-hover group h-full overflow-hidden p-0 border-border/70">
        <div className="relative aspect-[4/3] w-full overflow-hidden bg-gradient-to-br from-muted to-muted/40">
          {recipe.imageUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={recipe.imageUrl}
              alt={recipe.name}
              loading="lazy"
              className="size-full object-cover transition-transform duration-500 ease-out group-hover:scale-[1.06]"
            />
          ) : (
            <div className="flex size-full flex-col items-center justify-center gap-2 text-muted-foreground/60">
              <UtensilsCrossed className="size-9 transition-transform duration-300 group-hover:scale-110 group-hover:text-primary/50" />
              <span className="text-[10px] uppercase tracking-widest">CookManus</span>
            </div>
          )}
          <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-black/40 via-transparent to-transparent opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
        </div>
        <div className="space-y-1.5 p-3.5">
          <h3 className="line-clamp-1 font-medium leading-snug transition-colors group-hover:text-primary">
            {recipe.name}
          </h3>
          {recipe.alias && (
            <p className="line-clamp-1 text-xs text-muted-foreground/80">{recipe.alias}</p>
          )}
          {recipe.summary && (
            <p className="line-clamp-2 text-xs text-muted-foreground">
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
