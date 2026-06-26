"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Plus, Trash2, Loader2, ChefHat } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { PageHeader } from "@/components/common/states";
import { useAuthStore } from "@/lib/store/auth-store";
import {
  createRecipeClient,
  updateRecipeClient,
  listCategoriesClient,
} from "@/lib/api/recipes-client";
import { ApiError, friendlyMessage } from "@/lib/api/errors";
import type { CategoryResponse, RecipeDetail } from "@/lib/api/types";

const ingredientSchema = z.object({
  name: z.string().min(1, "必填"),
  brand: z.string().optional(),
  quantity: z.string().optional(),
  note: z.string().optional(),
});

const stepSchema = z.object({
  description: z.string().min(1, "必填"),
});

const formSchema = z.object({
  name: z.string().min(1, "请输入菜名"),
  categoryId: z.coerce.number({ message: "请选择分类" }).int().positive(),
  alias: z.string().optional(),
  imageUrl: z.string().optional(),
  summary: z.string().optional(),
  remark: z.string().optional(),
  nutritionJson: z.string().optional(),
  ingredients: z.array(ingredientSchema).min(1, "至少添加 1 个配料"),
  steps: z.array(stepSchema).min(1, "至少添加 1 个步骤"),
});

type FormValues = z.infer<typeof formSchema>;

