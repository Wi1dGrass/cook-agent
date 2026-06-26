"use client";

import Link from "next/link";
import { LayoutDashboard, Database, Users, ArrowRight } from "lucide-react";
import { Card } from "@/components/ui/card";
import { PageHeader } from "@/components/common/states";
import { useAuthStore } from "@/lib/store/auth-store";
import { ROLE_LABEL } from "@/lib/nav";

export default function AdminPage() {
  const user = useAuthStore((s) => s.user);

  return (
    <main className="mx-auto w-full max-w-4xl px-4 py-6">
      <PageHeader
        title="管理控制台"
        description={`欢迎，${user?.username ?? "管理员"}（${user ? ROLE_LABEL[user.role] : ""}）`}
        icon={LayoutDashboard}
      />

      <div className="mt-6 grid gap-4 sm:grid-cols-2">
        <Link href="/admin/etl" className="block cursor-pointer">
          <Card className="group flex items-center justify-between p-5 transition-colors hover:border-primary/40">
            <div className="flex items-center gap-3">
              <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <Database className="size-5" />
              </div>
              <div>
                <h3 className="font-medium">ETL 同步</h3>
                <p className="text-sm text-muted-foreground">同步知识库 / 重建向量索引</p>
              </div>
            </div>
            <ArrowRight className="size-4 text-muted-foreground transition-transform group-hover:translate-x-0.5" />
          </Card>
        </Link>

        <Link href="/admin/users" className="block cursor-pointer">
          <Card className="group flex items-center justify-between p-5 transition-colors hover:border-primary/40">
            <div className="flex items-center gap-3">
              <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
                <Users className="size-5" />
              </div>
              <div>
                <h3 className="font-medium">用户管理</h3>
                <p className="text-sm text-muted-foreground">启用/禁用、角色、重置密码</p>
              </div>
            </div>
            <ArrowRight className="size-4 text-muted-foreground transition-transform group-hover:translate-x-0.5" />
          </Card>
        </Link>
      </div>
    </main>
  );
}
