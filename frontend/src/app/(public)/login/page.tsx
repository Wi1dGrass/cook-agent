"use client";

import * as React from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { ChefHat, Loader2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { loginSchema, type LoginValues } from "@/lib/validations/auth";
import { useAuthStore } from "@/lib/store/auth-store";
import type { AuthUser, ErrorResponse } from "@/lib/api/types";

export default function LoginPage() {
  return (
    <React.Suspense fallback={null}>
      <LoginForm />
    </React.Suspense>
  );
}

function LoginForm() {
  const router = useRouter();
  const params = useSearchParams();
  const redirect = params.get("redirect") ?? "/";
  const setUser = useAuthStore((s) => s.setUser);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { username: "", password: "" },
  });

  async function onSubmit(values: LoginValues) {
    try {
      const res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(values),
      });
      const data = (await res.json()) as AuthUser | ErrorResponse;
      if (!res.ok || !("userId" in data)) {
        const err = data as ErrorResponse;
        toast.error(err.message ?? "登录失败");
        return;
      }
      setUser(data as AuthUser);
      toast.success("登录成功");
      router.push(redirect);
      router.refresh();
    } catch {
      toast.error("网络异常，请稍后重试");
    }
  }

  return (
    <main className="relative flex min-h-svh flex-col items-center justify-center overflow-hidden px-6 py-12">
      <div className="pointer-events-none absolute inset-0 -z-20 bg-grid opacity-50" />
      <div className="pointer-events-none absolute inset-x-0 top-0 -z-10 h-[50vh] bg-[radial-gradient(60%_50%_at_50%_0%,color-mix(in_srgb,var(--ai)_20%,transparent),transparent)]" />
      <div className="w-full max-w-sm animate-fade-up">
        <div className="mb-8 flex flex-col items-center text-center">
          <div className="flex size-12 items-center justify-center rounded-xl bg-gradient-to-br from-primary to-primary/70 text-primary-foreground shadow-sm">
            <ChefHat className="size-6" />
          </div>
          <h1 className="mt-4 text-2xl font-semibold tracking-tight">
            登录 <span className="text-gradient-ai">CookManus</span>
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">解锁收藏、历史与创作权限</p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <div className="space-y-2">
            <Label htmlFor="username">用户名</Label>
            <Input
              id="username"
              autoComplete="username"
              placeholder="请输入用户名"
              aria-invalid={!!errors.username}
              {...register("username")}
            />
            {errors.username && (
              <p className="text-xs text-destructive">{errors.username.message}</p>
            )}
          </div>
          <div className="space-y-2">
            <Label htmlFor="password">密码</Label>
            <Input
              id="password"
              type="password"
              autoComplete="current-password"
              placeholder="请输入密码"
              aria-invalid={!!errors.password}
              {...register("password")}
            />
            {errors.password && (
              <p className="text-xs text-destructive">{errors.password.message}</p>
            )}
          </div>
          <Button type="submit" className="w-full cursor-pointer glow-ai" disabled={isSubmitting}>
            {isSubmitting && <Loader2 className="size-4 animate-spin" />}
            登录
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-muted-foreground">
          还没有账号？{" "}
          <Link
            href="/register"
            className="font-medium text-primary hover:underline cursor-pointer"
          >
            立即注册
          </Link>
        </p>
      </div>
    </main>
  );
}
