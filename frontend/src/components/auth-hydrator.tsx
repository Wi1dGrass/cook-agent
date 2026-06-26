"use client";

import { useEffect } from "react";
import { useAuthStore } from "@/lib/store/auth-store";
import type { AuthUser } from "@/lib/api/types";

function readCookie(name: string): string | null {
  if (typeof document === "undefined") return null;
  const match = document.cookie.match(
    new RegExp("(?:^|; )" + name.replace(/[.$?*|{}()[\]\\/+^]/g, "\\$&") + "=([^;]*)")
  );
  return match ? decodeURIComponent(match[1]) : null;
}

export function AuthHydrator() {
  const setUser = useAuthStore((s) => s.setUser);

  useEffect(() => {
    const raw = readCookie("cookmanus_user");
    if (raw) {
      try {
        setUser(JSON.parse(raw) as AuthUser);
        return;
      } catch {
        /* ignore */
      }
    }
    setUser(null);
  }, [setUser]);

  return null;
}