export function RecipeForm({ initial }: { initial?: RecipeDetail }) {
  const router = useRouter();
  const user = useAuthStore((s) => s.user);
  const [categories, setCategories] = React.useState<CategoryResponse[]>([]);
  const [submitting, setSubmitting] = React.useState(false);

  React.useEffect(() => {
    listCategoriesClient().then(setCategories).catch(() => {});
  }, []);

  const isEdit = !!initial;

  const defaultValues: FormValues = React.useMemo(
    () =>
      initial
        ? {
            name: initial.name,
            categoryId: initial.categoryId,
            alias: initial.alias ?? "",
            imageUrl: initial.imageUrl ?? "",
            summary: initial.summary ?? "",
            remark: initial.remark ?? "",
            nutritionJson: initial.nutritionJson ?? "",
            ingredients: initial.ingredients.map((i) => ({
              name: i.name,
              brand: i.brand ?? "",
              quantity: i.quantity ?? "",
              note: i.note ?? "",
            })),
            steps: initial.steps.map((s) => ({ description: s.description })),
          }
        : {
            name: "",
            categoryId: 0,
            alias: "",
            imageUrl: "",
            summary: "",
            remark: "",
            nutritionJson: "",
            ingredients: [{ name: "", brand: "", quantity: "", note: "" }],
            steps: [{ description: "" }],
          },
    [initial]
  );

  const {
    register,
    handleSubmit,
    control,
    setValue,
    watch,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues,
  });

  const ingredients = useFieldArray({ control, name: "ingredients" });
  const steps = useFieldArray({ control, name: "steps" });
  const categoryId = watch("categoryId");

  async function onSubmit(values: FormValues) {
    setSubmitting(true);
    try {
      const body = {
        ...values,
        ingredients: values.ingredients.map((ing, i) => ({
          ...ing,
          sortOrder: i,
        })),
        steps: values.steps.map((s, i) => ({ stepOrder: i + 1, description: s.description })),
      };
      if (isEdit && initial) {
        await updateRecipeClient(initial.id, body);
        toast.success("菜谱已更新");
        router.push(`/recipes/${initial.id}`);
      } else {
        const created = await createRecipeClient(body);
        toast.success("菜谱已创建");
        router.push(`/recipes/${created.id}`);
      }
      router.refresh();
    } catch (e) {
      toast.error(e instanceof ApiError ? friendlyMessage(e) : "保存失败");
    } finally {
      setSubmitting(false);
    }
  }

  if (!user) return null;

  return (
    <main className="mx-auto w-full max-w-3xl px-4 py-6">
      <PageHeader
        title={isEdit ? "编辑菜谱" : "新建菜谱"}
        description={isEdit ? `修改：${initial?.name}` : "填写菜谱信息、配料与步骤"}
        icon={ChefHat}
      />

      <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-6" noValidate>
        <Card>
          <CardHeader>
            <CardTitle className="text-base">基本信息</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="name">菜名 *</Label>
                <Input id="name" {...register("name")} aria-invalid={!!errors.name} />
                {errors.name && <p className="text-xs text-destructive">{errors.name.message}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="categoryId">分类 *</Label>
                <Select
                  value={categoryId ? String(categoryId) : ""}
                  onValueChange={(v) => setValue("categoryId", Number(v), { shouldValidate: true })}
                >
                  <SelectTrigger id="categoryId" aria-invalid={!!errors.categoryId}>
                    <SelectValue placeholder="选择分类" />
                  </SelectTrigger>
                  <SelectContent>
                    {categories.map((c) => (
                      <SelectItem key={c.id} value={String(c.id)}>
                        {c.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {errors.categoryId && (
                  <p className="text-xs text-destructive">{errors.categoryId.message}</p>
                )}
              </div>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="alias">别名</Label>
                <Input id="alias" {...register("alias")} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="imageUrl">图片 URL</Label>
                <Input id="imageUrl" {...register("imageUrl")} placeholder="https://…" />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="summary">简介</Label>
              <Textarea id="summary" rows={2} {...register("summary")} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="remark">备注</Label>
              <Textarea id="remark" rows={2} {...register("remark")} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="nutritionJson">营养数据（JSON，可选）</Label>
              <Textarea
                id="nutritionJson"
                rows={3}
                {...register("nutritionJson")}
                placeholder='{"热量":"59 Kcal","蛋白质":"3.0 g"}'
                className="font-mono text-xs"
              />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex-row items-center justify-between space-y-0">
            <CardTitle className="text-base">配料</CardTitle>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="cursor-pointer"
              onClick={() => ingredients.append({ name: "", brand: "", quantity: "", note: "" })}
            >
              <Plus className="size-4" />
              添加
            </Button>
          </CardHeader>
          <CardContent className="space-y-3">
            {ingredients.fields.map((field, i) => (
              <div key={field.id} className="flex flex-wrap items-end gap-2">
                <div className="w-32 space-y-1.5">
                  <Label className="text-xs">名称</Label>
                  <Input {...register(`ingredients.${i}.name`)} />
                </div>
                <div className="w-28 space-y-1.5">
                  <Label className="text-xs">品牌</Label>
                  <Input {...register(`ingredients.${i}.brand`)} />
                </div>
                <div className="w-24 space-y-1.5">
                  <Label className="text-xs">用量</Label>
                  <Input {...register(`ingredients.${i}.quantity`)} />
                </div>
                <div className="flex-1 min-w-32 space-y-1.5">
                  <Label className="text-xs">备注</Label>
                  <Input {...register(`ingredients.${i}.note`)} />
                </div>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="cursor-pointer text-muted-foreground"
                  onClick={() => ingredients.remove(i)}
                  disabled={ingredients.fields.length === 1}
                  aria-label="删除配料"
                >
                  <Trash2 className="size-4" />
                </Button>
              </div>
            ))}
            {errors.ingredients?.message && (
              <p className="text-xs text-destructive">{errors.ingredients.message}</p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex-row items-center justify-between space-y-0">
            <CardTitle className="text-base">步骤</CardTitle>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="cursor-pointer"
              onClick={() => steps.append({ description: "" })}
            >
              <Plus className="size-4" />
              添加
            </Button>
          </CardHeader>
          <CardContent className="space-y-3">
            {steps.fields.map((field, i) => (
              <div key={field.id} className="flex items-start gap-2">
                <span className="mt-2.5 flex size-6 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-medium text-primary">
                  {i + 1}
                </span>
                <Textarea
                  rows={2}
                  {...register(`steps.${i}.description`)}
                  placeholder={`第 ${i + 1} 步描述`}
                  className="flex-1"
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="cursor-pointer text-muted-foreground"
                  onClick={() => steps.remove(i)}
                  disabled={steps.fields.length === 1}
                  aria-label="删除步骤"
                >
                  <Trash2 className="size-4" />
                </Button>
              </div>
            ))}
            {errors.steps?.message && (
              <p className="text-xs text-destructive">{errors.steps.message}</p>
            )}
          </CardContent>
        </Card>

        <Separator />
        <div className="flex justify-end gap-2">
          <Button
            type="button"
            variant="outline"
            className="cursor-pointer"
            onClick={() => router.back()}
          >
            取消
          </Button>
          <Button type="submit" className="cursor-pointer" disabled={submitting}>
            {submitting && <Loader2 className="size-4 animate-spin" />}
            {isEdit ? "保存修改" : "创建菜谱"}
          </Button>
        </div>
      </form>
    </main>
  );
}
