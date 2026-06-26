"use client";

import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { cn } from "@/lib/utils";

export function Markdown({
  children,
  className,
}: {
  children: string;
  className?: string;
}) {
  return (
    <div
      className={cn(
        "prose-chat max-w-none text-sm leading-relaxed [&_*]:my-0",
        "[&_p]:my-2 [&_ul]:my-2 [&_ol]:my-2 [&_li]:my-0.5 [&_li]:ml-5",
        "[&_ul]:list-disc [&_ol]:list-decimal",
        "[_h1]:mt-3 [_h1]:mb-2 [_h1]:text-lg [_h1]:font-semibold",
        "[_h2]:mt-3 [_h2]:mb-2 [_h2]:text-base [_h2]:font-semibold",
        "[_h3]:mt-2 [_h3]:mb-1 [_h3]:text-sm [_h3]:font-semibold",
        "[&_strong]:font-semibold",
        "[&_a]:text-primary [&_a]:underline",
        "[&_code]:rounded [&_code]:bg-muted [&_code]:px-1 [&_code]:py-0.5 [&_code]:text-xs [&_code]:font-mono",
        "[&_pre]:my-3 [&_pre]:overflow-x-auto [&_pre]:rounded-lg [&_pre]:bg-muted [&_pre]:p-3",
        "[&_table]:my-3 [&_table]:w-full [&_table]:border-collapse [&_table]:text-xs",
        "[&_th]:border [&_th]:border-border [&_th]:px-2 [&_th]:py-1 [&_th]:bg-muted [&_th]:text-left [&_th]:font-medium",
        "[&_td]:border [&_td]:border-border [&_td]:px-2 [&_td]:py-1",
        "[&_blockquote]:my-2 [&_blockquote]:border-l-2 [&_blockquote]:border-primary/40 [&_blockquote]:pl-3 [&_blockquote]:text-muted-foreground",
        className
      )}
    >
      <ReactMarkdown remarkPlugins={[remarkGfm]}>{children}</ReactMarkdown>
    </div>
  );
}
