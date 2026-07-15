import { NavLink } from "react-router-dom";
import { 
  LayoutDashboard, 
  FileText, 
  MessageSquare, 
  Share2, 
  BrainCircuit, 
  CalendarDays, 
  CheckCircle, 
  User, 
  Settings 
} from "lucide-react";
import { cn } from "../../utils/cn";

const navItems = [
  { name: "Dashboard", href: "/", icon: LayoutDashboard },
  { name: "Documents", href: "/documents", icon: FileText },
  { name: "AI Chat", href: "/chat", icon: MessageSquare },
  { name: "Knowledge Graph", href: "/graph", icon: Share2 },
  { name: "Memory", href: "/memory", icon: BrainCircuit },
  { name: "Revision", href: "/revision", icon: CalendarDays },
  { name: "Quiz Center", href: "/quiz", icon: CheckCircle },
];

const bottomItems = [
  { name: "Profile", href: "/profile", icon: User },
  { name: "Settings", href: "/settings", icon: Settings },
];

export function Sidebar() {
  return (
    <aside className="w-64 glass border-r flex flex-col h-screen sticky top-0 transition-colors duration-500">
      <div className="h-16 flex items-center px-6 border-b border-slate-200/50 dark:border-zinc-800">
        <div className="flex items-center gap-2 text-indigo-600 dark:text-indigo-400 font-bold text-xl tracking-tight">
          <BrainCircuit className="w-6 h-6" />
          MemoraAI
        </div>
      </div>
      
      <div className="flex-1 py-6 px-4 space-y-1 overflow-y-auto">
        {navItems.map((item) => (
          <NavLink
            key={item.name}
            to={item.href}
            className={({ isActive }) =>
              cn(
                "flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors",
                isActive 
                  ? "bg-indigo-50/80 text-indigo-700 shadow-sm dark:bg-indigo-500/10 dark:text-indigo-400 dark:shadow-none" 
                  : "text-slate-600 hover:bg-slate-100/50 hover:text-slate-900 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-50"
              )
            }
          >
            <item.icon className="w-5 h-5" />
            {item.name}
          </NavLink>
        ))}
      </div>

      <div className="p-4 space-y-1 border-t border-slate-200/50 dark:border-zinc-800">
        {bottomItems.map((item) => (
          <NavLink
            key={item.name}
            to={item.href}
            className={({ isActive }) =>
              cn(
                "flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors",
                isActive 
                  ? "bg-indigo-50/80 text-indigo-700 shadow-sm dark:bg-indigo-500/10 dark:text-indigo-400 dark:shadow-none" 
                  : "text-slate-600 hover:bg-slate-100/50 hover:text-slate-900 dark:text-zinc-400 dark:hover:bg-zinc-800 dark:hover:text-zinc-50"
              )
            }
          >
            <item.icon className="w-5 h-5" />
            {item.name}
          </NavLink>
        ))}
      </div>
    </aside>
  );
}
