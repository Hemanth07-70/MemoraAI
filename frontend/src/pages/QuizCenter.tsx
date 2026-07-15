import { useState } from "react";
import { useParams, Link } from "react-router-dom";
import { useQuery, useMutation } from "@tanstack/react-query";
import { quizApi, documentsApi } from "../services/apiClient";
import { motion, AnimatePresence } from "framer-motion";
import { Loader2, ArrowLeft, BrainCircuit, ArrowRight, FileText, PlayCircle, BookOpen, CheckCircle2, XCircle } from "lucide-react";
import { cn } from "../utils/cn";
import type { QuizDto, QuizResultDto } from "../types";
import { PageWrapper } from "../components/layout/PageWrapper";

export function QuizCenter() {
  const { documentId } = useParams<{ documentId: string }>();

  const [activeQuiz, setActiveQuiz] = useState<QuizDto | null>(null);
  const [currentQuestionIdx, setCurrentQuestionIdx] = useState(0);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [quizResult, setQuizResult] = useState<QuizResultDto | null>(null);
  const [showText, setShowText] = useState(false);
  const [showReport, setShowReport] = useState(false);

  const { data: documents, isLoading: isLoadingDocs } = useQuery({
    queryKey: ["documents"],
    queryFn: documentsApi.getAll,
    enabled: !documentId,
  });

  const { data: documentText, isLoading: isLoadingText } = useQuery({
    queryKey: ["documentText", documentId],
    queryFn: () => documentsApi.getText(documentId!),
    enabled: !!documentId && showText,
  });

  const generateMutation = useMutation({
    mutationFn: () => quizApi.generate(documentId!, 5),
    onSuccess: (data) => {
      setActiveQuiz(data);
      setCurrentQuestionIdx(0);
      setAnswers({});
      setQuizResult(null);
      setShowReport(false);
    },
  });

  const submitMutation = useMutation({
    mutationFn: () => quizApi.submit(activeQuiz!.id, {
      answers: Object.entries(answers).map(([questionId, userAnswer]) => ({ questionId, userAnswer }))
    }),
    onSuccess: (data) => {
      setQuizResult(data);
    },
  });

  const handleStartQuiz = () => {
    generateMutation.mutate();
  };

  const handleOptionSelect = (option: string) => {
    if (quizResult || !activeQuiz) return;
    setAnswers(prev => ({
      ...prev,
      [activeQuiz.questions[currentQuestionIdx].id]: option
    }));
  };

  const handleNext = () => {
    if (currentQuestionIdx < (activeQuiz?.questions.length || 0) - 1) {
      setCurrentQuestionIdx(prev => prev + 1);
    } else {
      submitMutation.mutate();
    }
  };

  if (!documentId) {
    return (
      <PageWrapper className="flex flex-col min-h-screen w-full">
        <div className="mb-8">
          <h2 className="text-2xl font-bold text-zinc-900 dark:text-white flex items-center gap-2">
            <BrainCircuit className="w-6 h-6 text-indigo-500" />
            Quiz Center
          </h2>
          <p className="text-zinc-500 mt-2">Select a document to test your knowledge and reinforce your memory.</p>
        </div>

        {isLoadingDocs ? (
          <div className="flex-1 flex items-center justify-center">
            <Loader2 className="w-8 h-8 animate-spin text-indigo-500" />
          </div>
        ) : !documents || documents.length === 0 ? (
          <div className="flex-1 flex flex-col items-center justify-center text-zinc-500">
            <FileText className="w-12 h-12 mb-4 text-zinc-300" />
            <p>No documents found.</p>
            <Link to="/documents" className="mt-4 px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors font-medium">Upload a Document</Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {documents.filter(doc => doc.status === 'READY').map((doc) => (
              <motion.div
                key={doc.id}
                whileHover={{ y: -4 }}
                className="glass-card bg-white dark:bg-zinc-950 p-6 rounded-2xl border border-zinc-200 dark:border-zinc-800 shadow-sm flex flex-col justify-between"
              >
                <div>
                  <h3 className="font-bold text-lg mb-2 truncate" title={doc.originalFileName}>{doc.originalFileName}</h3>
                  <div className="flex flex-wrap gap-2 mb-4">
                    <span className="text-xs px-2 py-1 bg-zinc-100 dark:bg-zinc-900 rounded-md text-zinc-600 dark:text-zinc-400">
                      {doc.extension}
                    </span>
                  </div>
                </div>
                <Link 
                  to={`/quiz/${doc.id}`}
                  className="w-full py-2.5 bg-indigo-50 dark:bg-indigo-500/10 hover:bg-indigo-100 dark:hover:bg-indigo-500/20 text-indigo-600 dark:text-indigo-400 rounded-lg text-sm font-semibold flex items-center justify-center gap-2 transition-colors"
                >
                  Generate Quiz <ArrowRight className="w-4 h-4" />
                </Link>
              </motion.div>
            ))}
          </div>
        )}
      </PageWrapper>
    );
  }



  if (quizResult) {
    return (
      <PageWrapper className="max-w-3xl mx-auto mt-10">
        <motion.div 
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          className="glass-card max-w-4xl mx-auto rounded-2xl shadow-xl overflow-hidden relative border border-zinc-200 dark:border-zinc-800"
        >
          <div className="p-10 text-center bg-gradient-to-b from-indigo-50 to-white dark:from-indigo-500/10 dark:to-zinc-950 border-b border-zinc-100 dark:border-zinc-800">
            <div className="w-20 h-20 bg-indigo-100 dark:bg-indigo-900/50 text-indigo-600 dark:text-indigo-400 rounded-full flex items-center justify-center mx-auto mb-6">
              <BrainCircuit className="w-10 h-10" />
            </div>
            <h2 className="text-3xl font-bold mb-2">Quiz Completed!</h2>
            <p className="text-zinc-500">Your memory score has been updated based on your performance.</p>
          </div>

          <div className="p-8">
            <div className="grid grid-cols-3 gap-6 mb-10">
              <div className="text-center p-6 bg-zinc-50 dark:bg-zinc-900 rounded-2xl border border-zinc-100 dark:border-zinc-800">
                <p className="text-sm font-medium text-zinc-500 mb-1">Score</p>
                <p className="text-3xl font-bold text-indigo-600 dark:text-indigo-400">{quizResult.percentage}%</p>
              </div>
              <div className="text-center p-6 bg-emerald-50 dark:bg-emerald-900/20 rounded-2xl border border-emerald-100 dark:border-emerald-900/30">
                <p className="text-sm font-medium text-emerald-600 dark:text-emerald-500 mb-1">Correct</p>
                <p className="text-3xl font-bold text-emerald-600 dark:text-emerald-400">{quizResult.correctAnswers}</p>
              </div>
              <div className="text-center p-6 bg-rose-50 dark:bg-rose-900/20 rounded-2xl border border-rose-100 dark:border-rose-900/30">
                <p className="text-sm font-medium text-rose-600 dark:text-rose-500 mb-1">Incorrect</p>
                <p className="text-3xl font-bold text-rose-600 dark:text-rose-400">{quizResult.wrongAnswers}</p>
              </div>
            </div>

            <div className="flex justify-center gap-4">
              <button 
                onClick={handleStartQuiz}
                className="px-6 py-3 bg-indigo-600 text-white font-medium rounded-lg hover:bg-indigo-700 transition-colors"
              >
                Take Another Quiz
              </button>
              <button 
                onClick={() => setShowReport(!showReport)}
                className="px-6 py-3 bg-white dark:bg-zinc-950 border border-indigo-200 dark:border-indigo-900 text-indigo-600 dark:text-indigo-400 font-medium rounded-lg hover:bg-indigo-50 dark:hover:bg-indigo-900/20 transition-colors"
              >
                {showReport ? "Hide Report" : "View Report"}
              </button>
              <Link 
                to="/memory"
                className="px-6 py-3 bg-zinc-100 dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300 font-medium rounded-lg hover:bg-zinc-200 dark:hover:bg-zinc-700 transition-colors"
              >
                View Memory Stats
              </Link>
            </div>

            <AnimatePresence>
              {showReport && quizResult.questionResults && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: "auto" }}
                  exit={{ opacity: 0, height: 0 }}
                  className="mt-8 text-left space-y-4"
                >
                  <h3 className="text-xl font-bold mb-4">Detailed Report</h3>
                  {quizResult.questionResults.map((qr, idx) => (
                    <div key={qr.questionId} className={cn(
                      "p-5 rounded-xl border",
                      qr.correct ? "bg-emerald-50/50 dark:bg-emerald-900/10 border-emerald-100 dark:border-emerald-900/30" : "bg-rose-50/50 dark:bg-rose-900/10 border-rose-100 dark:border-rose-900/30"
                    )}>
                      <div className="flex items-start gap-3">
                        <div className="mt-1">
                          {qr.correct ? (
                            <CheckCircle2 className="w-5 h-5 text-emerald-500" />
                          ) : (
                            <XCircle className="w-5 h-5 text-rose-500" />
                          )}
                        </div>
                        <div className="flex-1 space-y-3">
                          <p className="font-semibold text-zinc-900 dark:text-zinc-100">{idx + 1}. {qr.questionText}</p>
                          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
                            <div className="bg-white dark:bg-zinc-950 p-3 rounded-lg border border-zinc-200 dark:border-zinc-800">
                              <span className="text-zinc-500 block mb-1">Your Answer:</span>
                              <span className={qr.correct ? "text-emerald-600 dark:text-emerald-400 font-medium" : "text-rose-600 dark:text-rose-400 font-medium"}>
                                {qr.userAnswer || "No answer provided"}
                              </span>
                            </div>
                            <div className="bg-white dark:bg-zinc-950 p-3 rounded-lg border border-zinc-200 dark:border-zinc-800">
                              <span className="text-zinc-500 block mb-1">Correct Answer:</span>
                              <span className="text-emerald-600 dark:text-emerald-400 font-medium">
                                {qr.correctAnswer}
                              </span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </motion.div>
      </PageWrapper>
    );
  }

  if (activeQuiz) {
    if (!activeQuiz.questions || activeQuiz.questions.length === 0) {
      return (
        <PageWrapper className="flex flex-col items-center justify-center min-h-screen text-center max-w-lg mx-auto">
          <div className="w-20 h-20 bg-rose-50 dark:bg-rose-500/10 text-rose-600 dark:text-rose-400 rounded-2xl flex items-center justify-center mb-6 shadow-sm">
            <BrainCircuit className="w-10 h-10" />
          </div>
          <h1 className="text-3xl font-bold mb-4">Quiz Generation Failed</h1>
          <p className="text-zinc-500 mb-8 leading-relaxed">
            The AI was unable to generate a quiz for this document. This usually happens if the document doesn't contain enough extractable concepts, or if the AI service timed out.
          </p>
          <button 
            onClick={() => setActiveQuiz(null)}
            className="bg-indigo-600 text-white px-8 py-3.5 rounded-full font-medium hover:bg-indigo-700 transition-colors shadow-lg shadow-indigo-500/20"
          >
            Go Back
          </button>
        </PageWrapper>
      );
    }

    const question = activeQuiz.questions[currentQuestionIdx];
    const isAnswered = !!answers[question.id];
    const progress = ((currentQuestionIdx + 1) / activeQuiz.questions.length) * 100;

    return (
      <PageWrapper className="max-w-7xl mx-auto w-full">
        <div className="flex flex-col lg:flex-row gap-6 min-h-screen">
          <div className={cn("flex flex-col transition-all duration-300", showText ? "lg:w-1/2" : "w-full max-w-3xl mx-auto")}>
            <div className="mb-6 flex items-center justify-between">
              <div className="flex items-center gap-4 flex-1">
                <button onClick={() => setActiveQuiz(null)} className="p-2 -ml-2 hover:bg-zinc-200 dark:hover:bg-zinc-800 rounded-full transition-colors text-zinc-500">
                  <ArrowLeft className="w-5 h-5" />
                </button>
                <div className="flex-1 max-w-md">
                  <div className="flex justify-between text-sm font-medium mb-2">
                    <span className="text-zinc-500">Question {currentQuestionIdx + 1} of {activeQuiz.questions.length}</span>
                    <span className="text-indigo-600 dark:text-indigo-400">{Math.round(progress)}%</span>
                  </div>
                  <div className="h-2 bg-zinc-200 dark:bg-zinc-800 rounded-full overflow-hidden">
                    <div className="h-full bg-indigo-500 rounded-full transition-all duration-300" style={{ width: `${progress}%` }}></div>
                  </div>
                </div>
              </div>
              
              <button 
                onClick={() => setShowText(!showText)}
                className="flex items-center gap-2 px-4 py-2 bg-zinc-100 dark:bg-zinc-900 hover:bg-zinc-200 dark:hover:bg-zinc-800 rounded-lg text-sm font-medium transition-colors border border-zinc-200 dark:border-zinc-700"
              >
                <BookOpen className="w-4 h-4" />
                {showText ? "Hide Text" : "View Source Text"}
              </button>
            </div>

            <div className="flex-1 overflow-y-auto custom-scrollbar pb-10">
              <AnimatePresence mode="wait">
                <motion.div
                  key={question.id}
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  className="glass-card bg-white dark:bg-zinc-950 rounded-2xl border border-zinc-200 dark:border-zinc-800 shadow-sm overflow-hidden"
                >
                  <div className="p-8 md:p-10 border-b border-zinc-100 dark:border-zinc-800/50">
                    <h2 className="text-xl md:text-2xl font-bold leading-relaxed">{question.questionText}</h2>
                  </div>
                  
                  <div className="p-8 md:p-10 space-y-3 bg-zinc-50/50 dark:bg-zinc-900/30">
                    {question.questionType === 'FILL_BLANK' || !question.options || question.options.length === 0 ? (
                      <div className="pt-4">
                        <input 
                          type="text" 
                          value={answers[question.id] || ''}
                          onChange={(e) => setAnswers(prev => ({ ...prev, [question.id]: e.target.value }))}
                          placeholder="Type your answer here..."
                          className="w-full bg-white dark:bg-zinc-900 border-2 border-zinc-200 dark:border-zinc-700 rounded-xl p-5 text-[15px] font-medium focus:outline-none focus:border-indigo-500 focus:ring-4 focus:ring-indigo-500/20 transition-all shadow-sm"
                        />
                      </div>
                    ) : (
                      question.options.map((option, idx) => {
                      const isSelected = answers[question.id] === option;
                      return (
                        <button
                          key={idx}
                          onClick={() => handleOptionSelect(option)}
                          className={cn(
                            "w-full text-left p-4 rounded-xl border-2 transition-all font-medium flex items-center justify-between",
                            isSelected 
                              ? "border-indigo-500 bg-indigo-50 dark:bg-indigo-500/10 text-indigo-900 dark:text-indigo-100 shadow-sm" 
                              : "border-zinc-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 hover:border-indigo-300 dark:hover:border-indigo-700 hover:bg-zinc-50 dark:hover:bg-zinc-800"
                          )}
                        >
                          <span className="text-[15px] leading-relaxed">{option}</span>
                          <div className={cn(
                            "w-6 h-6 rounded-full border-2 flex items-center justify-center shrink-0",
                            isSelected ? "border-indigo-500 bg-indigo-500" : "border-zinc-300 dark:border-zinc-600"
                          )}>
                            {isSelected && <div className="w-2.5 h-2.5 bg-white rounded-full"></div>}
                          </div>
                        </button>
                      );
                    }))}
                  </div>

                  <div className="p-6 bg-white dark:bg-zinc-950 flex justify-end">
                    <button
                      onClick={handleNext}
                      disabled={!isAnswered || submitMutation.isPending}
                      className="bg-indigo-600 text-white px-8 py-3 rounded-lg font-medium hover:bg-indigo-700 transition-colors disabled:opacity-50 flex items-center gap-2"
                    >
                      {submitMutation.isPending ? (
                        <><Loader2 className="w-4 h-4 animate-spin" /> Submitting...</>
                      ) : currentQuestionIdx === activeQuiz.questions.length - 1 ? (
                        "Submit Quiz"
                      ) : (
                        <>Next Question <ArrowRight className="w-4 h-4" /></>
                      )}
                    </button>
                  </div>
                </motion.div>
              </AnimatePresence>
            </div>
          </div>
          
          {showText && (
            <motion.div 
              initial={{ opacity: 0, width: 0 }}
              animate={{ opacity: 1, width: "50%" }}
              className="hidden lg:flex flex-col h-full bg-white dark:bg-zinc-950 rounded-2xl border border-zinc-200 dark:border-zinc-800 shadow-sm"
            >
              <div className="p-4 border-b border-zinc-200 dark:border-zinc-800 bg-zinc-50 dark:bg-zinc-900/50 rounded-t-2xl">
                <h3 className="font-bold text-lg flex items-center gap-2">
                  <FileText className="w-5 h-5 text-indigo-500" />
                  Source Text
                </h3>
              </div>
              <div className="flex-1 overflow-y-auto p-6 custom-scrollbar">
                {isLoadingText ? (
                  <div className="flex h-full items-center justify-center">
                    <Loader2 className="w-8 h-8 animate-spin text-indigo-500" />
                  </div>
                ) : (
                  <div className="prose prose-sm dark:prose-invert max-w-none">
                    <pre className="whitespace-pre-wrap font-sans text-[15px] leading-relaxed text-zinc-700 dark:text-zinc-300 bg-transparent border-0 p-0 m-0">
                      {documentText}
                    </pre>
                  </div>
                )}
              </div>
            </motion.div>
          )}
        </div>
      </PageWrapper>
    );
  }

  return (
    <PageWrapper className="flex flex-col items-center justify-center min-h-screen text-center max-w-lg mx-auto w-full">
      <div className="w-20 h-20 bg-indigo-50 dark:bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 rounded-2xl flex items-center justify-center mb-6 shadow-sm">
        <BrainCircuit className="w-10 h-10" />
      </div>
      <h1 className="text-3xl font-bold mb-4">Quiz Center</h1>
      <p className="text-zinc-500 mb-8 leading-relaxed">
        Test your knowledge and strengthen your memory retention. 
        We'll generate a personalized quiz based on the concepts extracted from this document.
      </p>
      
      <button 
        onClick={handleStartQuiz}
        disabled={generateMutation.isPending}
        className="bg-indigo-600 text-white px-8 py-3.5 rounded-full font-medium hover:bg-indigo-700 transition-colors shadow-lg shadow-indigo-500/20 flex items-center gap-3 text-lg disabled:opacity-70"
      >
        {generateMutation.isPending ? (
          <><Loader2 className="w-6 h-6 animate-spin" /> Generating Quiz...</>
        ) : (
          <><PlayCircle className="w-6 h-6" /> Start New Quiz</>
        )}
      </button>

      {generateMutation.isError && (
        <p className="mt-4 text-rose-500 font-medium max-w-md mx-auto">
          {generateMutation.error instanceof Error && (generateMutation.error as any).response?.data?.message
            ? (generateMutation.error as any).response.data.message
            : "Failed to generate quiz. The AI service may be temporarily unavailable."}
        </p>
      )}
    </PageWrapper>
  );
}
