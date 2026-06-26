"use client";

import * as React from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Users, Loader2, Search } from "lucide-react";
import { toast } from "sonner";

import {
  listUsers,
  setUserEnabled,
  setUserRole,
  resetUserPassword,
} from "@/lib/api/admin";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  DialogFooter,
  DialogClose,
} from "@/components/ui/dialog";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { PageHeader, EmptyState, ErrorState } from "@/components/common/states";
import { formatDate } from "@/lib/utils/format";
import { friendlyMessage, ApiError } from "@/lib/api/errors";
import { ROLE_LABEL } from "@/lib/nav";
import type { UserRole, UserResponse } from "@/lib/api/types";

const ROLES: UserRole[] = ["CHEF", "MANAGER", "ADMIN"];

export default function AdminUsersPage() {
  const qc = useQueryClient();
  const [keyword, setKeyword] = React.useState("");
  const [submitted, setSubmitted] = React.useState<string | undefined>(undefined);
  const [page, setPage] = React.useState(1);

  const { data, isFetching, error } = useQuery({
    queryKey: ["admin-users", submitted, page],
    queryFn: () => listUsers({ pageNum: page, pageSize: 15, keyword: submitted }),
    retry: false,
  });

  async function toggleEnabled(u: UserResponse, enabled: boolean) {
    try {
      await setUserEnabled(u.id, enabled ? 1 : 0);
      qc.setQueryData<{ records: UserResponse[] }>(["admin-users", submitted, page], (old) => {
        if (!old) return old;
        return {
          ...old,
          records: old.records.map((r) =>
            r.id === u.id ? { ...r, enabled: enabled ? 1 : 0 } : r
          ),
        };
      });
      toast.success(enabled ? "已启用" : "已禁用");
    } catch (e) {
      toast.error(e instanceof ApiError ? friendlyMessage(e) : "操作失败");
    }
  }

  async function changeRole(u: UserResponse, role: UserRole) {
    try {
      await setUserRole(u.id, role);
      qc.setQueryData<{ records: UserResponse[] }>(["admin-users", submitted, page], (old) => {
        if (!old) return old;
        return {
          ...old,
          records: old.records.map((r) =>
            r.id === u.id ? { ...r, role } : r
          ),
        };
      });
      toast.success(`角色已改为 ${ROLE_LABEL[role]}`);
    } catch (e) {
      toast.error(e instanceof ApiError ? friendlyMessage(e) : "操作失败");
    }
  }

  return (
    <main className="mx-auto w-full max-w-5xl px-4 py-6">
      <PageHeader
        title="用户管理"
        description="管理用户状态、角色与密码"
        icon={Users}
      />

      <form
        className="mt-6 flex gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          setPage(1);
          setSubmitted(keyword.trim() || undefined);
        }}
      >
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="按用户名搜索"
            className="pl-9"
            aria-label="搜索用户"
          />
        </div>
        <Button type="submit" className="cursor-pointer" disabled={isFetching}>
          {isFetching ? <Loader2 className="size-4 animate-spin" /> : "搜索"}
        </Button>
      </form>

      <div className="mt-4">
        {isFetching && <div className="h-48 animate-pulse rounded-xl bg-muted" />}
        {error && <ErrorState message={friendlyMessage(error as ApiError)} />}
        {data && !isFetching && (
          data.records.length === 0 ? (
            <EmptyState icon={Users} title="未找到用户" />
          ) : (
            <div className="space-y-4">
              <div className="overflow-x-auto rounded-lg border border-border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>用户名</TableHead>
                      <TableHead>角色</TableHead>
                      <TableHead>状态</TableHead>
                      <TableHead>注册时间</TableHead>
                      <TableHead className="text-right">操作</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {data.records.map((u) => (
                      <TableRow key={u.id}>
                        <TableCell className="font-medium">{u.username}</TableCell>
                        <TableCell>
                          <Select
                            value={u.role}
                            onValueChange={(v) => changeRole(u, v as UserRole)}
                          >
                            <SelectTrigger className="h-8 w-28" aria-label="角色">
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              {ROLES.map((r) => (
                                <SelectItem key={r} value={r}>
                                  {ROLE_LABEL[r]}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center gap-2">
                            <Switch
                              checked={u.enabled === 1}
                              onCheckedChange={(v) => toggleEnabled(u, v)}
                              aria-label="启用状态"
                            />
                            <Badge
                              variant={u.enabled === 1 ? "default" : "secondary"}
                              className="cursor-default"
                            >
                              {u.enabled === 1 ? "启用" : "禁用"}
                            </Badge>
                          </div>
                        </TableCell>
                        <TableCell className="text-xs text-muted-foreground">
                          {formatDate(u.createdAt)}
                        </TableCell>
                        <TableCell className="text-right">
                          <ResetPasswordDialog user={u} />
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>

              {data.pages > 1 && (
                <div className="flex items-center justify-center gap-2 py-2">
                  <Button
                    variant="outline"
                    size="sm"
                    className="cursor-pointer"
                    disabled={page <= 1}
                    onClick={() => setPage((p) => p - 1)}
                  >
                    上一页
                  </Button>
                  <span className="text-sm text-muted-foreground">
                    {page} / {data.pages}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    className="cursor-pointer"
                    disabled={page >= data.pages}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    下一页
                  </Button>
                </div>
              )}
            </div>
          )
        )}
      </div>
    </main>
  );
}

function ResetPasswordDialog({ user }: { user: UserResponse }) {
  const [open, setOpen] = React.useState(false);
  const [pwd, setPwd] = React.useState("");
  const [loading, setLoading] = React.useState(false);

  async function handleReset() {
    if (pwd.length < 6) {
      toast.error("密码至少 6 位");
      return;
    }
    setLoading(true);
    try {
      await resetUserPassword(user.id, pwd);
      toast.success("密码已重置");
      setOpen(false);
      setPwd("");
    } catch (e) {
      toast.error(e instanceof ApiError ? friendlyMessage(e) : "重置失败");
    } finally {
      setLoading(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="ghost" size="sm" className="cursor-pointer">
          重置密码
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>重置 {user.username} 的密码</DialogTitle>
        </DialogHeader>
        <div className="space-y-2 py-2">
          <Input
            type="password"
            value={pwd}
            onChange={(e) => setPwd(e.target.value)}
            placeholder="新密码（6-64 位）"
            aria-label="新密码"
          />
        </div>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline" className="cursor-pointer">
              取消
            </Button>
          </DialogClose>
          <Button onClick={handleReset} className="cursor-pointer" disabled={loading}>
            {loading && <Loader2 className="size-4 animate-spin" />}
            确认重置
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
