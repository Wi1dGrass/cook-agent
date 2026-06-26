import { clientApiFetch } from "./client-fetch";
import type { RecipeSummary } from "./types";

export function listFavorites() {
  return clientApiFetch<RecipeSummary[]>("/user/favorites");
}

export function addFavorite(recipeId: number) {
  return clientApiFetch<{ recipeId: number; favorited: boolean }>(
    `/user/favorites/${recipeId}`,
    { method: "POST" }
  );
}

export function removeFavorite(recipeId: number) {
  return clientApiFetch<{ recipeId: number; favorited: boolean }>(
    `/user/favorites/${recipeId}`,
    { method: "DELETE" }
  );
}
