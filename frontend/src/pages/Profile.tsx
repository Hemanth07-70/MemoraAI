import { Mail, Calendar, Shield } from "lucide-react";
import { PageWrapper } from "../components/layout/PageWrapper";

export function Profile() {
  return (
    <PageWrapper className="max-w-3xl mx-auto space-y-6">
      <h1 className="text-2xl font-bold tracking-tight">Profile</h1>
      
      <div className="bg-white dark:bg-zinc-950 rounded-2xl border border-zinc-200 dark:border-zinc-800 shadow-sm overflow-hidden">
        <div className="h-32 bg-gradient-to-r from-indigo-500 to-purple-600"></div>
        <div className="px-8 pb-8 relative">
          <div className="w-24 h-24 bg-white dark:bg-zinc-900 rounded-full border-4 border-white dark:border-zinc-950 absolute -top-12 flex items-center justify-center text-4xl font-bold text-indigo-600 dark:text-indigo-400 shadow-md">
            HC
          </div>
          
          <div className="mt-16">
            <h2 className="text-2xl font-bold">Hemanth Chowdary</h2>
            <p className="text-zinc-500">Student</p>
          </div>

          <div className="mt-8 space-y-6">
            <div className="flex items-center gap-4 py-3 border-b border-zinc-100 dark:border-zinc-800/50">
              <div className="w-10 h-10 rounded-full bg-zinc-100 dark:bg-zinc-900 flex items-center justify-center text-zinc-500">
                <Mail className="w-5 h-5" />
              </div>
              <div>
                <p className="text-sm text-zinc-500">Email Address</p>
                <p className="font-medium">test@memora.ai</p>
              </div>
            </div>

            <div className="flex items-center gap-4 py-3 border-b border-zinc-100 dark:border-zinc-800/50">
              <div className="w-10 h-10 rounded-full bg-zinc-100 dark:bg-zinc-900 flex items-center justify-center text-zinc-500">
                <Shield className="w-5 h-5" />
              </div>
              <div>
                <p className="text-sm text-zinc-500">Role</p>
                <p className="font-medium">Student</p>
              </div>
            </div>

            <div className="flex items-center gap-4 py-3">
              <div className="w-10 h-10 rounded-full bg-zinc-100 dark:bg-zinc-900 flex items-center justify-center text-zinc-500">
                <Calendar className="w-5 h-5" />
              </div>
              <div>
                <p className="text-sm text-zinc-500">Joined Date</p>
                <p className="font-medium">July 2026</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </PageWrapper>
  );
}
