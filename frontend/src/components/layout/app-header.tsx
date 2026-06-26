"use client";

import * as React from "react";
import { useTheme } from "next-themes";
import { Moon, Sun, Activity } from "lucide-react";
import { Button } from "@/components/ui/button";
import { SidebarTrigger } from "@/components/ui/sidebar";
import { cn } from "@/lib/utils";

export function AppHeader() {
  const { theme, setTheme } = useTheme();
  const [mounted, setMounted] = React.useState(false);
  const [health, setHealth] = React.useState<"up" | "down" | "checking">("checking");

  React.useEffect(() => setMounted(true), []);

  React.useEffect(() => {
    let active = true;
    async function check() {
      try {
        const res = await fetch(
          `${process.env.NEXT_PUBLIC_API_BASE_URL}/health`,
          { cache: "no-store" }
        );
        if (active) setHealth(res.ok ? "up" : "down");
      } catch {
        if (active) setHealth("down");
      }
    }
    check();
    const t = setInterval(check, 30000);
    return () => {
      active = false;
      clearInterval(t);
    };
  }, []);

  return (
    <header className="sticky top-0 z-20 flex h-14 items-center gap-2 border-b border-border bg-background/80 px-3 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <SidebarTrigger className="cursor-pointer" />
      <div className="flex-1" />
      <div className="flex items-center gap-2 text-xs text-muted-foreground">
        <span className="inline-flex items-center gap-1.5">
          <span
            className={cn(
              "inline-block size-2 rounded-full",
              health === "up" && "bg-emerald-500",
              health === "down" && "bg-destructive",
              health === "checking" && "bg-muted-foreground animate-pulse"
            )}
          />
          <Activity className="size-3.5 hidden sm:inline" />
          <span className="hidden sm:inline">
            {health === "up" ? "服务正常" : health === "down" ? "服务离线" : "检测中"}
          </span>
        </span>
      </div>
      <Button
        variant="ghost"
        size="icon"
        className="size-9 cursor-pointer"
        aria-label="切换主题"
        onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
      >
        {mounted && theme === "dark" ? (
          <Sun className="size-4" />
        ) : (
          <Moon className="size-4" />
        )}
      </Button>
    </header>
  );
}
