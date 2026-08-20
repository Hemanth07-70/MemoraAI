from pydantic import BaseModel, Field
from typing import Optional


class ProcessRequest(BaseModel):
    jobId: str = Field(..., description="UUID of the processing job")
    documentId: str = Field(..., description="UUID of the document")
    jobType: str = Field(..., description="Type of job (e.g., OCR, EMBEDDING)")
    filePath: Optional[str] = Field(None, description="Local path to file (local dev only)")
    fileContent: Optional[str] = Field(None, description="Base64-encoded file bytes (cloud deployment)")


class ProcessResponse(BaseModel):
    success: bool
    status: str
    message: str
    pageCount: Optional[int] = None
    wordCount: Optional[int] = None
    characterCount: Optional[int] = None
    text: Optional[str] = None
