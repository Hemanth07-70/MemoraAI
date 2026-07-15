import { useQuery } from "@tanstack/react-query";
import { revisionApi } from "../services/apiClient";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { CalendarDays, Loader2, ArrowRight, BookOpen, AlertCircle } from "lucide-react";
import { cn } from "../utils/cn";
import { PageWrapper } from "../components/layout/PageWrapper";

export function Revision() {
  const { data: plan, isLoading, error } = useQuery({
    queryKey: ["revision-today"],
    queryFn: revisionApi.getToday,
  });

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Loader2 className="w-8 h-8 animate-spin text-indigo-500" />
      </div>
    );
  }

  if (error || !plan) {
    return (
      <div className="flex flex-col h-64 items-center justify-center text-zinc-500">
        <AlertCircle className="w-12 h-12 mb-4 text-zinc-300" />
        <p>Could not load today's revision plan.</p>
      </div>
    );
  }

  return (
    <PageWrapper className="space-y-8 max-w-5xl mx-auto">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Today's Revision</h1>
          <p className="text-zinc-500">Your dynamically generated, priority-based study plan.</p>
        </div>
        <div className="bg-indigo-50 dark:bg-indigo-500/10 text-indigo-700 dark:text-indigo-400 px-4 py-2 rounded-lg font-medium flex items-center gap-2 text-sm border border-indigo-100 dark:border-indigo-500/20">
          <CalendarDays className="w-4 h-4" />
          {new Date(plan.revisionDate).toLocaleDateString(undefined, { weekday: 'long', month: 'short', day: 'numeric' })}
        </div>
      </div>

      {plan.concepts.length === 0 ? (
        <div className="glass-card p-12 text-center rounded-2xl">
          <div className="w-16 h-16 bg-emerald-50 dark:bg-emerald-500/10 text-emerald-500 rounded-full flex items-center justify-center mb-4 mx-auto">
            <BookOpen className="w-8 h-8" />
          </div>
          <h3 className="text-xl font-bold mb-2">You're all caught up!</h3>
          <p className="text-zinc-500">You have no concepts due for review today. Great job keeping your memory strong.</p>
        </div>
      ) : (
        <div className="space-y-4">
          <p className="text-sm font-medium text-zinc-500 mb-2">
            {plan.concepts.length} {plan.concepts.length === 1 ? 'concept' : 'concepts'} to review
          </p>
          
          <div className="grid grid-cols-1 gap-4">
            {plan.concepts.map((concept, idx) => (
              <motion.div 
                key={concept.conceptId}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: idx * 0.05 }}
                className="glass-card p-5 rounded-2xl flex flex-col sm:flex-row sm:items-center gap-6 group hover:border-indigo-200 dark:hover:border-indigo-800 transition-colors"
              >
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-1">
                    <h3 className="font-bold text-lg">{concept.conceptName}</h3>
                    <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-rose-50 text-rose-600 dark:bg-rose-500/10 dark:text-rose-400 border border-rose-100 dark:border-rose-500/20">
                      Priority {(concept.priorityScore * 100).toFixed(0)}
                    </span>
                  </div>
                  <div className="flex items-center gap-4 text-sm text-zinc-500 mt-2">
                    <span className="flex items-center gap-1.5">
                      <div className={cn("w-2 h-2 rounded-full", concept.memoryScore < 0.5 ? "bg-rose-500" : "bg-amber-500")}></div>
                      Memory: {(concept.memoryScore * 100).toFixed(0)}%
                    </span>
                    <span className="flex items-center gap-1.5">
                      <div className="w-2 h-2 rounded-full bg-zinc-300 dark:bg-zinc-600"></div>
                      Difficulty: {(concept.difficultyScore * 100).toFixed(0)}%
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-3 shrink-0">
                  <Link 
                    to={`/graph/${concept.documentId}`} 
                    className="px-4 py-2 text-sm font-medium text-zinc-600 dark:text-zinc-300 bg-zinc-100 dark:bg-zinc-800 hover:bg-zinc-200 dark:hover:bg-zinc-700 rounded-lg transition-colors"
                  >
                    View Graph
                  </Link>
                  <Link 
                    to={`/quiz/${concept.documentId}`} 
                    className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg transition-colors shadow-sm shadow-indigo-500/20 flex items-center gap-2"
                  >
                    Review & Quiz <ArrowRight className="w-4 h-4" />
                  </Link>
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      )}
    </PageWrapper>
  );
}
