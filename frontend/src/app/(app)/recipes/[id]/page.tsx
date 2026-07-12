import * as React from "react";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowLeft, UtensilsCrossed, ListOrdered, Apple, FileText } from "lucide-react";

import { getRecipeDetail, listCategories } from "@/lib/api/recipes";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Markdown } from "@/components/common/markdown";
import { FavoriteButton } from "@/components/recipe/favorite-button";
import { parseNutrition, resolveImageUrl } from "@/lib/utils/format";
import { cn } from "@/lib/utils";
import { ApiError } from "@/lib/api/errors";

export const dynamic = "force-dynamic";

interface PageProps {
  params: Promise<{ id: string }>;
}

export default async function RecipeDetailPage({ params }: PageProps) {
  const { id } = await params;
  const numId = Number(id);
  if (!numId) notFound();

  let detail;
  try {
    detail = await getRecipeDetail(numId);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  }

  const categories = await listCategories().catch(() => []);
  const category = categories.find((c) => c.id === detail.categoryId);
  const nutrition = parseNutrition(detail.nutritionJson);

  return (
    <main className="mx-auto w-full max-w-4xl px-4 py-6">
      <Link
        href="/recipes"
        className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors cursor-pointer"
      >
        <ArrowLeft className="size-4" />
        返回菜谱列表
      </Link>

      {detail.imageUrl && (
        <div className="mt-4 overflow-hidden rounded-2xl border border-border/60 shadow-sm">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={resolveImageUrl(detail.imageUrl) ?? ""}
            alt={detail.name}
            className="mx-auto h-full max-h-72 w-full object-cover"
          />
        </div>
      )}

      <div className={cn("flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between", detail.imageUrl ? "mt-6" : "mt-4")}>
        <div className="flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-semibold tracking-tight">{detail.name}</h1>
            {category && (
              <Badge variant="secondary" className="cursor-default">
                {category.name}
              </Badge>
            )}
          </div>
          {detail.alias && (
            <p className="mt-1 text-sm text-muted-foreground">别名：{detail.alias}</p>
          )}
          {detail.summary && (
            <p className="mt-2 text-sm text-muted-foreground">{detail.summary}</p>
          )}
        </div>
        <FavoriteButton recipeId={detail.id} initialFavorited={!!detail.favorited} />
      </div>

      <Tabs defaultValue="detail" className="mt-6">
        <TabsList>
          <TabsTrigger value="detail" className="cursor-pointer">
            <UtensilsCrossed className="size-4" />
            详情
          </TabsTrigger>
          <TabsTrigger value="nutrition" className="cursor-pointer">
            <Apple className="size-4" />
            营养
          </TabsTrigger>
          <TabsTrigger value="raw" className="cursor-pointer">
            <FileText className="size-4" />
            原始 Markdown
          </TabsTrigger>
        </TabsList>

        <TabsContent value="detail" className="mt-4 space-y-4">
          <div className="grid gap-4 md:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base">
                  <UtensilsCrossed className="size-4 text-primary" />
                  配料
                </CardTitle>
              </CardHeader>
              <CardContent>
                {detail.ingredients.length === 0 ? (
                  <p className="text-sm text-muted-foreground">暂无配料信息</p>
                ) : (
                  <ul className="space-y-1.5">
                    {detail.ingredients.map((ing) => (
                      <li key={ing.id} className="flex items-baseline justify-between gap-2 text-sm">
                        <span>
                          {ing.name}
                          {ing.brand && (
                            <span className="text-muted-foreground">（{ing.brand}）</span>
                          )}
                        </span>
                        {ing.quantity && (
                          <span className="text-muted-foreground">{ing.quantity}</span>
                        )}
                      </li>
                    ))}
                  </ul>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base">
                  <ListOrdered className="size-4 text-primary" />
                  步骤
                </CardTitle>
              </CardHeader>
              <CardContent>
                {detail.steps.length === 0 ? (
                  <p className="text-sm text-muted-foreground">暂无步骤信息</p>
                ) : (
                  <ol className="space-y-3">
                    {detail.steps.map((step) => (
                      <li key={step.stepOrder} className="flex gap-3 text-sm">
                        <span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-medium text-primary">
                          {step.stepOrder}
                        </span>
                        <span className="leading-relaxed">{step.description}</span>
                      </li>
                    ))}
                  </ol>
                )}
              </CardContent>
            </Card>
          </div>

          {detail.remark && (
            <Card>
              <CardHeader>
                <CardTitle className="text-base">备注</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-muted-foreground">{detail.remark}</p>
              </CardContent>
            </Card>
          )}
        </TabsContent>

        <TabsContent value="nutrition" className="mt-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-base">
                <Apple className="size-4 text-primary" />
                营养成分（每 100g）
              </CardTitle>
            </CardHeader>
            <CardContent>
              {nutrition ? (
                <div className="overflow-hidden rounded-lg border border-border">
                  <table className="w-full text-sm">
                    <tbody>
                      {Object.entries(nutrition).map(([k, v]) => (
                        <tr key={k} className="border-b border-border last:border-0">
                          <td className="bg-muted/50 px-4 py-2 font-medium">{k}</td>
                          <td className="px-4 py-2 text-muted-foreground">{v}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="text-sm text-muted-foreground">暂无营养数据</p>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="raw" className="mt-4">
          <Card>
            <CardContent className="pt-6">
              {detail.rawMarkdown ? (
                <Markdown>{detail.rawMarkdown}</Markdown>
              ) : (
                <p className="text-sm text-muted-foreground">暂无原始 Markdown</p>
              )}
              {detail.sourceFile && (
                <>
                  <Separator className="my-4" />
                  <p className="font-mono text-xs text-muted-foreground">
                    来源文件：{detail.sourceFile}
                  </p>
                </>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </main>
  );
}
