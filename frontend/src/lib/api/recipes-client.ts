"use client";

import { clientApiFetch } from "./client-fetch";
import type {
  RecipeDetail,
  SearchResult,
  CompareResult,
  DailyRecommendResult,
  IngredientSearchResult,
  CategoryResponse,
  RecipeSummary,
  CreateRecipeRequest,
  UpdateRecipeRequest,
} from "./types";

export function semanticSearch(params: {
  keyword: string;
  topK?: number;
  category?: string;
}) {
  return clientApiFetch<SearchResult>("/recipes/search", { query: params });
}

export function recommend(params: { criteria: string; count?: number; category?: string }) {
  return clientApiFetch<SearchResult>("/recipes/recommend", { query: params });
}

export function compare(names: string) {
  return clientApiFetch<CompareResult>("/recipes/compare", { query: { names } });
}

export function dailyRecommend(params: { preference?: string; comboCount?: number }) {
  return clientApiFetch<DailyRecommendResult>("/recipes/daily-recommend", { query: params });
}

export function nutrition(name: string) {
  return clientApiFetch<RecipeDetail>("/recipes/nutrition", { query: { name } });
}

export function ingredientSearch(params: { names: string; mode?: "any" | "all" }) {
  return clientApiFetch<IngredientSearchResult>("/ingredients/search", { query: params });
}

export function listCategoriesClient() {
  return clientApiFetch<CategoryResponse[]>("/categories");
}

export function searchByNameClient(name: string) {
  return clientApiFetch<SearchResult>("/recipes", { query: { name } });
}

export function deleteRecipeClient(id: number) {
  return clientApiFetch<{ id: number; deleted: boolean }>(`/recipes/${id}`, {
    method: "DELETE",
  });
}

export function createRecipeClient(body: CreateRecipeRequest) {
  return clientApiFetch<RecipeDetail>("/recipes", { method: "POST", body });
}

export function updateRecipeClient(id: number, body: UpdateRecipeRequest) {
  return clientApiFetch<RecipeDetail>(`/recipes/${id}`, { method: "PUT", body });
}

export function getRecipeDetailClient(id: number) {
  return clientApiFetch<RecipeDetail>(`/recipes/${id}/detail`);
}

export type { RecipeSummary };
