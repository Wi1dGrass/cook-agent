import { NextResponse } from "next/server";
import { AUTH_COOKIE, USER_COOKIE } from "@/lib/api/client";

export const runtime = "nodejs";

export async function POST() {
  const response = NextResponse.json({ ok: true });
  response.cookies.set(AUTH_COOKIE, "", { path: "/", maxAge: 0 });
  response.cookies.set(USER_COOKIE, "", { path: "/", maxAge: 0 });
  return response;
}
