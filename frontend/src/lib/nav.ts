import {
  MessageSquare,
  Bot,
  UtensilsCrossed,
  Search,
  GitCompareArrows,
  Carrot,
  Apple,
  CalendarDays,
  FolderTree,
  Heart,
  History,
  ShieldCheck,
  ChefHat,
  LayoutDashboard,
  Database,
  Users,
  UserCircle,
  type LucideIcon,
} from "lucide-react";
import type { UserRole } from "@/lib/api/types";

export interface NavItem {
  title: string;
  href: string;
  icon: LucideIcon;
  minRole?: UserRole;
  group: string;
}

export const NAV_ITEMS: NavItem[] = [
  { title: "AI 对话", href: "/chat", icon: MessageSquare, group: "AI" },
  { title: "Agent 模式", href: "/agent", icon: Bot, group: "AI" },
  { title: "菜谱浏览", href: "/recipes", icon: UtensilsCrossed, group: "菜谱" },
  { title: "语义搜索", href: "/recipes/search", icon: Search, group: "菜谱" },
  { title: "智能推荐", href: "/recipes/search?tab=recommend", icon: Search, group: "菜谱" },
  { title: "菜谱对比", href: "/recipes/compare", icon: GitCompareArrows, group: "菜谱" },
  { title: "食材反查", href: "/recipes/ingredients", icon: Carrot, group: "菜谱" },
  { title: "营养查询", href: "/recipes/nutrition", icon: Apple, group: "菜谱" },
  { title: "每日推荐", href: "/recipes/daily", icon: CalendarDays, group: "菜谱" },
  { title: "菜谱分类", href: "/categories", icon: FolderTree, group: "菜谱" },
  { title: "新建菜谱", href: "/recipes/new", icon: ChefHat, minRole: "CHEF", group: "我的" },
  { title: "个人中心", href: "/user/profile", icon: UserCircle, minRole: "CHEF", group: "我的" },
  { title: "我的收藏", href: "/favorites", icon: Heart, minRole: "CHEF", group: "我的" },
  { title: "对话历史", href: "/history", icon: History, minRole: "CHEF", group: "我的" },
  { title: "控制台", href: "/admin", icon: LayoutDashboard, minRole: "ADMIN", group: "管理" },
  { title: "ETL 同步", href: "/admin/etl", icon: Database, minRole: "ADMIN", group: "管理" },
  { title: "用户管理", href: "/admin/users", icon: Users, minRole: "ADMIN", group: "管理" },
];

export const GROUP_ORDER = ["AI", "菜谱", "我的", "管理"];

export const ROLE_LABEL: Record<UserRole, string> = {
  CHEF: "厨师",
  MANAGER: "经理",
  ADMIN: "管理员",
};

export { ShieldCheck };
