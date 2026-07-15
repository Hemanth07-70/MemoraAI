import { useQuery } from "@tanstack/react-query";
import { memoryApi } from "../services/apiClient";
import { motion } from "framer-motion";
import { BrainCircuit, Loader2, Target, Zap, ShieldAlert } from "lucide-react";
import { Link } from "react-router-dom";
import { cn } from "../utils/cn";
import { PageWrapper } from "../components/layout/PageWrapper";

export function Memory() {
  const { data: memoryStates, isLoading } = useQuery({
    queryKey: ["memory-me"],
    queryFn: memoryApi.getForUser,
  });

  const avgScore = memoryStates?.length 
    ? memoryStates.reduce((acc, curr) => acc + curr.memoryScore, 0) / memoryStates.length 
    : 0;

  const strongCount = memoryStates?.filter(m => m.memoryScore >= 0.75).length || 0;
  const weakCount = memoryStates?.filter(m => m.memoryScore < 0.50).length || 0;

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Loader2 className="w-8 h-8 animate-spin text-indigo-500" />
      </div>
    );
  }

  return (
    <PageWrapper className="space-y-8 max-w-7xl mx-auto">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Memory Engine</h1>
        <p className="text-zinc-500">Track your concept mastery and retention over time.</p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="glass-card p-6 rounded-2xl">
          <div className="flex items-center gap-4 mb-4">
            <div className="w-12 h-12 rounded-xl bg-indigo-50 dark:bg-indigo-500/10 flex items-center justify-center text-indigo-600 dark:text-indigo-400">
              <BrainCircuit className="w-6 h-6" />
            </div>
            <div>
              <p className="text-sm font-medium text-zinc-500 dark:text-zinc-400">Average Memory</p>
              <h3 className="text-2xl font-bold">{(avgScore * 100).toFixed(1)}%</h3>
            </div>
          </div>
          <div className="h-2 w-full bg-zinc-100 dark:bg-zinc-800 rounded-full overflow-hidden">
            <div className="h-full bg-indigo-500 rounded-full" style={{ width: `${avgScore * 100}%` }}></div>
          </div>
        </motion.div>

        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }} className="bg-white dark:bg-zinc-950 p-6 rounded-2xl border border-zinc-200 dark:border-zinc-800 shadow-sm">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-emerald-50 dark:bg-emerald-500/10 flex items-center justify-center text-emerald-600 dark:text-emerald-400">
              <Zap className="w-6 h-6" />
            </div>
            <div>
              <p className="text-sm font-medium text-zinc-500 dark:text-zinc-400">Mastered Concepts</p>
              <h3 className="text-2xl font-bold">{strongCount}</h3>
            </div>
          </div>
        </motion.div>

        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2 }} className="bg-white dark:bg-zinc-950 p-6 rounded-2xl border border-zinc-200 dark:border-zinc-800 shadow-sm">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-rose-50 dark:bg-rose-500/10 flex items-center justify-center text-rose-600 dark:text-rose-400">
              <ShieldAlert className="w-6 h-6" />
            </div>
            <div>
              <p className="text-sm font-medium text-zinc-500 dark:text-zinc-400">Needs Review</p>
              <h3 className="text-2xl font-bold">{weakCount}</h3>
            </div>
          </div>
        </motion.div>
      </div>

      {/* Memory Table */}
      <div className="glass-card rounded-2xl overflow-hidden">
        <div className="p-6 border-b border-zinc-200 dark:border-zinc-800">
          <h2 className="text-lg font-bold">Concept Database</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left">
            <thead className="bg-zinc-50 dark:bg-zinc-900/50 text-zinc-500 dark:text-zinc-400 uppercase text-xs font-semibold">
              <tr>
                <th className="px-6 py-4">Concept</th>
                <th className="px-6 py-4">Memory Score</th>
                <th className="px-6 py-4">Difficulty</th>
                <th className="px-6 py-4">Importance</th>
                <th className="px-6 py-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-200 dark:divide-zinc-800">
              {memoryStates?.map((state) => {
                const isStrong = state.memoryScore >= 0.75;
                const isWeak = state.memoryScore < 0.50;

                return (
                  <tr key={state.id} className="hover:bg-zinc-50 dark:hover:bg-zinc-900/30 transition-colors">
                    <td className="px-6 py-4 font-medium text-zinc-900 dark:text-zinc-100">
                      {state.concept?.name || 'Unknown Concept'}
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <span className="w-12">{Math.round(state.memoryScore * 100)}%</span>
                        <div className="h-1.5 w-24 bg-zinc-100 dark:bg-zinc-800 rounded-full overflow-hidden">
                          <div 
                            className={cn("h-full rounded-full", isStrong ? "bg-emerald-500" : isWeak ? "bg-rose-500" : "bg-amber-500")} 
                            style={{ width: `${state.memoryScore * 100}%` }}
                          ></div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-zinc-500">
                      {Math.round((state.concept?.difficultyScore || 0) * 100)}%
                    </td>
                    <td className="px-6 py-4 text-zinc-500">
                      {Math.round((state.concept?.importanceScore || 0) * 100)}%
                    </td>
                    <td className="px-6 py-4 text-right">
                      <Link 
                        to={`/chat/${state.concept?.documentId}`} 
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-zinc-100 dark:bg-zinc-800 text-xs font-medium hover:bg-zinc-200 dark:hover:bg-zinc-700 transition-colors"
                      >
                        <Target className="w-3.5 h-3.5" /> Study
                      </Link>
                    </td>
                  </tr>
                );
              })}
              {(!memoryStates || memoryStates.length === 0) && (
                <tr>
                  <td colSpan={5} className="px-6 py-8 text-center text-zinc-500">
                    No concepts extracted yet. Upload a document to start learning.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </PageWrapper>
  );
}
