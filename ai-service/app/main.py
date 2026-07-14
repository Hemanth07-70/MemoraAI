from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api.process import router as process_router
from app.api.embeddings import router as embeddings_router

app = FastAPI(
    title="MemoraAI - AI Service",
    description="AI processing service for MemoraAI learning platform",
    version="0.1.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(process_router, prefix="/api/v1")
app.include_router(embeddings_router, prefix="/api/v1")

@app.get("/")
def health():
    return {"status": "UP", "service": "ai-service"}
