import { Bell, Search, Sun, Moon } from "lucide-react";

export function Topbar() {
  const toggleTheme = () => {
    document.documentElement.classList.toggle('dark');
  };

  return (
    <header className="h-16 glass border-b flex items-center justify-between px-6 sticky top-0 z-10 transition-colors duration-500">
      <div className="flex-1 max-w-xl flex items-center gap-2 px-3 py-1.5 bg-slate-100/60 dark:bg-zinc-900 rounded-md border border-slate-200/50 dark:border-zinc-800">
        <Search className="w-4 h-4 text-zinc-500" />
        <input 
          type="text" 
          placeholder="Search documents, concepts, or quizzes..." 
          className="bg-transparent border-none outline-none w-full text-sm text-zinc-900 dark:text-zinc-100 placeholder:text-zinc-500"
        />
      </div>

      <div className="flex items-center gap-4 pl-6">
        <button onClick={toggleTheme} className="text-zinc-500 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100 transition-colors">
          <Sun className="w-5 h-5 hidden dark:block" />
          <Moon className="w-5 h-5 block dark:hidden" />
        </button>
        <button className="text-zinc-500 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100 transition-colors relative">
          <Bell className="w-5 h-5" />
          <span className="absolute top-0 right-0 w-2 h-2 bg-indigo-500 rounded-full border-2 border-white dark:border-zinc-950"></span>
        </button>
        <div className="w-8 h-8 rounded-full bg-indigo-100 text-indigo-700 dark:bg-indigo-900 dark:text-indigo-300 flex items-center justify-center font-bold text-sm">
          HC
        </div>
      </div>
    </header>
  );
}
