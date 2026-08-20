import base64
import logging
import os
import tempfile
from fastapi import APIRouter
from app.models.process import ProcessRequest, ProcessResponse
from app.services.pdf_extractor import extract_text

router = APIRouter()
logger = logging.getLogger(__name__)


@router.post("/process", response_model=ProcessResponse)
async def submit_process_job(request: ProcessRequest):
    logger.info("Received process request for job %s of type %s", request.jobId, request.jobType)

    if request.jobType == "OCR":
        return _handle_ocr(request)

    return _handle_non_ocr(request)


def _resolve_file_path(request: ProcessRequest):
    """Return (path, is_temp) — writes a temp file if fileContent is provided."""
    if request.fileContent:
        try:
            file_bytes = base64.b64decode(request.fileContent)
            tmp = tempfile.NamedTemporaryFile(suffix=".pdf", delete=False)
            tmp.write(file_bytes)
            tmp.close()
            logger.info("Decoded base64 file content to temp path %s for job %s", tmp.name, request.jobId)
            return tmp.name, True
        except Exception as e:
            logger.error("Failed to decode fileContent for job %s: %s", request.jobId, e)
            return None, False
    if request.filePath and os.path.exists(request.filePath):
        return request.filePath, False
    return None, False


def _handle_ocr(request: ProcessRequest) -> ProcessResponse:
    file_path, is_temp = _resolve_file_path(request)
    if not file_path:
        return ProcessResponse(success=False, status="FAILED", message="File not found or could not be decoded")

    logger.info("Starting PDF text extraction for job %s, file: %s", request.jobId, file_path)
    try:
        result = extract_text(file_path)
    finally:
        if is_temp:
            try:
                os.unlink(file_path)
            except OSError:
                pass

    if not result.success:
        logger.error("PDF extraction failed for job %s: %s", request.jobId, result.error_message)
        return ProcessResponse(success=False, status="FAILED", message=result.error_message or "Unable to extract text")

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
    file_path, _ = _resolve_file_path(request)
    if not file_path:
        logger.error("File not found for job %s", request.jobId)
        return ProcessResponse(success=False, status="FAILED", message="File not found")

    logger.info("Non-OCR job %s accepted for future processing", request.jobId)
    return ProcessResponse(success=True, status="PROCESSING", message="Job accepted")
