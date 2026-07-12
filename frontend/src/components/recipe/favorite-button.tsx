"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { Heart, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/lib/store/auth-store";
import { addFavorite, removeFavorite } from "@/lib/api/favorites";
import { ApiError, friendlyMessage } from "@/lib/api/errors";

export function FavoriteButton({
  recipeId,
  initialFavorited = false,
}: {
  recipeId: number;
  initialFavorited?: boolean;
}) {
  const user = useAuthStore((s) => s.user);
  const router = useRouter();
  const [favorited, setFavorited] = React.useState(initialFavorited);
  const [loading, setLoading] = React.useState(false);

  async function toggle() {
    if (!user) {
      toast.info("请先登录后再收藏");
      router.push("/login");
      return;
    }
    setLoading(true);
    try {
      if (favorited) {
        await removeFavorite(recipeId);
        setFavorited(false);
        toast.success("已取消收藏");
      } else {
        await addFavorite(recipeId);
        setFavorited(true);
        toast.success("已加入收藏");
      }
    } catch (e) {
      toast.error(e instanceof ApiError ? friendlyMessage(e) : "操作失败");
    } finally {
      setLoading(false);
    }
  }

  return (
    <Button
      variant={favorited ? "default" : "outline"}
      size="sm"
      className="cursor-pointer"
      onClick={toggle}
      disabled={loading}
      aria-pressed={favorited}
    >
      {loading ? (
        <Loader2 className="size-4 animate-spin" />
      ) : (
        <Heart className={cn("size-4", favorited && "fill-current")} />
      )}
      {favorited ? "已收藏" : "收藏"}
    </Button>
  );
}
