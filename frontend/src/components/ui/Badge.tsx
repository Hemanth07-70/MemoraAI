import * as React from "react"
import { cn } from "../../utils/cn"

export interface BadgeProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: "default" | "secondary" | "destructive" | "outline" | "success" | "warning" | "premium"
}

function Badge({ className, variant = "default", ...props }: BadgeProps) {
  const variants = {
    default: "border-transparent bg-indigo-600 text-white hover:bg-indigo-700",
    secondary: "border-transparent bg-slate-100 text-slate-900 hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-50",
    destructive: "border-transparent bg-red-500 text-white hover:bg-red-600 dark:bg-red-900 dark:text-red-100",
    success: "border-transparent bg-emerald-500 text-white hover:bg-emerald-600 dark:bg-emerald-900 dark:text-emerald-100",
    warning: "border-transparent bg-amber-500 text-white hover:bg-amber-600 dark:bg-amber-900 dark:text-amber-100",
    outline: "text-slate-950 dark:text-slate-50 border border-slate-200 dark:border-slate-800",
    premium: "border-transparent bg-gradient-to-r from-indigo-500 to-purple-600 text-white shadow-sm"
  }

  return (
    <div
      className={cn(
        "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2",
        variants[variant],
        className
      )}
      {...props}
    />
  )
}

export { Badge }
