import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { documentsApi } from "../services/apiClient";
import { 
  FileText, 
  Upload, 
  Loader2, 
  Share2, 
  MessageSquare, 
  CheckCircle, 
  Trash2, 
  BrainCircuit,
  AlertCircle
} from "lucide-react";
import { useState, useRef } from "react";
import { motion } from "framer-motion";
import { PageWrapper } from "../components/layout/PageWrapper";

export function Documents() {
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [isUploading, setIsUploading] = useState(false);

  const { data: documents, isLoading } = useQuery({
    queryKey: ["documents"],
    queryFn: documentsApi.getAll,
  });

  const uploadMutation = useMutation({
    mutationFn: documentsApi.upload,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["documents"] });
      setIsUploading(false);
    },
    onError: () => setIsUploading(false),
  });

  const deleteMutation = useMutation({
    mutationFn: documentsApi.delete,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["documents"] }),
  });

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setIsUploading(true);
      uploadMutation.mutate(e.target.files[0]);
    }
  };

  return (
    <PageWrapper className="space-y-6 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Documents</h1>
          <p className="text-zinc-500">Upload and manage your study materials.</p>
        </div>
        
        <input 
          type="file" 
          ref={fileInputRef} 
          onChange={handleFileChange} 
          className="hidden" 
          accept=".pdf,.txt,.docx" 
        />
        <button 
          onClick={() => fileInputRef.current?.click()}
          disabled={isUploading}
          className="bg-indigo-600 text-white px-5 py-2.5 rounded-lg font-medium hover:bg-indigo-700 transition-colors shadow-sm shadow-indigo-500/20 flex items-center gap-2 disabled:opacity-70"
        >
          {isUploading ? <Loader2 className="w-5 h-5 animate-spin" /> : <Upload className="w-5 h-5" />}
          {isUploading ? "Uploading..." : "Upload Document"}
        </button>
      </div>

      {isLoading ? (
        <div className="flex h-64 items-center justify-center">
          <Loader2 className="w-8 h-8 animate-spin text-indigo-500" />
        </div>
      ) : documents?.length === 0 ? (
        <div className="bg-white dark:bg-zinc-950 rounded-2xl border border-zinc-200 dark:border-zinc-800 p-12 flex flex-col items-center justify-center text-center shadow-sm">
          <div className="w-16 h-16 bg-indigo-50 dark:bg-indigo-500/10 text-indigo-500 rounded-full flex items-center justify-center mb-4">
            <FileText className="w-8 h-8" />
          </div>
          <h3 className="text-xl font-bold mb-2">No documents yet</h3>
          <p className="text-zinc-500 max-w-sm mb-6">Upload your first document to start extracting concepts, building knowledge graphs, and taking quizzes.</p>
          <button 
            onClick={() => fileInputRef.current?.click()}
            className="bg-indigo-50 text-indigo-600 dark:bg-indigo-500/10 dark:text-indigo-400 px-6 py-2 rounded-lg font-medium hover:bg-indigo-100 dark:hover:bg-indigo-500/20 transition-colors"
          >
            Upload your first file
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {documents?.map((doc, idx) => (
            <motion.div 
              key={doc.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: idx * 0.05 }}
              className="glass-card rounded-2xl border border-zinc-200 dark:border-zinc-800 shadow-sm overflow-hidden flex flex-col hover:border-indigo-300 dark:hover:border-indigo-700/50 transition-colors group"
            >
              <div className="p-5 border-b border-zinc-100 dark:border-zinc-800/50 flex items-start gap-4">
                <div className="w-10 h-10 rounded-lg bg-indigo-50 dark:bg-indigo-500/10 flex items-center justify-center text-indigo-600 dark:text-indigo-400 shrink-0">
                  <FileText className="w-5 h-5" />
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="font-semibold text-zinc-900 dark:text-zinc-50 truncate" title={doc.originalFileName}>
                    {doc.originalFileName}
                  </h3>
                  <div className="flex items-center gap-2 mt-1">
                    {doc.status === 'READY' ? (
                      <span className="flex items-center gap-1 text-xs font-medium text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-500/10 px-2 py-0.5 rounded-full">
                        <CheckCircle className="w-3 h-3" /> Processed
                      </span>
                    ) : doc.status === 'FAILED' ? (
                      <span className="flex items-center gap-1 text-xs font-medium text-rose-600 dark:text-rose-400 bg-rose-50 dark:bg-rose-500/10 px-2 py-0.5 rounded-full">
                        <AlertCircle className="w-3 h-3" /> Failed
                      </span>
                    ) : (
                      <span className="flex items-center gap-1 text-xs font-medium text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-500/10 px-2 py-0.5 rounded-full">
                        <Loader2 className="w-3 h-3 animate-spin" /> Processing
                      </span>
                    )}
                    <span className="text-xs text-zinc-400">•</span>
                    <span className="text-xs text-zinc-500">{(doc.size / 1024).toFixed(0)} KB</span>
                  </div>
                </div>
              </div>
              
              <div className="p-4 bg-zinc-50 dark:bg-zinc-900/30 flex justify-between items-center mt-auto">
                <div className="flex items-center gap-2">
                  <Link 
                    to={`/graph/${doc.id}`} 
                    className="p-2 text-zinc-500 hover:text-indigo-600 hover:bg-indigo-50 dark:hover:bg-indigo-500/10 rounded-lg transition-colors tooltip-trigger"
                    title="Knowledge Graph"
                  >
                    <Share2 className="w-5 h-5" />
                  </Link>
                  <Link 
                    to={`/chat/${doc.id}`} 
                    className="p-2 text-zinc-500 hover:text-indigo-600 hover:bg-indigo-50 dark:hover:bg-indigo-500/10 rounded-lg transition-colors tooltip-trigger"
                    title="AI Chat"
                  >
                    <MessageSquare className="w-5 h-5" />
                  </Link>
                  <Link 
                    to={`/quiz/${doc.id}`} 
                    className="p-2 text-zinc-500 hover:text-indigo-600 hover:bg-indigo-50 dark:hover:bg-indigo-500/10 rounded-lg transition-colors tooltip-trigger"
                    title="Quiz Center"
                  >
                    <BrainCircuit className="w-5 h-5" />
                  </Link>
                </div>
                <button 
                  onClick={() => deleteMutation.mutate(doc.id)}
                  className="p-2 text-zinc-400 hover:text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-500/10 rounded-lg transition-colors opacity-0 group-hover:opacity-100"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </motion.div>
          ))}
        </div>
      )}
    </PageWrapper>
  );
}
