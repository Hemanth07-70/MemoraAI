export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface Document {
  id: string;
  originalFileName: string;
  mimeType: string;
  extension: string;
  size: number;
  status: 'UPLOADED' | 'PROCESSING' | 'READY' | 'FAILED';
  downloadUrl?: string;
  uploadedAt: string;
}

export interface ConceptDto {
  id: string;
  documentId: string;
  name: string;
  description: string;
  importanceScore: number;
  difficultyScore: number;
}

export interface ConceptRelationshipDto {
  id: string;
  sourceConceptId: string;
  sourceConceptName: string;
  targetConceptId: string;
  targetConceptName: string;
  relationshipType: string;
  confidenceScore: number;
}

export interface KnowledgeGraphDto {
  documentId: string;
  concepts: ConceptDto[];
  relationships: ConceptRelationshipDto[];
}

export interface UserMemoryStateDto {
  id: string;
  userId: string;
  concept: ConceptDto;
  memoryScore: number;
  reviewCount: number;
  lastReviewedAt: string;
  nextReviewAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface RevisionConceptDto {
  conceptId: string;
  conceptName: string;
  documentId: string;
  memoryScore: number;
  importanceScore: number;
  difficultyScore: number;
  priorityScore: number;
}

export interface RevisionPlanDto {
  revisionDate: string;
  concepts: RevisionConceptDto[];
}

export interface QuizQuestionDto {
  id: string;
  questionType: 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'FILL_BLANK';
  questionText: string;
  options: string[];
  explanation: string;
}

export interface QuizDto {
  id: string;
  documentId: string;
  title: string;
  questionCount: number;
  status: 'PENDING' | 'COMPLETED';
  createdAt: string;
  questions: QuizQuestionDto[];
}

export interface AnswerDto {
  questionId: string;
  userAnswer: string;
}

export interface SubmitQuizRequest {
  answers: AnswerDto[];
}

export interface QuestionResultDto {
  questionId: string;
  questionText: string;
  userAnswer: string;
  correctAnswer: string;
  correct: boolean;
}

export interface QuizResultDto {
  score: number;
  correctAnswers: number;
  wrongAnswers: number;
  percentage: number;
  updatedMemory: UserMemoryStateDto[];
  questionResults: QuestionResultDto[];
}

export interface ChatMessage {
  id?: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp?: string;
}

export interface ChatRequest {
  documentId?: string;
  question: string;
  conversationId?: string;
}

export interface ChatResponse {
  answer: string;
  provider?: string;
  model?: string;
  sources?: any[];
  retrievalTimeMs?: number;
  generationTimeMs?: number;
  totalTimeMs?: number;
}

export interface ConversationDto {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
}
