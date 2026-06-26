import { NextRequest, NextResponse } from "next/server";
import type { UserRole } from "@/lib/api/types";

const ROLE_RANK: Record<UserRole, number> = { CHEF: 1, MANAGER: 2, ADMIN: 3 };

function getUser(req: NextRequest): { role: UserRole } | null {
  const raw = req.cookies.get("cookmanus_user")?.value;
  if (!raw) return null;
  try {
    return JSON.parse(raw) as { role: UserRole };
  } catch {
    return null;
  }
}

function hasMin(role: UserRole, min: UserRole): boolean {
  return ROLE_RANK[role] >= ROLE_RANK[min];
}

const AUTHED = ["/recipes/new", "/favorites", "/history"];
const CHEF_ONLY = ["/recipes/new", "/favorites", "/history"];
const ADMIN_ONLY = ["/admin"];

export function proxy(req: NextRequest) {
  const { pathname } = req.nextUrl;
  const user = getUser(req);

  const isEdit = pathname.startsWith("/recipes/") && pathname.endsWith("/edit");
  const needsAuth =
    AUTHED.some((p) => pathname === p || pathname.startsWith(p + "/")) || isEdit;
  const needsChef =
    CHEF_ONLY.some((p) => pathname === p || pathname.startsWith(p + "/")) || isEdit;
  const needsAdmin = ADMIN_ONLY.some((p) => pathname === p || pathname.startsWith(p + "/"));

  if ((needsAuth || needsChef || needsAdmin) && !user) {
    const url = req.nextUrl.clone();
    url.pathname = "/login";
    url.searchParams.set("redirect", pathname);
    return NextResponse.redirect(url);
  }
  if (needsAdmin && user && !hasMin(user.role, "ADMIN")) {
    return NextResponse.redirect(new URL("/", req.url));
  }
  if (needsChef && user && !hasMin(user.role, "CHEF")) {
    return NextResponse.redirect(new URL("/", req.url));
  }
  return NextResponse.next();
}

export const config = {
  matcher: [
    "/favorites/:path*",
    "/history/:path*",
    "/recipes/:path*",
    "/admin/:path*",
  ],
};
