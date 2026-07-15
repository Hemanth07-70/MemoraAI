import { Outlet, useLocation } from "react-router-dom";
import { Sidebar } from "./Sidebar";
import { Topbar } from "./Topbar";
import { AnimatePresence } from "framer-motion";

export function Layout() {
  const location = useLocation();
  return (
    <div className="flex min-h-screen bg-slate-50 bg-[radial-gradient(ellipse_at_top_right,_var(--tw-gradient-stops))] from-indigo-100/40 via-slate-50 to-slate-50 dark:bg-none dark:bg-zinc-950 text-slate-900 dark:text-zinc-100 font-sans transition-colors duration-500">
      <Sidebar />
      <div className="flex-1 flex flex-col h-screen overflow-hidden">
        <Topbar />
        <main className="flex-1 overflow-y-auto overflow-x-hidden p-6 md:p-10 relative custom-scrollbar">
          <AnimatePresence mode="wait">
            <div key={location.pathname} className="w-full h-full">
              <Outlet />
            </div>
          </AnimatePresence>
        </main>
      </div>
    </div>
  );
}
