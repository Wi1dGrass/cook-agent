"use client";

import { ApiError, parseErrorClient } from "./errors";

export async function clientApiFetch<T>(
  path: string,
  opts: {
    method?: "GET" | "POST" | "PUT" | "DELETE";
    body?: unknown;
    query?: Record<string, string | number | boolean | undefined | null>;
    signal?: AbortSignal;
  } = {}
): Promise<T> {
  const url = new URL("/api/proxy/[...path]", window.location.origin);
  url.searchParams.set("p", path.startsWith("/") ? path : `/${path}`);
  if (opts.query) {
    for (const [k, v] of Object.entries(opts.query)) {
      if (v !== undefined && v !== null && v !== "") url.searchParams.set(k, String(v));
    }
  }

  const headers: Record<string, string> = { Accept: "application/json" };
  if (opts.body !== undefined) headers["Content-Type"] = "application/json";

  const res = await fetch(url.toString(), {
    method: opts.method ?? "GET",
    headers,
    body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
    signal: opts.signal,
  });

  if (res.status === 204) return undefined as T;
  if (!res.ok) throw await parseErrorClient(res);

  const text = await res.text();
  if (!text) return undefined as T;
  try {
    return JSON.parse(text) as T;
  } catch {
    return text as unknown as T;
  }
}

export { ApiError };
