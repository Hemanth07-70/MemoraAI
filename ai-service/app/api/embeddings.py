from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import List, Optional
import logging

from app.services.embedding_service import EmbeddingService

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/embeddings",
    tags=["Embeddings"],
)

class EmbeddingRequest(BaseModel):
    chunkId: str = Field(..., description="The ID of the chunk to embed")
    text: str = Field(..., description="The text to generate an embedding for")

class EmbeddingResponse(BaseModel):
    success: bool
    dimension: int
    embedding: List[float]
    error: Optional[str] = None

# Initialize service once
embedding_service = EmbeddingService()

@router.post("", response_model=EmbeddingResponse)
async def generate_embedding(request: EmbeddingRequest):
    try:
        logger.info(f"Generating embedding for chunk ID: {request.chunkId}")
        if not request.text or not request.text.strip():
            raise ValueError("Text cannot be empty")
        embedding_vector = embedding_service.generate_embedding(request.text)
        return EmbeddingResponse(success=True, dimension=embedding_service.get_dimension(), embedding=embedding_vector)
    except Exception as e:
        logger.error(f"Failed to generate embedding for chunk {request.chunkId}: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


class BatchEmbeddingItem(BaseModel):
    chunkId: str
    text: str

class BatchEmbeddingResult(BaseModel):
    chunkId: str
    embedding: List[float]
    dimension: int

class BatchEmbeddingResponse(BaseModel):
    success: bool
    results: List[BatchEmbeddingResult]
    error: Optional[str] = None

@router.post("/batch", response_model=BatchEmbeddingResponse)
async def generate_embeddings_batch(items: List[BatchEmbeddingItem]):
    try:
        texts = [item.text for item in items]
        logger.info(f"Batch embedding {len(texts)} chunks")
        vectors = embedding_service.generate_embeddings_batch(texts)
        dim = embedding_service.get_dimension()
        results = [
            BatchEmbeddingResult(chunkId=item.chunkId, embedding=vec, dimension=dim)
            for item, vec in zip(items, vectors)
        ]
        return BatchEmbeddingResponse(success=True, results=results)
    except Exception as e:
        logger.error(f"Batch embedding failed: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))
