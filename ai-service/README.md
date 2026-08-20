# MemoraAI AI Service

PDF text extraction and semantic embedding service for MemoraAI.

## Endpoints

- `POST /api/v1/process` — OCR / PDF text extraction
- `POST /api/v1/embeddings` — Semantic embeddings via HF Inference API (all-MiniLM-L6-v2)
- `GET /` — Health check

## Environment Variables

| Variable | Description |
|---|---|
| `HF_API_KEY` | HuggingFace token for Inference API (embeddings) |
| `BACKEND_URL` | URL of the MemoraAI backend service |
