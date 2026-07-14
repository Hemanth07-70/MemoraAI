import logging
from fastapi import APIRouter
from app.models.process import ProcessRequest, ProcessResponse
from app.services.pdf_extractor import extract_text

router = APIRouter()
logger = logging.getLogger(__name__)


@router.post("/process", response_model=ProcessResponse)
async def submit_process_job(request: ProcessRequest):
    """
    Process a document based on job type.
    For OCR jobs: extracts text from PDF using PyMuPDF.
    For other job types: accepts the job for future processing.
    """
    logger.info("Received process request for job %s of type %s", request.jobId, request.jobType)

    if request.jobType == "OCR":
        return _handle_ocr(request)

    # Non-OCR job types: accept for future processing
    return _handle_non_ocr(request)


def _handle_ocr(request: ProcessRequest) -> ProcessResponse:
    """Handle OCR job by extracting text from a PDF file."""
    logger.info("Starting PDF text extraction for job %s, file: %s", request.jobId, request.filePath)

    result = extract_text(request.filePath)

    if not result.success:
        logger.error("PDF extraction failed for job %s: %s", request.jobId, result.error_message)
        return ProcessResponse(
            success=False,
            status="FAILED",
            message=result.error_message or "Unable to extract text",
        )

    logger.info(
        "PDF extraction succeeded for job %s: %d pages, %d words, %d chars",
        request.jobId, result.page_count, result.word_count, result.character_count,
    )

    return ProcessResponse(
        success=True,
        status="COMPLETED",
        message="Text extracted successfully",
        pageCount=result.page_count,
        wordCount=result.word_count,
        characterCount=result.character_count,
        text=result.text,
    )


def _handle_non_ocr(request: ProcessRequest) -> ProcessResponse:
    """Handle non-OCR job types with stub acceptance."""
    import os
    if not os.path.exists(request.filePath):
        logger.error("File not found: %s", request.filePath)
        return ProcessResponse(
            success=False,
            status="FAILED",
            message="File not found",
        )

    logger.info("Non-OCR job %s accepted for future processing", request.jobId)
    return ProcessResponse(
        success=True,
        status="PROCESSING",
        message="Job accepted",
    )
