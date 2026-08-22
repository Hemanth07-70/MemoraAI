# MemoraAI

An AI-powered learning platform that converts PDFs into an intelligent study system. Upload a document, chat with it using RAG, take adaptive quizzes, and watch a personalized memory engine track which concepts you know and which you need to review.

---

## Table of Contents

- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Algorithms & Data Models](#algorithms--data-models)
- [API Reference](#api-reference)
- [Local Setup](#local-setup)
- [Environment Variables](#environment-variables)

---

## Architecture

```
Browser
  │
  ▼
Frontend (React 19 + Vite 8)          :5173
  │  REST / JWT
  ▼
Backend (Spring Boot 3 / Java 17)     :8080
  │  internal HTTP                  │  PostgreSQL + pgvector
  ▼                                 ▼
AI Service (FastAPI / Python 3.11)   :8000      Postgres :5432
```

Four Docker containers communicate over an internal bridge network. The browser only talks to the backend; the backend calls the AI service for PDF extraction and embedding generation.

---

## Tech Stack

### Frontend
| Library | Version | Role |
|---------|---------|------|
| React | 19 | UI framework |
| Vite | 8 | Dev server & bundler |
| TypeScript | 6 | Type safety |
| TanStack Query | 5 | Server state & caching |
| Axios | 1.18 | HTTP client |
| React Router | 7 | Client-side routing |
| Tailwind CSS | 4 | Styling |
| Framer Motion | 12 | Animations |
| Recharts | 3 | Memory score charts |
| D3-force | 3 | Knowledge graph layout |
| react-zoom-pan-pinch | 4 | Graph pan/zoom |
| react-markdown | 10 | Render LLM answers |

### Backend
| Library | Version | Role |
|---------|---------|------|
| Spring Boot | 3 | Application framework |
| Java | 17 | Runtime |
| Spring Security | 6 | JWT auth |
| Spring Data JPA | 3 | ORM |
| Spring WebFlux | 3 | Reactive HTTP client (LLM calls) |
| PostgreSQL driver | - | DB connectivity |
| jjwt | 0.12 | JWT generation & validation |
| Lombok | - | Boilerplate reduction |
| MapStruct | - | Entity ↔ DTO mapping |
| SpringDoc OpenAPI | 2 | Swagger UI at `/swagger-ui.html` |
| Spring Actuator | 3 | Health endpoint |

### AI Service
| Library | Version | Role |
|---------|---------|------|
| FastAPI | 0.111 | REST framework |
| Uvicorn | 0.30 | ASGI server |
| sentence-transformers | 3.0.1 | Local embedding model (all-MiniLM-L6-v2) |
| PyTorch (CPU) | 2.3.1 | sentence-transformers runtime |
| PyMuPDF | 1.24.5 | PDF text extraction |

### Database
| Component | Version | Role |
|-----------|---------|------|
| PostgreSQL | 16 | Relational store |
| pgvector extension | pg16 | HNSW vector index for similarity search |

### LLM
| Model | Provider | Used for |
|-------|----------|---------|
| nvidia/nemotron-3-super-120b-a12b | NVIDIA API | Chat answers, quiz generation, concept extraction, KG relationships |

---

## Algorithms & Data Models

### End-to-End Data Flow

```
PDF Upload
    │
    ├─ 1. Store file to disk (/app/storage/uploads/documents)
    ├─ 2. Create ProcessingJob (status=PENDING)
    ├─ 3. POST /api/v1/process → AI Service (PyMuPDF extraction)
    ├─ 4. Save ExtractedDocument (raw text, page count, word count)
    ├─ 5. Chunk text → DocumentChunk[] (sliding window)
    ├─ 6. For each chunk: POST /api/v1/embeddings → AI Service → save DocumentEmbedding
    ├─ 7. Extract Concepts from chunks via LLM (parallel batches of 5)
    ├─ 8. Build KnowledgeGraph: extract ConceptRelationship[] via LLM
    ├─ 9. Initialize UserMemoryState per Concept (score = 0.50)
    └─ Document status → PROCESSED
```

---

### 1. PDF Extraction

Library: **PyMuPDF (fitz)**

- Opens PDF pages one by one
- Extracts text blocks, strips headers/footers via position heuristics
- Returns: `text`, `page_count`, `word_count`, `character_count`

---

### 2. Text Chunking

Algorithm: **Sliding Window with Sentence-Boundary Awareness**

```
chunk_size  = 1000 characters (configurable via CHUNKING_CHUNK_SIZE)
overlap     = 200 characters  (configurable via CHUNKING_OVERLAP)
```

Split logic:
1. Find the ideal split point at `current + chunk_size`
2. Walk backwards up to `overlap` characters looking for `.` `?` `!` `\n` to split at a sentence boundary
3. If no boundary found, split at word boundary (last space)
4. Guarantee minimum forward progress = `max(1, (chunk_size - overlap) / 2)` to prevent infinite loops
5. Persist each chunk with `start_offset`, `end_offset`, `word_count`, `character_count`

**Entity: `document_chunks`**

| Column | Type | Description |
|--------|------|-------------|
| id | UUID PK | |
| extracted_document_id | UUID FK | |
| chunk_index | INT | Order within document |
| chunk_text | TEXT | The chunk content |
| start_offset | INT | Byte offset in full text |
| end_offset | INT | Byte offset end |
| character_count | INT | |
| word_count | INT | |
| created_at | TIMESTAMP | |

---

### 3. Embedding Model

Model: **all-MiniLM-L6-v2** (sentence-transformers, local)

- Produces 384-dimensional dense vectors
- Runs entirely inside the AI Service container — no external API needed
- First container start downloads the model (~90 MB, cached in `model_cache` Docker volume)
- Embeddings stored as JSON in PostgreSQL; pgvector loads them for HNSW indexing

**Entity: `document_embeddings`**

| Column | Type | Description |
|--------|------|-------------|
| id | UUID PK | |
| chunk_id | UUID FK | Links to document_chunks |
| dimension | INT | Always 384 |
| embedding_json | TEXT | JSON array of 384 floats |
| generated_at | TIMESTAMP | |
| generation_time_ms | BIGINT | Latency tracking |

---

### 4. RAG Pipeline (3-Layer Retrieval)

Triggered on every `/api/v1/chat/ask` request.

#### Layer 1 — pgvector HNSW Similarity Search

- Query text is embedded with the same all-MiniLM-L6-v2 model
- pgvector `<=>` (cosine distance) operator retrieves top-K chunks
- HNSW index gives O(log n) approximate nearest-neighbour at query time
- Default top-K = 5 (overridable per request)

#### Layer 2 — Knowledge Graph Query Expansion

Before embedding the query, the backend:
1. Scans all concepts for the document whose `normalized_name` appears in the query string
2. **Match found**: walks 1-hop in the KG, appends neighbour concept names as `[context: ...]`
3. **No match**: appends the top-5 highest-importance concepts as context hints

This enriches the query embedding so it captures related concepts even when the user didn't name them explicitly.

#### Layer 3 — ANME Memory-Weighted Re-ranking

After vector retrieval, chunks are re-scored:

```
memory score 0.30–0.70  →  chunk score × 1.30   (learning zone — prioritised)
memory score > 0.70     →  chunk score × 0.85   (mastered — demoted slightly)
memory score < 0.30     →  chunk score × 1.00   (unseen — neutral)
no memory state         →  chunk score × 1.00   (neutral)
```

The multiplier is the max across all concepts that appear in the chunk text. Re-ranked list is re-sorted descending.

#### Hallucination Guard

If the top chunk's score after re-ranking is below `AI_HALLUCINATION_THRESHOLD` (default 0.30), the backend refuses to call the LLM and returns:

> "I couldn't find this information in the uploaded documents."

---

### 5. Concept Extraction

Trigger: runs automatically after embedding completes for all chunks.

Process:
1. Group chunks into batches of 5
2. Fire up to 4 parallel LLM calls (respects NVIDIA rate limits)
3. Each call asks Nemotron to return JSON: `[{name, description, importance_score (0–1), difficulty_score (0–1)}]`
4. Aggregate results: merge duplicates by `normalized_name` (lowercase, trimmed), summing frequency, averaging scores
5. Link each concept to its most representative chunk embedding

**Entity: `concepts`**

| Column | Type | Description |
|--------|------|-------------|
| id | UUID PK | |
| document_id | UUID FK | |
| name | VARCHAR | Original casing |
| normalized_name | VARCHAR | Lowercase, trimmed — unique per document |
| description | TEXT | LLM-generated definition |
| importance_score | DOUBLE | 0–1, how central to the document |
| difficulty_score | DOUBLE | 0–1, how hard to learn |
| frequency | INT | How many chunks mention it |
| embedding_id | UUID FK | Links to representative embedding |
| created_at | TIMESTAMP | |

---

### 6. Knowledge Graph

Trigger: runs after concept extraction, requires ≥ 2 concepts.

Process:
1. Select top-30 concepts by importance_score (prevents LLM context overflow)
2. Send concept names + descriptions to Nemotron
3. Nemotron returns JSON edges: `[{source, target, type, confidence}]`
4. Validate: both concepts must exist, type must be a known `RelationshipType`
5. Compute final `confidence_score` as weighted blend:
   - 40% LLM confidence
   - 20% cosine similarity of concept embeddings
   - 20% co-occurrence score
   - 20% frequency score

Relationship types: `PREREQUISITE | RELATED | DEPENDS_ON | PART_OF | IMPLEMENTS | USES | EXTENDS | SIMILAR`

**Entity: `concept_relationships`**

| Column | Type | Description |
|--------|------|-------------|
| id | UUID PK | |
| source_concept_id | UUID FK | |
| target_concept_id | UUID FK | |
| relationship_type | ENUM | See above |
| confidence_score | DOUBLE | 0–1 |

---

### 7. ANME Memory System

**ANME = Active Neural Memory Engine**

A deterministic spaced-repetition system tied to quiz performance.

#### Initialization

When a document finishes processing, one `UserMemoryState` row is created per concept per user, with `memory_score = 0.50`.

#### Memory Score Update (after quiz submission)

```
quiz score ≥ 80%  →  memory_score += 0.20
quiz score ≥ 60%  →  memory_score += 0.10
quiz score <  60%  →  memory_score -= 0.20
```

Score is clamped to `[0.0, 1.0]`.

#### Spaced Repetition Schedule

```
memory_score ≥ 0.80  →  next review in 7 days
memory_score ≥ 0.60  →  next review in 3 days
memory_score <  0.60  →  next review in 1 day
```

**Entity: `user_memory_states`**

| Column | Type | Description |
|--------|------|-------------|
| id | UUID PK | |
| user_id | UUID FK | |
| concept_id | UUID FK | |
| memory_score | DOUBLE | 0.0–1.0, starts at 0.50 |
| review_count | INT | Total quiz submissions for this concept |
| last_reviewed_at | TIMESTAMP | |
| next_review_at | TIMESTAMP | Spaced repetition target |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

---

### 8. Quiz Generation

1. Load concepts for the document with low or medium memory scores (prioritise weak areas)
2. Sample up to 5 chunks from the document for context
3. Prompt Nemotron: generate N multiple-choice questions (default 10, max 20) in JSON
4. Parse and persist `Quiz` + `QuizQuestion[]`
5. On submission: grade answers, compute percentage, update `UserMemoryState` via ANME, persist `QuizAttempt`

---

### Other Entities

**`users`** — `id`, `first_name`, `last_name`, `email`, `password_hash`, `role (STUDENT|ADMIN)`, `enabled`, `email_verified`, `created_at`

**`documents`** — `id`, `owner_id`, `file_name`, `original_file_name`, `mime_type`, `extension`, `size`, `storage_path`, `status (UPLOADED|PROCESSING|PROCESSED|FAILED)`, `is_deleted`, `uploaded_at`

**`extracted_documents`** — `id`, `document_id`, `extracted_text`, `page_count`, `word_count`, `character_count`

**`processing_jobs`** — `id`, `document_id`, `job_type (OCR|EMBEDDING|CONCEPT_EXTRACTION)`, `status (PENDING|PROCESSING|COMPLETED|FAILED)`, `progress`, `error`, `created_at`, `completed_at`

**`conversations`** — `id`, `user_id`, `document_id`, `title`, `created_at`

**`chat_messages`** — `id`, `conversation_id`, `role (USER|AI)`, `content`, `created_at`

**`quizzes`** — `id`, `document_id`, `user_id`, `status (ACTIVE|COMPLETED)`, `question_count`, `created_at`

**`quiz_questions`** — `id`, `quiz_id`, `question_text`, `option_a/b/c/d`, `correct_answer`, `explanation`, `type`

**`quiz_attempts`** — `id`, `quiz_id`, `user_id`, `score`, `percentage`, `started_at`, `completed_at`

**`learning_events`** — `id`, `user_id`, `concept_id`, `event_type`, `payload`, `created_at`

---

## API Reference

All endpoints return `{ success, message, data }`. Auth endpoints excluded from JWT requirement.

### Auth
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login, returns JWT |

### Documents
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/documents/upload` | Upload PDF (multipart/form-data) |
| GET | `/api/v1/documents` | List user's documents |
| GET | `/api/v1/documents/{id}` | Get document metadata |
| DELETE | `/api/v1/documents/{id}` | Soft-delete document |

### Chat
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/chat/ask` | RAG query (3-layer retrieval + LLM) |

### Conversations
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/conversations` | Create conversation |
| GET | `/api/v1/conversations` | List conversations |
| GET | `/api/v1/conversations/{id}/messages` | Get chat history |

### Quiz
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/quiz/generate` | Generate adaptive quiz for document |
| GET | `/api/v1/quiz/{id}` | Get quiz questions |
| POST | `/api/v1/quiz/{id}/submit` | Submit answers, grade, update memory |

### Memory (ANME)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/memory/me` | All UserMemoryStates for current user |
| GET | `/api/v1/anme/knowledge-graph/{docId}` | Concept graph for document |
| GET | `/api/v1/revision/plan` | Concepts due for review today |

### Internal (Backend → AI Service)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/process` | PDF text extraction (PyMuPDF) |
| POST | `/api/v1/embeddings` | Generate 384-dim embedding for a chunk |
| GET | `/` | AI service health |

---

## Local Setup

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (includes Docker Compose)
- A free NVIDIA API key from [build.nvidia.com](https://build.nvidia.com)

### Steps

```bash
# 1. Clone
git clone https://github.com/Hemanth07-70/MemoraAI.git
cd MemoraAI

# 2. Create .env
echo "NEMOTRON_API_KEY=nvapi-your-key-here" > .env

# 3. Build and start (first time: ~10 min — compiles Spring Boot, installs Python deps, downloads embedding model)
docker compose up --build

# 4. Open the app
open http://localhost:5173
```

After the first build, subsequent starts take ~30 seconds:

```bash
docker compose up
```

### What runs where

| Service | URL | Notes |
|---------|-----|-------|
| Frontend | http://localhost:5173 | React + Vite dev server, hot reload |
| Backend | http://localhost:8080 | Spring Boot, Swagger at /swagger-ui.html |
| AI Service | http://localhost:8000 | FastAPI, docs at /docs |
| PostgreSQL | localhost:5432 | pgvector/pgvector:pg16 |

### Useful commands

```bash
# Stop all services
docker compose down

# Wipe database and start fresh (deletes all data)
docker compose down -v

# View logs for a specific service
docker compose logs -f backend
docker compose logs -f ai-service

# Rebuild only one service
docker compose up --build ai-service

# Restart just the frontend
docker compose restart frontend
```

### Switching the LLM to Ollama (fully offline)

The backend has a built-in Ollama integration. To use it instead of Nemotron:

1. Add Ollama to `docker-compose.yml`:

```yaml
  ollama:
    image: ollama/ollama
    container_name: memoraai-ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    networks:
      - memoraai-network
```

2. Change backend environment:

```yaml
LLM_PROVIDER: ollama
OLLAMA_URL: http://ollama:11434
```

3. Pull a model (after `docker compose up`):

```bash
docker exec memoraai-ollama ollama pull llama3
```

---

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `NEMOTRON_API_KEY` | Yes (if LLM_PROVIDER=nemotron) | — | NVIDIA API key |
| `LLM_PROVIDER` | No | `nemotron` | `nemotron` or `ollama` |
| `OLLAMA_URL` | No | `http://localhost:11434` | Ollama base URL (when using ollama provider) |
| `AI_HALLUCINATION_THRESHOLD` | No | `0.30` | Min cosine score to answer; below this returns "not found" |
| `CHUNKING_CHUNK_SIZE` | No | `1000` | Characters per chunk |
| `CHUNKING_OVERLAP` | No | `200` | Overlap between consecutive chunks |
| `JWT_SECRET` | No | default value | Override for production |
| `SENTENCE_TRANSFORMERS_HOME` | No | `/app/models` | Cache dir for embedding model |
