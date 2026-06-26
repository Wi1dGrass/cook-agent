import { NextRequest, NextResponse } from "next/server";
import { API_BASE_URL, AUTH_COOKIE } from "@/lib/api/client";
import type { ErrorResponse } from "@/lib/api/types";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  return handle(req);
}
export async function POST(req: NextRequest) {
  return handle(req);
}
export async function PUT(req: NextRequest) {
  return handle(req);
}
export async function DELETE(req: NextRequest) {
  return handle(req);
}

async function handle(req: NextRequest) {
  const path = req.nextUrl.searchParams.get("p");
  if (!path) {
    return NextResponse.json(
      { code: "PARAM_INVALID", message: "missing path", status: 400 } satisfies ErrorResponse,
      { status: 400 }
    );
  }

  const token = req.cookies.get(AUTH_COOKIE)?.value;
  const target = new URL(`${API_BASE_URL}${path.startsWith("/") ? "" : "/"}${path}`);
  req.nextUrl.searchParams.forEach((v, k) => {
    if (k !== "p") target.searchParams.set(k, v);
  });

  const headers: Record<string, string> = { Accept: "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;

  const init: RequestInit = { method: req.method, headers };
  if (req.method !== "GET" && req.method !== "DELETE") {
    headers["Content-Type"] = req.headers.get("content-type") ?? "application/json";
    init.body = await req.text();
  }

  const upstream = await fetch(target.toString(), init);
  const text = await upstream.text();
  const contentType = upstream.headers.get("content-type") ?? "application/json";

  return new NextResponse(text, {
    status: upstream.status,
    headers: { "content-type": contentType },
  });
}
