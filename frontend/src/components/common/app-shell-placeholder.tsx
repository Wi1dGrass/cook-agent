export function AppShellPlaceholder({
  title,
  hint,
}: {
  title: string;
  hint?: string;
}) {
  return (
    <main className="mx-auto flex min-h-[calc(100svh-3.5rem)] max-w-5xl flex-col items-center justify-center gap-3 px-6 py-16 text-center">
      <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
      {hint && <p className="text-sm text-muted-foreground">{hint}</p>}
      <div className="mt-4 size-10 rounded-full border-2 border-primary/30 border-t-primary animate-spin" />
    </main>
  );
}
