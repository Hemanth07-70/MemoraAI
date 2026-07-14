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
            logger.warning(f"Empty text provided for chunk ID: {request.chunkId}")
            raise ValueError("Text cannot be empty")
            
        embedding_vector = embedding_service.generate_embedding(request.text)
        dimension = embedding_service.get_dimension()
        
        return EmbeddingResponse(
            success=True,
            dimension=dimension,
            embedding=embedding_vector
        )
    except Exception as e:
        logger.error(f"Failed to generate embedding for chunk {request.chunkId}: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))
