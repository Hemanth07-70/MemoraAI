import { useState, useRef, useEffect } from "react";
import { useParams, Link } from "react-router-dom";
import { useQuery, useMutation } from "@tanstack/react-query";
import { chatApi, conversationsApi } from "../services/apiClient";
import ReactMarkdown from "react-markdown";
import { motion, AnimatePresence } from "framer-motion";
import { Send, User, Bot, Loader2, ArrowLeft, MessageSquare, Plus, Trash2 } from "lucide-react";
import type { ChatMessage } from "../types";
import { cn } from "../utils/cn";
import { PageWrapper } from "../components/layout/PageWrapper";

export function Chat() {
  const { documentId } = useParams<{ documentId: string }>();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [activeConversationId, setActiveConversationId] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const { data: conversations, refetch: refetchConversations } = useQuery({
    queryKey: ["conversations"],
    queryFn: conversationsApi.getAll,
  });

  useEffect(() => {
    if (activeConversationId) {
      conversationsApi.getMessages(activeConversationId)
        .then(setMessages)
        .catch(err => console.error("Failed to load messages", err));
    } else {
      setMessages([]);
    }
  }, [activeConversationId]);

  const chatMutation = useMutation({
    mutationFn: chatApi.sendMessage,
    onSuccess: (data) => {
      setMessages((prev) => [
        ...prev,
        { role: "assistant", content: data?.answer || "Sorry, I couldn't generate an answer." },
      ]);
    },
    onError: (error: any) => {
      console.error("Chat error:", error);
      const isTimeout = error?.code === 'ECONNABORTED' || error?.message?.includes('timeout') || !error?.response;
      const msg = isTimeout
        ? "The server is waking up from sleep — this can take up to 60 seconds on the free tier. Please wait a moment and try again."
        : "Failed to get a response. Please try again.";
      setMessages((prev) => [
        ...prev,
        { role: "assistant", content: msg },
      ]);
    }
  });

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, chatMutation.isPending]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || chatMutation.isPending) return;

    const userText = input;
    const newMessage: ChatMessage = { role: "user", content: userText };
    setMessages((prev) => [...prev, newMessage]);
    setInput("");

    let currentConvId = activeConversationId;
    
    // Auto-create conversation on first message
    if (!currentConvId) {
      try {
        const title = userText.length > 30 ? userText.substring(0, 30) + '...' : userText;
        const newConv = await conversationsApi.create(title);
        currentConvId = newConv.id;
        setActiveConversationId(currentConvId);
        refetchConversations();
      } catch (err) {
        console.error("Failed to create conversation:", err);
      }
    }

    chatMutation.mutate({
      documentId: documentId,
      question: userText,
      conversationId: currentConvId || undefined,
    });
  };

  const handleNewChat = () => {
    setActiveConversationId(null);
    setMessages([]);
  };

  const handleDeleteConversation = async (e: React.MouseEvent, id: string) => {
    e.stopPropagation();
    if (window.confirm("Are you sure you want to delete this conversation?")) {
      await conversationsApi.delete(id);
      if (activeConversationId === id) {
        setActiveConversationId(null);
      }
      refetchConversations();
    }
  };

  return (
    <PageWrapper className="flex min-h-screen gap-6 glass-card rounded-2xl overflow-hidden relative">
      
      {/* Sidebar for Conversations */}
      <div className="w-64 border-r border-slate-200/50 dark:border-zinc-800 flex flex-col bg-slate-50/50 dark:bg-zinc-900/50 hidden md:flex">
        <div className="p-4 border-b border-zinc-200 dark:border-zinc-800">
          <button 
            onClick={handleNewChat}
            className="w-full flex items-center justify-center gap-2 py-2.5 px-4 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-medium transition-colors"
          >
            <Plus className="w-4 h-4" />
            New Chat
          </button>
        </div>
        <div className="flex-1 overflow-y-auto p-2 custom-scrollbar">
          <div className="space-y-1">
            {conversations?.map((conv) => (
              <div 
                key={conv.id}
                onClick={() => setActiveConversationId(conv.id)}
                className={cn(
                  "w-full flex items-center justify-between gap-3 px-3 py-2.5 text-sm rounded-lg cursor-pointer transition-colors group",
                  activeConversationId === conv.id 
                    ? "bg-zinc-200 dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100 font-medium" 
                    : "text-zinc-600 dark:text-zinc-400 hover:bg-zinc-200/50 dark:hover:bg-zinc-800/50"
                )}
              >
                <div className="flex items-center gap-3 overflow-hidden">
                  <MessageSquare className="w-4 h-4 shrink-0" />
                  <span className="truncate">{conv.title}</span>
                </div>
                <button
                  onClick={(e) => handleDeleteConversation(e, conv.id)}
                  className="p-1 rounded hover:bg-zinc-300 dark:hover:bg-zinc-700 text-zinc-400 hover:text-rose-500 opacity-0 group-hover:opacity-100 transition-all"
                  title="Delete Chat"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            ))}
            {(!conversations || conversations.length === 0) && (
              <p className="text-center text-xs text-zinc-500 p-4">No past conversations.</p>
            )}
          </div>
        </div>
      </div>

      {/* Main Chat Area */}
      <div className="flex-1 flex flex-col min-w-0 bg-white dark:bg-zinc-950">
        {/* Header */}
        <div className="h-16 px-6 border-b border-slate-200/50 dark:border-zinc-800 flex items-center gap-4 bg-slate-50/30 dark:bg-zinc-900/50 shrink-0">
        <Link to="/documents" className="p-2 -ml-2 hover:bg-zinc-200 dark:hover:bg-zinc-800 rounded-full transition-colors text-zinc-500">
          <ArrowLeft className="w-5 h-5" />
        </Link>
        <div>
          <h2 className="font-bold text-zinc-900 dark:text-zinc-50">
            {documentId ? "AI Document Assistant" : "Global AI Assistant"}
          </h2>
          <p className="text-xs text-zinc-500">
            {documentId ? "Ask questions about your uploaded document" : "Ask questions across all your uploaded documents"}
          </p>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-6 space-y-6 custom-scrollbar">
        {messages.length === 0 ? (
          <div className="h-full flex flex-col items-center justify-center text-zinc-500 space-y-4">
            <Bot className="w-12 h-12 text-indigo-200 dark:text-indigo-900" />
            <p>
              {documentId 
                ? "How can I help you with this document today?" 
                : "How can I help you across your knowledge base today?"}
            </p>
          </div>
        ) : (
          <AnimatePresence initial={false}>
            {messages.map((msg, idx) => (
              <motion.div
                key={idx}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className={cn(
                  "flex gap-4 max-w-4xl mx-auto",
                  msg.role === "user" ? "flex-row-reverse" : "flex-row"
                )}
              >
                <div className={cn(
                  "w-8 h-8 rounded-full flex items-center justify-center shrink-0 mt-1",
                  msg.role === "user" ? "bg-indigo-100 text-indigo-700 dark:bg-indigo-900 dark:text-indigo-300" : "bg-emerald-100 text-emerald-700 dark:bg-emerald-900 dark:text-emerald-300"
                )}>
                  {msg.role === "user" ? <User className="w-4 h-4" /> : <Bot className="w-4 h-4" />}
                </div>
                
                <div className={cn(
                  "px-5 py-3.5 rounded-2xl max-w-[85%]",
                  msg.role === "user" 
                    ? "bg-indigo-600 text-white rounded-tr-sm" 
                    : "bg-zinc-100 dark:bg-zinc-900 text-zinc-800 dark:text-zinc-200 rounded-tl-sm border border-zinc-200 dark:border-zinc-800"
                )}>
                  {msg.role === "user" ? (
                    <p className="whitespace-pre-wrap text-[15px] leading-relaxed">{msg.content}</p>
                  ) : (
                    <div className="prose prose-sm dark:prose-invert max-w-none prose-p:leading-relaxed prose-pre:bg-zinc-900 prose-pre:border prose-pre:border-zinc-800">
                      <ReactMarkdown>{msg.content}</ReactMarkdown>
                    </div>
                  )}
                </div>
              </motion.div>
            ))}
          </AnimatePresence>
        )}

        {chatMutation.isPending && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="flex gap-4 max-w-4xl mx-auto flex-row"
          >
            <div className="w-8 h-8 rounded-full bg-emerald-100 text-emerald-700 dark:bg-emerald-900 dark:text-emerald-300 flex items-center justify-center shrink-0 mt-1">
              <Bot className="w-4 h-4" />
            </div>
            <div className="px-5 py-4 rounded-2xl bg-zinc-100 dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 rounded-tl-sm flex items-center gap-2">
              <span className="w-1.5 h-1.5 bg-zinc-400 rounded-full animate-bounce" style={{ animationDelay: "0ms" }}></span>
              <span className="w-1.5 h-1.5 bg-zinc-400 rounded-full animate-bounce" style={{ animationDelay: "150ms" }}></span>
              <span className="w-1.5 h-1.5 bg-zinc-400 rounded-full animate-bounce" style={{ animationDelay: "300ms" }}></span>
            </div>
          </motion.div>
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Input Area */}
      <div className="p-4 bg-transparent border-t border-slate-200/50 dark:border-zinc-800">
        <form onSubmit={handleSubmit} className="max-w-4xl mx-auto relative flex items-center">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            disabled={chatMutation.isPending}
            placeholder={documentId ? "Ask a question about the document..." : "Ask a question across all documents..."}
            className="w-full bg-zinc-100 dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 rounded-full pl-6 pr-14 py-3.5 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 dark:focus:ring-indigo-500/30 transition-all text-[15px] disabled:opacity-50"
          />
          <button
            type="submit"
            disabled={!input.trim() || chatMutation.isPending}
            className="absolute right-2 p-2 bg-indigo-600 text-white rounded-full hover:bg-indigo-700 disabled:opacity-50 disabled:hover:bg-indigo-600 transition-colors"
          >
            {chatMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
          </button>
        </form>
        <p className="text-center text-[11px] text-zinc-400 mt-3">AI responses can make mistakes. Verify important information.</p>
        </div>
      </div>
    </PageWrapper>
  );
}
