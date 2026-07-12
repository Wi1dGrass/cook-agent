"use client";

import Link from "next/link";
import { Heart, History, MessageSquare, ChefHat, LogOut } from "lucide-react";
import { useAuthStore } from "@/lib/store/auth-store";
import { ROLE_LABEL } from "@/lib/nav";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { toast } from "sonner";

export default function ProfilePage() {
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);

  if (!user) {
    return (
      <main className="mx-auto w-full max-w-2xl px-4 py-6">
        <Card>
          <CardContent className="flex flex-col items-center gap-4 py-12">
            <ChefHat className="size-10 text-muted-foreground" />
            <p className="text-muted-foreground">请先登录以查看个人中心</p>
            <Button asChild className="cursor-pointer">
              <Link href="/login">去登录</Link>
            </Button>
          </CardContent>
        </Card>
      </main>
    );
  }

  async function handleLogout() {
    try {
      await fetch("/api/auth/logout", { method: "POST" });
    } finally {
      logout();
      toast.success("已退出登录");
      window.location.href = "/";
    }
  }

  const links = [
    { title: "我的收藏", desc: "查看已收藏的菜谱", href: "/favorites", icon: Heart },
    { title: "对话历史", desc: "回顾历史对话记录", href: "/history", icon: History },
    { title: "AI 对话", desc: "开始新的对话", href: "/chat", icon: MessageSquare },
  ];

  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-6">
      <Card>
        <CardHeader>
          <CardTitle>个人中心</CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="flex items-center gap-4">
            <div className="flex size-16 items-center justify-center rounded-full bg-gradient-to-br from-primary to-primary/70 text-primary-foreground text-2xl font-semibold shadow-sm">
              {user.username.slice(0, 1).toUpperCase()}
            </div>
            <div className="space-y-1">
              <p className="text-xl font-semibold">{user.username}</p>
              <p className="text-sm text-muted-foreground">
                角色：{ROLE_LABEL[user.role]}
              </p>
              <p className="text-xs text-muted-foreground">ID：{user.userId}</p>
            </div>
          </div>

          <div className="space-y-2">
            {links.map((link) => {
              const Icon = link.icon;
              return (
                <Link
                  key={link.href}
                  href={link.href}
                  className="card-hover flex items-center gap-3 rounded-xl border border-border bg-card/60 p-4 cursor-pointer"
                >
                  <div className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
                    <Icon className="size-4" />
                  </div>
                  <div className="flex-1">
                    <p className="font-medium">{link.title}</p>
                    <p className="text-xs text-muted-foreground">{link.desc}</p>
                  </div>
                </Link>
              );
            })}
          </div>

          <Button
            variant="outline"
            className="w-full cursor-pointer"
            onClick={handleLogout}
          >
            <LogOut className="size-4" />
            退出登录
          </Button>
        </CardContent>
      </Card>
    </main>
  );
}
