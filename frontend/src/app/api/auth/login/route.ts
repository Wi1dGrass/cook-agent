import { NextRequest, NextResponse } from "next/server";
import { API_BASE_URL, AUTH_COOKIE, USER_COOKIE } from "@/lib/api/client";
import type { LoginResponse, ErrorResponse } from "@/lib/api/types";

export const runtime = "nodejs";

export async function POST(req: NextRequest) {
  let body: { username?: string; password?: string };
  try {
    body = await req.json();
  } catch {
    return NextResponse.json(
      { code: "PARAM_INVALID", message: "请求体无效", status: 400 } satisfies ErrorResponse,
      { status: 400 }
    );
  }

  const res = await fetch(`${API_BASE_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify(body),
  });

  const text = await res.text();
  const data = text ? (JSON.parse(text) as LoginResponse | ErrorResponse) : null;

  if (!res.ok || !data || !("token" in data)) {
    const err = (data as ErrorResponse) ?? {
      code: "HTTP_" + res.status,
      message: "登录失败",
      status: res.status,
    };
    return NextResponse.json(err, { status: res.status });
  }

  const login = data as LoginResponse;
  const user = {
    userId: login.userId,
    username: login.username,
    role: login.role,
  };

  const response = NextResponse.json(user);
  response.cookies.set(AUTH_COOKIE, login.token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: 60 * 120,
  });
  response.cookies.set(USER_COOKIE, JSON.stringify(user), {
    httpOnly: false,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: 60 * 120,
  });
  return response;
}
