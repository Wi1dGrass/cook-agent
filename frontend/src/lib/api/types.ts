export type UserRole = "CHEF" | "MANAGER" | "ADMIN";

export interface UserResponse {
  id: number;
  username: string;
  role: UserRole;
  phone?: string | null;
  enabled: number;
  createdAt: string;
}

export interface LoginResponse {
  token: string;
  userId: number;
  username: string;
  role: UserRole;
}

export interface AuthUser {
  userId: number;
  username: string;
  role: UserRole;
}

export interface CategoryResponse {
  id: number;
  name: string;
  dirName?: string;
  sortOrder?: number;
  recipeCount?: number;
}

export interface RecipeSummary {
  id: number;
  name: string;
  categoryId: number;
  alias?: string | null;
  imageUrl?: string | null;
  summary?: string | null;
  createdAt?: string;
}

export interface IngredientInfo {
  id: number;
  name: string;
  brand?: string | null;
  quantity?: string | null;
  note?: string | null;
}

export interface StepInfo {
  stepOrder: number;
  description: string;
}

export interface RecipeDetail {
  id: number;
  name: string;
  categoryId: number;
  alias?: string | null;
  imageUrl?: string | null;
  summary?: string | null;
  remark?: string | null;
  nutritionJson?: string | null;
  rawMarkdown?: string | null;
  sourceFile?: string | null;
  ingredients: IngredientInfo[];
  steps: StepInfo[];
  createdAt?: string;
  favorited?: boolean;
}

export interface PageResult<T> {
  page: number;
  pageSize: number;
  total: number;
  pages: number;
  records: T[];
}

export interface SearchResult {
  count: number;
  recipes: RecipeSummary[];
}

export interface IngredientMatchResult {
  recipe: RecipeSummary;
  matchedIngredients: string[];
}

export interface IngredientSearchResult {
  count: number;
  matchMode: "any" | "all";
  results: IngredientMatchResult[];
}

export interface IngredientCompareItem {
  name: string;
  quantities: Record<string, string>;
}

export interface CompareResult {
  recipes: RecipeSummary[];
  commonIngredients: IngredientCompareItem[];
  uniqueIngredients: Record<string, string[]>;
}

export interface RecommendCombo {
  meat: RecipeSummary | null;
  veggie: RecipeSummary | null;
  soup: RecipeSummary | null;
}

export interface DailyRecommendResult {
  season: string;
  dietAdvice: string;
  combos: RecommendCombo[];
}

export interface ChatHistory {
  id: number;
  userId: number;
  conversationId: string;
  query: string;
  reply: string;
  channel: "CHAT" | "AGENT";
  title?: string | null;
  createdAt: string;
}

export interface ChatNewResponse {
  conversationId: string;
  reply: string;
}

export interface ChatSendResponse {
  reply: string;
}

export interface AgentChatResponse {
  conversationId: string;
  reply: string;
  status?: string;
}

/** 会话摘要 — 服务端按 conversationId 分组返回 */
export interface SessionSummary {
  conversationId: string;
  title: string | null;
  firstQuery: string | null;
  channel: "CHAT" | "AGENT";
  messageCount: number;
  status?: string | null;
  createdAt: string;
  lastAt: string;
}

/** Agent 会话消息视图 */
export interface AgentSessionMessageView {
  type: "USER" | "ASSISTANT" | "SYSTEM" | "TOOL";
  content: string;
}

/** Agent 会话详情 */
export interface AgentSessionResponse {
  conversationId: string;
  title: string;
  status: string;
  compressed: number;
  messages: AgentSessionMessageView[];
}

/** Agent 会话列表项 */
export interface AgentSessionListItem {
  conversationId: string;
  title: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface ErrorResponse {
  code: string;
  message: string;
  status: number;
}

export interface CreateRecipeRequest {
  name: string;
  categoryId: number;
  alias?: string;
  imageUrl?: string;
  summary?: string;
  remark?: string;
  nutritionJson?: string;
  ingredients: {
    name: string;
    brand?: string;
    quantity?: string;
    note?: string;
    sortOrder?: number;
  }[];
  steps: { stepOrder: number; description: string }[];
}

export type UpdateRecipeRequest = Partial<CreateRecipeRequest>;
