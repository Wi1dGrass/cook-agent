import { NextRequest } from "next/server";
import { API_BASE_URL, AUTH_COOKIE } from "@/lib/api/client";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

/**
 * SSE 代理路由 — 为 Agent 流式对话注入 JWT 认证。
 * <p>
 * 前端无法读取 httpOnly 的 cookmanus_token cookie，因此通过本服务端路由代理
 * SSE 请求，在服务端读取 cookie 并附加 Authorization 头，再以流式透传响应。
 */
export async function GET(req: NextRequest) {
  const message = req.nextUrl.searchParams.get("message");
  const conversationId = req.nextUrl.searchParams.get("conversationId");

  if (!message) {
    return new Response("message is required", { status: 400 });
  }

  const token = req.cookies.get(AUTH_COOKIE)?.value;
  const target = new URL(`${API_BASE_URL}/agent/chat/stream`);
  target.searchParams.set("message", message);
  if (conversationId) {
    target.searchParams.set("conversationId", conversationId);
  }

  const headers: Record<string, string> = { Accept: "text/event-stream" };
  if (token) headers.Authorization = `Bearer ${token}`;

  const upstream = await fetch(target.toString(), { method: "GET", headers });

  if (!upstream.ok || !upstream.body) {
    return new Response(`Agent stream failed: ${upstream.status}`, {
      status: upstream.status,
    });
  }

  // 流式透传 SSE 响应
  return new Response(upstream.body, {
    status: upstream.status,
    headers: {
      "content-type": "text/event-stream",
      "cache-control": "no-cache",
      connection: "keep-alive",
    },
  });
}
