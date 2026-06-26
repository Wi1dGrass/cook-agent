import { clientApiFetch } from "./client-fetch";
import type { UserResponse, PageResult, UserRole } from "./types";

export function listUsers(params: {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
}) {
  return clientApiFetch<PageResult<UserResponse>>("/admin/users", { query: params });
}

export function getUser(id: number) {
  return clientApiFetch<UserResponse>(`/admin/users/${id}`);
}

export function setUserEnabled(id: number, enabled: number) {
  return clientApiFetch<{ id: number; enabled: number }>(
    `/admin/users/${id}/enabled`,
    { method: "PUT", query: { enabled } }
  );
}

export function setUserRole(id: number, role: UserRole) {
  return clientApiFetch<{ id: number; role: string }>(`/admin/users/${id}/role`, {
    method: "PUT",
    query: { role },
  });
}

export function resetUserPassword(id: number, newPassword: string) {
  return clientApiFetch<{ id: number; updated: boolean }>(
    `/admin/users/${id}/password`,
    { method: "PUT", query: { newPassword } }
  );
}

export function etlSync() {
  return clientApiFetch<{ message: string }>("/admin/etl/sync", { method: "POST" });
}

export function etlRebuildIndex() {
  return clientApiFetch<{ message: string }>("/admin/etl/rebuild-index", {
    method: "POST",
  });
}
