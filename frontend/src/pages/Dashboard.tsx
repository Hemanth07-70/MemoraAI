import { motion } from "framer-motion";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { 
  FileText, 
  BrainCircuit, 
  CalendarDays, 
  Trophy,
  ArrowRight,
  Upload,
  MessageSquare
} from "lucide-react";
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import { PageWrapper } from "../components/layout/PageWrapper";
import { documentsApi, memoryApi, revisionApi } from "../services/apiClient";
import { Skeleton } from "../components/ui/Skeleton";

const mockChartData = [
  { name: 'Mon', score: 65 },
  { name: 'Tue', score: 72 },
  { name: 'Wed', score: 78 },
  { name: 'Thu', score: 81 },
  { name: 'Fri', score: 86 },
  { name: 'Sat', score: 90 },
  { name: 'Sun', score: 92 },
];

export function Dashboard() {
  const { data: documents = [], isLoading: isLoadingDocs } = useQuery({
    queryKey: ['documents'],
    queryFn: documentsApi.getAll,
    refetchInterval: (query) => {
      const docs = query.state.data;
      if (!docs) return false;
      const needsPolling = docs.some(d => d.status === 'PROCESSING' || d.status === 'UPLOADED');
      return needsPolling ? 3000 : false;
    }
  });

  const { data: memoryStates = [], isLoading: isLoadingMemory } = useQuery({
    queryKey: ['memory-states'],
    queryFn: memoryApi.getForUser
  });

  const { data: revisionPlan, isLoading: isLoadingRevision } = useQuery({
    queryKey: ['revision-today'],
    queryFn: revisionApi.getToday
  });

  const avgMemoryScore = memoryStates.length 
    ? Math.round(memoryStates.reduce((acc, curr) => acc + curr.memoryScore, 0) / memoryStates.length * 100) 
    : 0;

  const totalConcepts = memoryStates.length;
  const todayRevisionsCount = revisionPlan?.concepts?.length || 0;

  const stats = [
    { name: "Documents Uploaded", value: isLoadingDocs ? "..." : documents.length.toString(), icon: FileText, change: "Total" },
    { name: "Concepts Extracted", value: isLoadingMemory ? "..." : totalConcepts.toString(), icon: BrainCircuit, change: "Total" },
    { name: "Today's Revisions", value: isLoadingRevision ? "..." : todayRevisionsCount.toString(), icon: CalendarDays, change: "Pending" },
    { name: "Avg. Memory Score", value: isLoadingMemory ? "..." : `${avgMemoryScore}%`, icon: Trophy, change: "Overall" },
  ];

  return (
    <PageWrapper className="space-y-8 max-w-7xl mx-auto">
      {/* Hero Section */}
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-indigo-600 to-purple-600 p-8 sm:p-10 text-white shadow-xl shadow-indigo-500/20"
      >
        <div className="relative z-10 max-w-2xl">
          <h1 className="text-3xl sm:text-4xl font-bold tracking-tight mb-4">
            Welcome back, Student
          </h1>
          <p className="text-indigo-100 text-lg mb-8 leading-relaxed">
            You have {todayRevisionsCount} concepts to review today. Your overall memory retention is {avgMemoryScore}%. 
            Ready to master your knowledge?
          </p>
          <div className="flex flex-wrap gap-4">
            <Link 
              to="/revision"
              className="bg-white text-indigo-600 px-6 py-3 rounded-full font-medium hover:bg-indigo-50 transition-colors shadow-sm inline-flex items-center gap-2"
            >
              Start Revision <ArrowRight className="w-4 h-4" />
            </Link>
            <Link 
              to="/documents"
              className="bg-indigo-500/30 text-white px-6 py-3 rounded-full font-medium hover:bg-indigo-500/40 transition-colors backdrop-blur-sm border border-indigo-400/30 inline-flex items-center gap-2"
            >
              <Upload className="w-4 h-4" /> Upload Document
            </Link>
          </div>
        </div>
        
        {/* Decorative background elements */}
        <div className="absolute right-0 top-0 -mr-20 -mt-20 w-96 h-96 bg-white opacity-5 rounded-full blur-3xl mix-blend-overlay"></div>
        <div className="absolute right-40 bottom-0 -mb-20 w-72 h-72 bg-purple-400 opacity-20 rounded-full blur-2xl mix-blend-overlay"></div>
      </motion.div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
        {stats.map((stat, idx) => (
          <motion.div 
            key={stat.name}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: idx * 0.1 }}
            className="glass-card p-6 rounded-2xl"
          >
            <div className="flex items-center justify-between mb-4">
              <div className="w-10 h-10 rounded-full bg-indigo-50 dark:bg-indigo-500/10 flex items-center justify-center text-indigo-600 dark:text-indigo-400">
                <stat.icon className="w-5 h-5" />
              </div>
              <span className="text-xs font-medium text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-500/10 px-2 py-1 rounded-full">
                {stat.change}
              </span>
            </div>
            <h3 className="text-zinc-500 dark:text-zinc-400 text-sm font-medium mb-1">{stat.name}</h3>
            <p className="text-2xl font-bold text-zinc-900 dark:text-zinc-50">{stat.value}</p>
          </motion.div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Chart Section */}
        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="lg:col-span-2 glass-card p-6 rounded-2xl flex flex-col"
        >
          <h3 className="text-lg font-bold text-zinc-900 dark:text-zinc-50 mb-6">Learning Progress</h3>
          <div className="flex-1 min-h-[300px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={mockChartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorScore" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#6366f1" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e4e4e7" className="dark:stroke-zinc-800" />
                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{fill: '#71717a', fontSize: 12}} dy={10} />
                <YAxis axisLine={false} tickLine={false} tick={{fill: '#71717a', fontSize: 12}} />
                <Tooltip 
                  contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)' }}
                  itemStyle={{ color: '#6366f1', fontWeight: 600 }}
                />
                <Area type="monotone" dataKey="score" stroke="#6366f1" strokeWidth={3} fillOpacity={1} fill="url(#colorScore)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </motion.div>

        {/* Recent Activity */}
        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
          className="glass-card p-6 rounded-2xl"
        >
          <div className="flex items-center justify-between mb-6">
            <h3 className="text-lg font-bold text-zinc-900 dark:text-zinc-50">Recent Documents</h3>
            <Link to="/documents" className="text-sm text-indigo-600 dark:text-indigo-400 hover:underline font-medium">View all</Link>
          </div>
          
          <div className="space-y-4">
            {isLoadingDocs ? (
              [1, 2, 3].map((i) => <Skeleton key={i} className="h-16 w-full rounded-xl" />)
            ) : documents.length === 0 ? (
              <p className="text-sm text-zinc-500 text-center py-4">No documents uploaded yet.</p>
            ) : (
              documents.slice(0, 5).map((doc) => (
                <div key={doc.id} className="flex items-start gap-4 p-3 rounded-xl hover:bg-zinc-50 dark:hover:bg-zinc-900/50 transition-colors border border-transparent hover:border-zinc-200 dark:hover:border-zinc-800 group">
                  <div className="w-10 h-10 rounded-lg bg-indigo-50 dark:bg-indigo-500/10 flex items-center justify-center text-indigo-600 dark:text-indigo-400 shrink-0">
                    <FileText className="w-5 h-5" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <h4 className="text-sm font-semibold text-zinc-900 dark:text-zinc-50 truncate">{doc.originalFileName}</h4>
                    <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1 capitalize">{doc.status.toLowerCase()} • {new Date(doc.uploadedAt).toLocaleDateString()}</p>
                  </div>
                  <Link to={`/chat/${doc.id}`} className="opacity-0 group-hover:opacity-100 p-2 text-zinc-400 hover:text-indigo-600 dark:hover:text-indigo-400 transition-all">
                    <MessageSquare className="w-4 h-4" />
                  </Link>
                </div>
              ))
            )}
          </div>
        </motion.div>
      </div>
    </PageWrapper>
  );
}
