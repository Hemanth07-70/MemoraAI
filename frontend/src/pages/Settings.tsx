import { Moon, Sun, Bell, LogOut, Globe } from "lucide-react";
import { PageWrapper } from "../components/layout/PageWrapper";

export function Settings() {
  const toggleTheme = (theme: 'light' | 'dark') => {
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  };

  const handleLogout = () => {
    localStorage.removeItem("token");
    window.location.reload();
  };

  return (
    <PageWrapper className="max-w-3xl mx-auto space-y-6">
      <h1 className="text-2xl font-bold tracking-tight">Settings</h1>
      
      <div className="space-y-6">
        {/* Appearance */}
        <section className="glass-card p-6 rounded-2xl">
          <h2 className="text-lg font-bold mb-4">Appearance</h2>
          <div className="flex gap-4">
            <button 
              onClick={() => toggleTheme('light')}
              className="flex-1 flex flex-col items-center justify-center gap-3 p-4 rounded-xl border-2 border-transparent bg-zinc-50 dark:bg-zinc-900 hover:border-indigo-500 transition-colors"
            >
              <Sun className="w-6 h-6 text-amber-500" />
              <span className="font-medium">Light Theme</span>
            </button>
            <button 
              onClick={() => toggleTheme('dark')}
              className="flex-1 flex flex-col items-center justify-center gap-3 p-4 rounded-xl border-2 border-transparent bg-zinc-50 dark:bg-zinc-900 hover:border-indigo-500 transition-colors"
            >
              <Moon className="w-6 h-6 text-indigo-400" />
              <span className="font-medium">Dark Theme</span>
            </button>
          </div>
        </section>

        {/* Preferences */}
        <section className="glass-card p-6 rounded-2xl space-y-4">
          <h2 className="text-lg font-bold mb-2">Preferences</h2>
          
          <div className="flex items-center justify-between py-3 border-b border-zinc-100 dark:border-zinc-800/50">
            <div className="flex items-center gap-4">
              <div className="w-10 h-10 rounded-full bg-zinc-100 dark:bg-zinc-900 flex items-center justify-center text-zinc-500">
                <Globe className="w-5 h-5" />
              </div>
              <div>
                <p className="font-medium">Language</p>
                <p className="text-sm text-zinc-500">English (US)</p>
              </div>
            </div>
            <button className="text-sm font-medium text-indigo-600 dark:text-indigo-400 hover:underline">Change</button>
          </div>

          <div className="flex items-center justify-between py-3">
            <div className="flex items-center gap-4">
              <div className="w-10 h-10 rounded-full bg-zinc-100 dark:bg-zinc-900 flex items-center justify-center text-zinc-500">
                <Bell className="w-5 h-5" />
              </div>
              <div>
                <p className="font-medium">Notifications</p>
                <p className="text-sm text-zinc-500">Enabled for study reminders</p>
              </div>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input type="checkbox" className="sr-only peer" defaultChecked />
              <div className="w-11 h-6 bg-zinc-200 peer-focus:outline-none rounded-full peer dark:bg-zinc-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-zinc-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-zinc-600 peer-checked:bg-indigo-600"></div>
            </label>
          </div>
        </section>

        {/* Account Actions */}
        <section className="glass-card p-6 rounded-2xl border-rose-200/50 dark:border-rose-900/30">
          <h2 className="text-lg font-bold text-rose-600 dark:text-rose-400 mb-4">Account</h2>
          <button 
            onClick={handleLogout}
            className="flex items-center gap-2 text-rose-600 dark:text-rose-400 font-medium hover:underline"
          >
            <LogOut className="w-5 h-5" /> Sign out of MemoraAI
          </button>
        </section>
      </div>
    </PageWrapper>
  );
}
