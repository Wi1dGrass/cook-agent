import { notFound } from "next/navigation";
import { getRecipeDetail } from "@/lib/api/recipes";
import { RecipeForm } from "@/components/recipe/recipe-form";
import { ApiError } from "@/lib/api/errors";

export const dynamic = "force-dynamic";

interface PageProps {
  params: Promise<{ id: string }>;
}

export default async function EditRecipePage({ params }: PageProps) {
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

  return <RecipeForm initial={detail} />;
}
