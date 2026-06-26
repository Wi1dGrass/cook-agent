import { apiFetch } from "./client";
import type {
  RecipeSummary,
  RecipeDetail,
  CategoryResponse,
  PageResult,
  SearchResult,
  CompareResult,
  DailyRecommendResult,
  IngredientSearchResult,
  CreateRecipeRequest,
  UpdateRecipeRequest,
} from "./types";

export function listRecipes(params: {
  pageNum?: number;
  pageSize?: number;
  categoryId?: number;
  keyword?: string;
}) {
  return apiFetch<PageResult<RecipeSummary>>("/recipes/page", { query: params });
}

export function getRecipeSummary(id: number) {
  return apiFetch<RecipeSummary>(`/recipes/${id}`);
}

export function getRecipeDetail(id: number) {
  return apiFetch<RecipeDetail>(`/recipes/${id}/detail`);
}

export function searchByName(name: string) {
  return apiFetch<SearchResult>("/recipes", { query: { name } });
}

export function listCategories() {
  return apiFetch<CategoryResponse[]>("/categories");
}

export function recipesByCategory(id: number) {
  return apiFetch<{ categoryId: number; categoryName: string; count: number; recipes: RecipeSummary[] }>(
    `/categories/${id}/recipes`
  );
}

export function semanticSearch(params: {
  keyword: string;
  topK?: number;
  category?: string;
}) {
  return apiFetch<SearchResult>("/recipes/search", { query: params });
}

export function recommend(params: { criteria: string; count?: number; category?: string }) {
  return apiFetch<SearchResult>("/recipes/recommend", { query: params });
}

export function compare(names: string) {
  return apiFetch<CompareResult>("/recipes/compare", { query: { names } });
}

export function dailyRecommend(params: { preference?: string; comboCount?: number }) {
  return apiFetch<DailyRecommendResult>("/recipes/daily-recommend", { query: params });
}

export function nutrition(name: string) {
  return apiFetch<RecipeDetail>("/recipes/nutrition", { query: { name } });
}

export function ingredientSearch(params: { names: string; mode?: "any" | "all" }) {
  return apiFetch<IngredientSearchResult>("/ingredients/search", { query: params });
}

export function createRecipe(body: CreateRecipeRequest) {
  return apiFetch<RecipeDetail>("/recipes", { method: "POST", body });
}

export function updateRecipe(id: number, body: UpdateRecipeRequest) {
  return apiFetch<RecipeDetail>(`/recipes/${id}`, { method: "PUT", body });
}

export function deleteRecipe(id: number) {
  return apiFetch<{ id: number; deleted: boolean }>(`/recipes/${id}`, { method: "DELETE" });
}
