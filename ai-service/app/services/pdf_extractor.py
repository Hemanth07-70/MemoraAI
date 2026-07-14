import os
import logging
from dataclasses import dataclass
from typing import Optional

import fitz  # PyMuPDF

logger = logging.getLogger(__name__)


@dataclass
class ExtractionResult:
    """Result of a PDF text extraction operation."""
    success: bool
    text: Optional[str] = None
    page_count: int = 0
    word_count: int = 0
    character_count: int = 0
    error_message: Optional[str] = None


def extract_text(file_path: str) -> ExtractionResult:
    """
    Extract text from a PDF file using PyMuPDF.

    Opens the PDF, iterates page-by-page, extracts text,
    and calculates statistics. Handles missing, corrupted,
    encrypted, and empty files gracefully.
    """
    if not os.path.exists(file_path):
        logger.error("File not found: %s", file_path)
        return ExtractionResult(success=False, error_message="File not found")

    if not file_path.lower().endswith(".pdf"):
        logger.error("Unsupported file format: %s", file_path)
        return ExtractionResult(success=False, error_message="Unsupported file format. Only PDF files are supported.")

    try:
        doc = fitz.open(file_path)
    except Exception as e:
        logger.error("Failed to open PDF %s: %s", file_path, str(e))
        return ExtractionResult(success=False, error_message=f"Failed to open PDF: {str(e)}")

    try:
        if doc.is_encrypted:
            logger.error("PDF is encrypted: %s", file_path)
            doc.close()
            return ExtractionResult(success=False, error_message="PDF is encrypted and cannot be processed")

        page_count = len(doc)
        logger.info("PDF opened successfully: %s (%d pages)", file_path, page_count)

        if page_count == 0:
            logger.warning("PDF has no pages: %s", file_path)
            doc.close()
            return ExtractionResult(success=False, error_message="PDF has no pages")

        text_parts = []
        for page_num in range(page_count):
            page = doc[page_num]
            page_text = page.get_text("text")
            text_parts.append(page_text)
            logger.debug("Extracted text from page %d/%d", page_num + 1, page_count)

        doc.close()

        full_text = "\n".join(text_parts).strip()

        if not full_text:
            logger.warning("No text content extracted from PDF: %s", file_path)
            return ExtractionResult(
                success=True,
                text="",
                page_count=page_count,
                word_count=0,
                character_count=0,
            )

        word_count = len(full_text.split())
        character_count = len(full_text)

        logger.info(
            "Text extraction complete: %d pages, %d words, %d characters",
            page_count, word_count, character_count,
        )

        return ExtractionResult(
            success=True,
            text=full_text,
            page_count=page_count,
            word_count=word_count,
            character_count=character_count,
        )

    except Exception as e:
        logger.error("Error during text extraction from %s: %s", file_path, str(e))
        try:
            doc.close()
        except Exception:
            pass
        return ExtractionResult(success=False, error_message=f"Extraction failed: {str(e)}")
