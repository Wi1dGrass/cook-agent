import { SidebarProvider, SidebarInset } from "@/components/ui/sidebar";
import { TooltipProvider } from "@/components/ui/tooltip";
import { AppSidebar } from "@/components/layout/app-sidebar";
import { AppHeader } from "@/components/layout/app-header";
import { AuthHydrator } from "@/components/auth-hydrator";

export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <SidebarProvider defaultOpen>
      <TooltipProvider delayDuration={200}>
        <AuthHydrator />
        <AppSidebar />
        <SidebarInset>
          <AppHeader />
          <div className="flex-1">{children}</div>
        </SidebarInset>
      </TooltipProvider>
    </SidebarProvider>
  );
}
