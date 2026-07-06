"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { ChefHat, Loader2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { registerSchema, type RegisterValues } from "@/lib/validations/auth";
import type { ErrorResponse, UserResponse } from "@/lib/api/types";

export default function RegisterPage() {
  const router = useRouter();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { username: "", password: "", phone: "" },
  });

  async function onSubmit(values: RegisterValues) {
    try {
      const body: Record<string, string> = {
        username: values.username,
        password: values.password,
      };
      if (values.phone) body.phone = values.phone;

      const res = await fetch(
        `${process.env.NEXT_PUBLIC_API_BASE_URL}/auth/register`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        }
      );
      const data = (await res.json()) as UserResponse | ErrorResponse;
      if (!res.ok || !("id" in data)) {
        const err = data as ErrorResponse;
        toast.error(err.message ?? "注册失败");
        return;
      }
      toast.success("注册成功，请登录");
      router.push("/login");
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
            注册 <span className="text-gradient-ai">CookManus</span>
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">新用户默认角色：厨师</p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <div className="space-y-2">
            <Label htmlFor="username">用户名</Label>
            <Input
              id="username"
              autoComplete="username"
              placeholder="3-32 个字符"
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
              autoComplete="new-password"
              placeholder="6-64 个字符"
              aria-invalid={!!errors.password}
              {...register("password")}
            />
            {errors.password && (
              <p className="text-xs text-destructive">{errors.password.message}</p>
            )}
          </div>
          <div className="space-y-2">
            <Label htmlFor="phone">手机号（可选）</Label>
            <Input
              id="phone"
              autoComplete="tel"
              placeholder="11 位手机号"
              aria-invalid={!!errors.phone}
              {...register("phone")}
            />
            {errors.phone && (
              <p className="text-xs text-destructive">{errors.phone.message}</p>
            )}
          </div>
          <Button type="submit" className="w-full cursor-pointer glow-ai" disabled={isSubmitting}>
            {isSubmitting && <Loader2 className="size-4 animate-spin" />}
            注册
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-muted-foreground">
          已有账号？{" "}
          <Link
            href="/login"
            className="font-medium text-primary hover:underline cursor-pointer"
          >
            去登录
          </Link>
        </p>
      </div>
    </main>
  );
}
