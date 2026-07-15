# MemoraAI

> Multimodal AI-powered learning platform for students.

MemoraAI transforms how students learn by ingesting PDFs, presentations, lecture recordings, images, and handwritten notes — then generating personalized quizzes, knowledge graphs, and adaptive study plans powered by local AI.

---

### Current Status: Milestone 3 Complete

*   **Frontend**: React, TypeScript, Tailwind CSS, Lucide Icons, Vite
*   **Backend**: Spring Boot 3, Java 17, PostgreSQL, Maven
*   **AI Service**: FastAPI, Python 3.11, PyPDF2
*   **DevOps**: Docker Compose, multi-container networking
*   **Authentication**: Stateless JWT Authentication, BCrypt Password Hashing, Feature-Based Architecture

---

## Architecture

```mermaid
graph TB
    subgraph Client
        FE[Frontend<br/>React + TypeScript + Vite]
    end

    subgraph Services
        BE[Backend<br/>Spring Boot 3.x / Java 17]
        AI[AI Service<br/>FastAPI / Python 3.11]
    end

    subgraph Infrastructure
        PG[(PostgreSQL 16)]
        OL[Ollama<br/>Llama 3]
    end

    FE -->|REST API| BE
    FE -->|REST API| AI
    BE --> PG
    AI --> OL
```

---

## Technology Stack

| Layer          | Technology                                      |
|----------------|------------------------------------------------|
| Frontend       | React 18, TypeScript, Vite, TailwindCSS         |
| Backend        | Java 21, Spring Boot 3.x, Maven, Spring Web     |
| AI Service     | Python 3.11, FastAPI, Uvicorn, PyTorch, FAISS    |
| Database       | PostgreSQL 16                                    |
| LLM Runtime    | Ollama (Llama 3)                                 |
| Orchestration  | Docker Compose                                   |

---

## Folder Structure

```
MemoraAI/
├── frontend/               # React + TypeScript + Vite
│   └── src/
│       ├── pages/
│       ├── components/
│       ├── layouts/
│       ├── hooks/
│       ├── services/
│       ├── types/
│       └── utils/
├── backend/                # Spring Boot 3.x
│   └── src/main/java/com/memoraai/
│       ├── config/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── entity/
│       ├── dto/
│       ├── security/
│       ├── exception/
│       └── util/
├── ai-service/             # FastAPI
│   └── app/
│       ├── api/
│       ├── core/
│       ├── services/
│       ├── models/
│       └── utils/
├── docker/                 # Docker configurations
├── docs/                   # Documentation
├── scripts/                # Automation scripts
├── datasets/               # Training datasets
├── models/                 # ML model artifacts
├── research/               # Research notes
├── .github/workflows/      # CI/CD pipelines
├── docker-compose.yml
├── .gitignore
└── README.md
```

---

## Getting Started

### Prerequisites

- Docker & Docker Compose
- Node.js 20+ (for local frontend development)
- Java 21 (for local backend development)
- Python 3.11+ (for local AI service development)

### Docker (Recommended)

Start all services with a single command:

```bash
docker compose up --build
```

| Service     | URL                          |
|-------------|------------------------------|
| Frontend    | http://localhost:5173         |
| Backend     | http://localhost:8080         |
| AI Service  | http://localhost:8000         |
| PostgreSQL  | localhost:5432               |
| Ollama      | http://localhost:11434        |

### Health Checks

```bash
# Backend
curl http://localhost:8080/api/health

# AI Service
curl http://localhost:8000/
```

### Local Development

**Frontend**
```bash
cd frontend
npm install
npm run dev
```

**Backend**
```bash
cd backend
./mvnw spring-boot:run
```

**AI Service**
```bash
cd ai-service
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

---

## Deployment Guide

MemoraAI is fully configured for free cloud hosting using Vercel, Render, and Neon PostgreSQL.

### 1. Database (Neon PostgreSQL)
1. Create a free account on [Neon](https://neon.tech/).
2. Create a new PostgreSQL database.
3. Copy the connection string. It will look like `postgres://username:password@hostname/dbname?sslmode=require`.
4. Ensure you append `?sslmode=require` if it's not already there.

### 2. AI Service (Render)
1. Connect your GitHub repository to [Render](https://render.com/).
2. Create a new **Web Service** from the repository.
3. Select `ai-service` as the Root Directory.
4. Render will automatically detect the Dockerfile.
5. Add the following Environment Variables (from `ai-service/.env.example`):
   - `BACKEND_URL`: URL of your backend (can be added after deploying the backend)
   - `LLM_API_KEY`: Your Nvidia Nemotron API key
   - `MODEL_NAME`: `nvidia/nemotron-3-super-120b-a12b`
6. Deploy the service.

### 3. Backend (Render)
1. Create a new **Web Service** from the repository on Render.
2. Select `backend` as the Root Directory.
3. Render will automatically detect the Dockerfile.
4. Add the following Environment Variables (from `backend/.env.example`):
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `SPRING_DATASOURCE_URL`: `jdbc:postgresql://<NEON_HOSTNAME>/<DB_NAME>?sslmode=require` (Replace `postgres://` with `jdbc:postgresql://`)
   - `SPRING_DATASOURCE_USERNAME`: Your Neon username
   - `SPRING_DATASOURCE_PASSWORD`: Your Neon password
   - `JWT_SECRET`: A secure random string
   - `JWT_EXPIRATION`: `86400000`
   - `AI_SERVICE_URL`: The URL of your deployed AI Service
   - `CORS_ALLOWED_ORIGINS`: Your Vercel frontend URL (e.g. `https://memoraai.vercel.app`)
   - `NEMOTRON_API_KEY`: Your Nvidia Nemotron API key
5. Deploy the service.

### 4. Frontend (Vercel)
1. Connect your GitHub repository to [Vercel](https://vercel.com/).
2. Set the **Framework Preset** to `Vite`.
3. Set the **Root Directory** to `frontend`.
4. Add the following Environment Variables:
   - `VITE_BACKEND_URL`: The URL of your deployed Backend
   - `VITE_AI_SERVICE_URL`: The URL of your deployed AI Service
5. Deploy the project. Vercel will automatically use `vercel.json` for React Router SPA fallbacks.

---

## License

This project is proprietary. All rights reserved.
