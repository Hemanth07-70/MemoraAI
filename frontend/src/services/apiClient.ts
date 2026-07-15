import api from './api';
import type { 
  ApiResponse, 
  Document, 
  KnowledgeGraphDto, 
  UserMemoryStateDto, 
  RevisionPlanDto, 
  QuizDto, 
  SubmitQuizRequest, 
  QuizResultDto,
  ChatRequest,
  ChatResponse,
  ConversationDto,
  ChatMessage
} from '../types';

export const documentsApi = {
  getAll: async () => {
    const res = await api.get<ApiResponse<{ documents: Document[], totalCount: number }>>('/api/v1/documents');
    return res.data.data.documents || [];
  },
  upload: async (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await api.post<ApiResponse<Document>>('/api/v1/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return res.data.data;
  },
  delete: async (id: string) => {
    await api.delete(`/api/v1/documents/${id}`);
  },
  getText: async (id: string) => {
    const res = await api.get<ApiResponse<string>>(`/api/v1/documents/${id}/text`);
    return res.data.data;
  }
};

export const knowledgeGraphApi = {
  getForDocument: async (documentId: string) => {
    const res = await api.get<ApiResponse<KnowledgeGraphDto>>(`/api/v1/documents/${documentId}/knowledge-graph`);
    return res.data.data;
  },
  getGlobal: async () => {
    try {
      const docs = await documentsApi.getAll();
      const readyDocs = docs.filter(d => d.status === 'READY');
      console.log("Global KG - Ready docs:", readyDocs);
      
      const graphs = await Promise.all(readyDocs.map(doc => knowledgeGraphApi.getForDocument(doc.id)));
      console.log("Global KG - Fetched graphs:", graphs);
      
      const allConcepts = graphs.flatMap(g => g.concepts || []);
      const allRelationships = graphs.flatMap(g => g.relationships || []);
      
      // Deduplicate concepts by ID just in case
      const uniqueConcepts = Array.from(new Map(allConcepts.map(c => [c.id, c])).values());
      const uniqueRelationships = Array.from(new Map(allRelationships.map(r => [r.id, r])).values());
      
      console.log("Global KG - Merged result:", { concepts: uniqueConcepts.length, relationships: uniqueRelationships.length });
      
      return {
        documentId: 'global',
        concepts: uniqueConcepts,
        relationships: uniqueRelationships
      } as KnowledgeGraphDto;
    } catch (e) {
      console.error("Global KG error:", e);
      throw e;
    }
  }
};

export const memoryApi = {
  getForUser: async () => {
    const res = await api.get<ApiResponse<UserMemoryStateDto[]>>('/api/v1/memory/me');
    return res.data.data;
  },
  getForDocument: async (documentId: string) => {
    const res = await api.get<ApiResponse<UserMemoryStateDto[]>>(`/api/v1/memory/document/${documentId}`);
    return res.data.data;
  }
};

export const revisionApi = {
  getToday: async () => {
    const res = await api.get<ApiResponse<RevisionPlanDto>>('/api/v1/revision/today');
    return res.data.data;
  }
};

export const quizApi = {
  generate: async (documentId: string, questionCount: number = 5) => {
    const res = await api.post<ApiResponse<QuizDto>>(`/api/v1/documents/${documentId}/quiz`, { questionCount });
    return res.data.data;
  },
  get: async (quizId: string) => {
    const res = await api.get<ApiResponse<QuizDto>>(`/api/v1/quizzes/${quizId}`);
    return res.data.data;
  },
  submit: async (quizId: string, request: SubmitQuizRequest) => {
    const res = await api.post<ApiResponse<QuizResultDto>>(`/api/v1/quizzes/${quizId}/submit`, request);
    return res.data.data;
  }
};

export const chatApi = {
  sendMessage: async (request: ChatRequest) => {
    // Uses the proxy or direct call
    const res = await api.post<ChatResponse>('/api/v1/chat/ask', request);
    return res.data;
  }
};

export const conversationsApi = {
  getAll: async () => {
    const res = await api.get<ApiResponse<ConversationDto[]>>('/api/v1/conversations');
    return res.data.data;
  },
  getMessages: async (id: string) => {
    // The backend ChatMessageDto matches ChatMessage mostly, but let's be careful.
    // The backend returns role as upper case "USER" or "AI", we may need to map it.
    const res = await api.get<ApiResponse<any[]>>(`/api/v1/conversations/${id}/messages`);
    return res.data.data.map(msg => ({
      id: msg.id,
      role: msg.role === 'USER' ? 'user' : 'assistant',
      content: msg.content,
      timestamp: msg.timestamp
    })) as ChatMessage[];
  },
  create: async (title: string) => {
    const res = await api.post<ApiResponse<ConversationDto>>('/api/v1/conversations', { title });
    return res.data.data;
  },
  delete: async (id: string) => {
    await api.delete(`/api/v1/conversations/${id}`);
  }
};
