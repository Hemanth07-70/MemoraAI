# MemoraAI

AI-powered adaptive learning platform that turns PDFs into personalized knowledge. Upload a document, ask questions, take quizzes, and watch the system adapt retrieval and spaced repetition to your individual learning state.

---

## Live Demo

| Service | URL |
|---|---|
| Frontend | https://memoraai-frontend.vercel.app |
| Backend API | https://memoraai-backend-7cru.onrender.com |
| AI Service | https://memoraai-ai-service-0uyz.onrender.com |

---

## Quick Start (Local)

**Prerequisites:** Docker Desktop, a free NVIDIA API key from [build.nvidia.com](https://build.nvidia.com)

```bash
git clone https://github.com/Hemanth07-70/MemoraAI.git
cd MemoraAI

# Create .env — only one key needed
echo "NEMOTRON_API_KEY=nvapi-your-key-here" > .env

# First build (~10-20 min: downloads PyTorch CPU + sentence-transformers, compiles Spring Boot)
docker compose up --build

# Open the app
open http://localhost:5173
```

After first build, subsequent starts take ~60s: `docker compose up`

| Service | URL |
|---|---|
| Frontend | http://localhost:5173 |
| Backend | http://localhost:8080 |
| AI Service | http://localhost:8000 |
| PostgreSQL | localhost:5432 |

```bash
# Verify everything is up
curl http://localhost:8080/api/health   # {"status":"UP"}
curl http://localhost:8000/             # {"status":"UP","service":"ai-service"}
```

---

## Architecture

```
Browser (React 19)
    │  JWT + REST
    ▼
Spring Boot 3.3.1 (Java 17)  ──────────────────  PostgreSQL 16 + pgvector
    │                                              (documents, chunks, embeddings,
    │                                               concepts, memory states, quizzes)
    ├── POST /api/v1/process ──────────────────▶  FastAPI 0.111 (Python 3.11)
    │                                              ├─ PyMuPDF  — PDF extraction
    │                                              └─ sentence-transformers (local CPU)
    │                                                 all-MiniLM-L6-v2  →  384-dim
    │
    └── NVIDIA Nemotron API  (nvidia/nemotron-3-super-120b-a12b)
        ├─ Concept extraction
        ├─ Knowledge graph relationship extraction
        ├─ Document intelligence (summary, skills, keywords)
        ├─ Quiz generation
        └─ RAG answer generation
```

---

## Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Frontend | React, TypeScript, Vite | React 19, TS ~6, Vite 8 |
| Styling | Tailwind CSS, Framer Motion | v4, v12 |
| State / Data | TanStack Query, axios | v5, v1.18 |
| Graph viz | d3-force, recharts | v3 each |
| Backend | Spring Boot, Java 17, Maven | 3.3.1 |
| Security | Spring Security, JWT (jjwt) | — |
| HTTP client | Spring WebFlux WebClient | reactive |
| AI Service | FastAPI, Uvicorn, Python 3.11 | 0.111, 0.30 |
| PDF extraction | PyMuPDF (fitz) | 1.24.5 |
| Embeddings | sentence-transformers (local CPU) | 3.0.1 |
| Embedding model | all-MiniLM-L6-v2 | 384 dimensions |
| PyTorch | CPU-only | 2.3.1+cpu |
| LLM | NVIDIA Nemotron | nvidia/nemotron-3-super-120b-a12b |
| Database | PostgreSQL 16 + pgvector | pgvector:pg16 |
| Containerization | Docker Compose | — |
| CI/CD | GitHub Actions | — |

---

## Document Processing Pipeline

Every uploaded PDF triggers a fully-automated 5-stage pipeline. A Spring `@Scheduled` task polls for `PENDING` jobs every 5 seconds.

```
Upload PDF
    │
    ▼  Stage 1 — OCR
    PyMuPDF fitz.open() → page-by-page text extraction
    → ExtractedDocument (fullText, pageCount, wordCount, characterCount)
    │
    ▼  Stage 2 — EMBEDDING
    Sliding-window chunking (1000 chars, 200 overlap, word-boundary aligned)
    → For each DocumentChunk:
        sentence-transformers.encode(chunkText)  →  384-dim float[]
        Stored: embeddingJson TEXT  +  embedding_vector vector(384) via native SQL cast
    │
    ├───────────────────────────────────────────────────────────┐
    ▼  Stage 3a — INTELLIGENCE                                  ▼  Stage 3b — CONCEPT_EXTRACTION
    Full text (≤50k chars) → Nemotron                          Batches of 5 chunks → Nemotron
    → {executiveSummary, skills, technologies,                  Up to 4 concurrent LLM threads
       organizations, education, projects, keywords}            → per-concept {name, description,
    → DocumentIntelligence row saved                              importanceScore, difficultyScore}
                                                               → top 30 concepts persisted
                                                                    │
                                                                    ▼  Stage 4 — KNOWLEDGE_GRAPH
                                                                    Top 30 concepts → Nemotron
                                                                    → relationships {source, target,
                                                                      type, llmConfidence}
                                                                    → ConceptRelationship rows saved
                                                                    → UserMemoryState per concept
                                                                      (memoryScore = 0.50)
    │
    ▼  Document.status → READY
```

**Typical time:** 3–8 minutes for a 20–50 page PDF.

### Chunking Algorithm

Split priority (DocumentChunkingService):
1. Paragraph boundary `\n\n`
2. Single newline `\n`
3. Sentence end `. ` `! ` `? `
4. Whitespace
5. Hard cut at `chunkSize`

Next chunk start = `splitPoint - overlap`, adjusted to word boundary. Strict forward-progress guarantee prevents infinite loops.

### Concept Importance Score

```
importanceScore = 0.40 × frequencyScore
                + 0.30 × headingBoost        (1.0 if in filename, 0.5 if in first chunk)
                + 0.20 × avgLlmImportance
                + 0.10 × embeddingCentrality
clamped to [0.0, 1.0], top 30 kept
```

### Concept Extraction Parallelization

```java
int parallelism = Math.min(4, batches.size());
ExecutorService executor = Executors.newFixedThreadPool(parallelism);
// Sequential: ~880s for 29-chunk doc → Parallel: ~176s  (5× speedup)
```

---

## Retrieval Architecture — 3-Layer Adaptive RAG

### Layer 1 — pgvector HNSW (O(log n))

```sql
-- Retrieval query (CosineSimilarityService)
SELECT d.id, dc.id, dc.chunk_index, dc.chunk_text,
       (1 - (de.embedding_vector <=> CAST(:queryVector AS vector))) AS score
FROM document_embeddings de
JOIN document_chunks dc  ON de.chunk_id = dc.id
JOIN extracted_documents ed ON dc.extracted_document_id = ed.id
JOIN documents d ON ed.document_id = d.id
WHERE d.owner_id = :userId
  AND d.id = :documentId
  AND d.is_deleted = false
ORDER BY de.embedding_vector <=> CAST(:queryVector AS vector)
LIMIT :topK
```

### Layer 2 — Knowledge Graph Query Expansion

```
Input:  "explain attention mechanism"

1. Match query tokens against concept normalizedNames
   → "attention" → Concept: Self-Attention (matched)

2. Walk 1-hop ConceptRelationship neighbours
   → Transformer, Multi-Head Attention, BERT, Encoder-Decoder

3. Expand:
   "explain attention mechanism [context: Transformer, Multi-Head Attention, BERT, ...]"

4. Re-embed expanded string → richer 384-dim vector → better recall

Fallback (no match): top-5 concepts by importanceScore appended instead.
```

### Layer 3 — ANME Memory-Weighted Re-ranking

| User's memory score for concept in chunk | Multiplier | Rationale |
|---|---|---|
| 0.30 – 0.70 | ×1.30 | Active learning zone — boost |
| > 0.70 | ×0.85 | Mastered — slight demotion |
| < 0.30 | ×1.00 | Not yet seen — neutral |

Two users asking the same question get different context passed to the LLM.

### Hallucination Guard

If the top chunk's adjusted score < `AI_HALLUCINATION_THRESHOLD` (default 0.30), returns `"I couldn't find this information in the uploaded documents."` — no LLM call made.

---

## ANME — Adaptive Neural Memory Enhancement

Each `(user, concept)` pair has a `UserMemoryState` row, initialized with `memoryScore = 0.50`.

### Memory Update After Quiz

```
quiz percentage ≥ 80%  →  memoryScore += 0.20
quiz percentage ≥ 60%  →  memoryScore += 0.10
quiz percentage  < 60%  →  memoryScore -= 0.20
always clamped to [0.0, 1.0]
```

Per-concept percentage is tracked separately (not just overall quiz score).

### Spaced Repetition Schedule

```
memoryScore ≥ 0.80  →  nextReviewAt = now + 7 days
memoryScore ≥ 0.60  →  nextReviewAt = now + 3 days
memoryScore  < 0.60  →  nextReviewAt = now + 1 day
```

### Revision Priority Formula

```
priority = 0.50 × (1 - memoryScore)    // urgency
         + 0.30 × importanceScore       // concept value
         + 0.20 × difficultyScore       // inherent hardness
clamped to [0.0, 1.0], sorted descending
```

---

## Knowledge Graph

### Relationship Types
`PREREQUISITE`, `RELATED`, `DEPENDS_ON`, `PART_OF`, `IMPLEMENTS`, `USES`, `EXTENDS`, `SIMILAR`

### Relationship Confidence Score

```
confidence = 0.40 × llmConfidence
           + 0.20 × cosineSimilarity(embedding_A, embedding_B)
           + 0.20 × coOccurrenceScore
           + 0.20 × frequencyScore
clamped to [0.0, 1.0]
```

---

## Database Schema

| Table | Key Columns |
|---|---|
| `users` | id UUID, firstName, lastName, email (unique), password (BCrypt), role (STUDENT/ADMIN) |
| `documents` | id, owner_id→users, fileName, originalFileName, size, storagePath, status (UPLOADED/PROCESSING/READY/FAILED), isDeleted |
| `extracted_documents` | id, document_id, extractedText TEXT, pageCount, wordCount, characterCount |
| `document_chunks` | id, extracted_document_id, chunkIndex, chunkText TEXT, startOffset, endOffset, characterCount, wordCount |
| `document_embeddings` | id, chunk_id (unique), dimension (384), embeddingJson TEXT, **embedding_vector vector(384)**, modelName, generatedAt |
| `processing_jobs` | id, document_id, jobType (OCR/EMBEDDING/CONCEPT_EXTRACTION/INTELLIGENCE/KNOWLEDGE_GRAPH), status (PENDING/PROCESSING/COMPLETED/FAILED), progress 0-100 |
| `concepts` | id, document_id, name, normalizedName (unique per doc), description TEXT, importanceScore, difficultyScore, frequency |
| `concept_relationships` | id, source_concept_id, target_concept_id, relationshipType, confidenceScore |
| `user_memory_states` | id, user_id, concept_id (unique pair), memoryScore (default 0.5), reviewCount, lastReviewedAt, nextReviewAt |
| `quizzes` | id, document_id, user_id, title, questionCount, status (PENDING/COMPLETED) |
| `quiz_questions` | id, quiz_id, concept_id, questionType (MULTIPLE_CHOICE/TRUE_FALSE/FILL_BLANK), questionText, options[], correctAnswer, difficulty |
| `quiz_attempts` | id, quiz_id, user_id, answers (map), score, correctAnswers, wrongAnswers, percentage, completedAt |
| `conversations` | id, user_id, title, createdAt |
| `chat_messages` | id, conversation_id, role (USER/AI), content TEXT, timestamp |
| `document_intelligence` | id, document_id, executiveSummary, skills[], technologies[], organizations[], education[], projects[], keywords[] |

---

## API Reference

### Auth
| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/api/v1/auth/register` | `{firstName, lastName, email, password}` | `{token, user}` |
| POST | `/api/v1/auth/login` | `{email, password}` | `{token, user}` |

### Documents
| Method | Path | Notes |
|---|---|---|
| POST | `/api/v1/documents/upload` | multipart `file`, PDF only, max 50 MB |
| GET | `/api/v1/documents` | list user's documents |
| GET | `/api/v1/documents/{id}` | single document |
| GET | `/api/v1/documents/{id}/text` | full extracted text |
| GET | `/api/v1/documents/{id}/download` | original file |
| DELETE | `/api/v1/documents/{id}` | soft delete |

### Chat
| Method | Path | Body |
|---|---|---|
| POST | `/api/v1/chat/ask` | `{question, documentId?, conversationId?, topK?}` |

Response: `{answer, provider, model, sources[], retrievalTimeMs, generationTimeMs, totalTimeMs}`

### Memory & Revision
| Method | Path |
|---|---|
| GET | `/api/v1/memory/me` |
| GET | `/api/v1/memory/document/{documentId}` |
| GET | `/api/v1/revision/today` |

### Quiz
| Method | Path | Notes |
|---|---|---|
| POST | `/api/v1/documents/{docId}/quiz` | `{questionCount}` |
| GET | `/api/v1/quizzes/{quizId}` | correct answers not exposed |
| POST | `/api/v1/quizzes/{quizId}/submit` | `{answers:[{questionId, userAnswer}]}` |

### Knowledge Graph
| Method | Path |
|---|---|
| GET | `/api/v1/documents/{documentId}/knowledge-graph` |
| GET | `/api/v1/documents/{documentId}/concepts` |
| GET | `/api/v1/concepts/{conceptId}/related` |
| GET | `/api/v1/documents/{documentId}/intelligence` |

### Conversations
| Method | Path |
|---|---|
| GET | `/api/v1/conversations` |
| POST | `/api/v1/conversations` |
| GET | `/api/v1/conversations/{id}/messages` |
| DELETE | `/api/v1/conversations/{id}` |

### System
| Method | Path |
|---|---|
| GET | `/api/health` |
| GET | `/api/v1/processing/jobs` |

### AI Service (port 8000)
| Method | Path | Body |
|---|---|---|
| GET | `/` | — |
| POST | `/api/v1/process` | `{jobId, documentId, jobType, filePath?, fileContent?}` |
| POST | `/api/v1/embeddings` | `{chunkId, text}` → `{embedding[384]}` |

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `NEMOTRON_API_KEY` | — | **Required.** NVIDIA API key |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/memoraai` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `memoraai` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `memoraai_secret` | DB password |
| `AI_SERVICE_URL` | `http://localhost:8000` | FastAPI service URL |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,...` | Comma-separated allowed origins |
| `JWT_SECRET` | hardcoded (dev) | JWT signing key — set in production |
| `AI_HALLUCINATION_THRESHOLD` | `0.30` | Min cosine score to answer |
| `LLM_PROVIDER` | `nemotron` | `nemotron` or `ollama` |
| `SENTENCE_TRANSFORMERS_HOME` | `/app/models` | Model cache directory (ai-service) |

---

## Running Without Docker

**PostgreSQL** — must have pgvector extension installed:
```bash
psql -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

**AI Service:**
```bash
cd ai-service
pip install -r requirements-docker.txt
# First run downloads all-MiniLM-L6-v2 (~90 MB)
uvicorn app.main:app --reload --port 8000
```

**Backend:**
```bash
cd backend
export NEMOTRON_API_KEY=nvapi-...
./mvnw spring-boot:run
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```

---

## Project Structure

```
MemoraAI/
├── frontend/                     React 19 + TypeScript SPA
│   └── src/
│       ├── pages/                Chat, Dashboard, Documents, KnowledgeGraph,
│       │                         Memory, QuizCenter, Revision, Profile, Settings
│       ├── services/             api.ts (axios instance), apiClient.ts (typed calls)
│       └── types/backend.ts      All DTO interfaces
│
├── backend/                      Spring Boot 3.3.1
│   └── src/main/java/com/memoraai/
│       ├── auth/                 JWT register + login
│       ├── document/             Upload, storage, soft-delete
│       ├── processing/           Pipeline scheduler (5s poll)
│       ├── chunking/             Sliding-window text chunker
│       ├── embedding/            Calls AI service, persists vectors
│       ├── search/               pgvector HNSW cosine search
│       ├── chat/                 3-layer RAG, Nemotron/Ollama
│       ├── anme/                 Memory states + KG service
│       ├── concept/              Parallel concept extraction
│       ├── quiz/                 Quiz generation + grading
│       ├── revision/             Spaced repetition planner
│       ├── conversation/         Chat history
│       └── documentintelligence/ Doc summary, skills, keywords
│
├── ai-service/                   FastAPI Python 3.11
│   └── app/
│       ├── api/                  process.py, embeddings.py
│       └── services/             pdf_extractor.py, embedding_service.py
│
├── docker-compose.yml            Local orchestration (4 services + volumes)
├── .env                          NEMOTRON_API_KEY (git-ignored)
└── .env.example                  Template
```

---

## License

Proprietary. All rights reserved.
