import "server-only";
import { cookies } from "next/headers";
import { ApiError, parseError } from "./errors";
import type { AuthUser, ErrorResponse } from "./types";

export const API_BASE_URL =
  process.env.API_BASE_URL ?? process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8088/api";

export const AUTH_COOKIE = "cookmanus_token";
export const USER_COOKIE = "cookmanus_user";

export async function getToken(): Promise<string | null> {
  const store = await cookies();
  return store.get(AUTH_COOKIE)?.value ?? null;
}

export async function getAuthUser(): Promise<AuthUser | null> {
  const store = await cookies();
  const raw = store.get(USER_COOKIE)?.value;
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthUser;
  } catch {
    return null;
  }
}

export interface ApiFetchOptions {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  body?: unknown;
  query?: Record<string, string | number | boolean | undefined | null>;
  cache?: RequestCache;
  revalidate?: number;
  signal?: AbortSignal;
}

function buildUrl(path: string, query?: ApiFetchOptions["query"]): string {
  const url = new URL(
    path.startsWith("http") ? path : `${API_BASE_URL}${path.startsWith("/") ? "" : "/"}${path}`
  );
  if (query) {
    for (const [k, v] of Object.entries(query)) {
      if (v !== undefined && v !== null && v !== "") {
        url.searchParams.set(k, String(v));
      }
    }
  }
  return url.toString();
}

export async function apiFetch<T>(
  path: string,
  opts: ApiFetchOptions = {}
): Promise<T> {
  const token = await getToken();
  const headers: Record<string, string> = {
    Accept: "application/json",
  };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (opts.body !== undefined) headers["Content-Type"] = "application/json";

  const init: RequestInit = {
    method: opts.method ?? "GET",
    headers,
    cache: opts.cache,
    signal: opts.signal,
  };
  if (opts.body !== undefined) init.body = JSON.stringify(opts.body);
  if (opts.revalidate !== undefined) {
    (init as RequestInit & { next?: { revalidate?: number } }).next = {
      revalidate: opts.revalidate,
    };
  }

  const res = await fetch(buildUrl(path, opts.query), init);

  if (res.status === 204) return undefined as T;
  if (!res.ok) {
    throw await parseError(res);
  }

  const text = await res.text();
  if (!text) return undefined as T;
  try {
    return JSON.parse(text) as T;
  } catch {
    return text as unknown as T;
  }
}

export { ApiError };
export type { ErrorResponse };
