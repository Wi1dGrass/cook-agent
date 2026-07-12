"use client";

import Link from "next/link";
import { ArrowRight, LogOut } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/lib/store/auth-store";
import { ROLE_LABEL } from "@/lib/nav";
import { toast } from "sonner";

export function HomeNav() {
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);

  async function handleLogout() {
    try {
      await fetch("/api/auth/logout", { method: "POST" });
    } finally {
      logout();
      toast.success("已退出登录");
    }
  }

  return (
    <div className="flex items-center gap-2">
      {user ? (
        <>
          <span className="flex items-center gap-2 text-sm">
            <span className="flex size-7 items-center justify-center rounded-full bg-secondary text-secondary-foreground text-xs font-medium">
              {user.username.slice(0, 1).toUpperCase()}
            </span>
            <span className="hidden sm:inline">{user.username}</span>
            <span className="hidden text-xs text-muted-foreground sm:inline">
              {ROLE_LABEL[user.role]}
            </span>
          </span>
          <Button asChild size="sm" className="cursor-pointer">
            <Link href="/chat">
              进入工作台
              <ArrowRight className="size-4" />
            </Link>
          </Button>
          <Button
            size="sm"
            variant="ghost"
            className="cursor-pointer"
            onClick={handleLogout}
            aria-label="退出登录"
          >
            <LogOut className="size-4" />
          </Button>
        </>
      ) : (
        <>
          <Button asChild variant="ghost" size="sm" className="cursor-pointer">
            <Link href="/login">登录</Link>
          </Button>
          <Button asChild size="sm" className="cursor-pointer">
            <Link href="/chat">
              开始使用
              <ArrowRight className="size-4" />
            </Link>
          </Button>
        </>
      )}
    </div>
  );
}
