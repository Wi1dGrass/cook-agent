export function formatDate(iso?: string | null): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function truncate(s: string | null | undefined, n = 80): string {
  if (!s) return "";
  return s.length > n ? s.slice(0, n) + "…" : s;
}

export function parseNutrition(json?: string | null): Record<string, string> | null {
  if (!json) return null;
  try {
    return JSON.parse(json) as Record<string, string>;
  } catch {
    return null;
  }
}

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8088/api";

export function resolveImageUrl(url?: string | null): string | null {
  if (!url) return null;
  if (url.startsWith("images/") || url.startsWith("/images/")) {
    return `${API_BASE}/${url.replace(/^\//, "")}`;
  }
  if (url.startsWith("http://") || url.startsWith("https://")) return url;
  return `${API_BASE}/${url.replace(/^\//, "")}`;
}
