"use client";

import { create } from "zustand";
import type { AuthUser, UserRole } from "@/lib/api/types";

interface AuthState {
  user: AuthUser | null;
  hydrated: boolean;
  setUser: (user: AuthUser | null) => void;
  logout: () => void;
  hasRole: (...roles: UserRole[]) => boolean;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  hydrated: false,
  setUser: (user) => set({ user, hydrated: true }),
  logout: () => {
    set({ user: null, hydrated: true });
  },
  hasRole: (...roles) => {
    const u = get().user;
    return !!u && roles.includes(u.role);
  },
}));

export const ROLE_RANK: Record<UserRole, number> = {
  CHEF: 1,
  MANAGER: 2,
  ADMIN: 3,
};

export function hasMinRole(user: AuthUser | null, min: UserRole): boolean {
  if (!user) return false;
  return ROLE_RANK[user.role] >= ROLE_RANK[min];
}
