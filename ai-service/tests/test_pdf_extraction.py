import os
import uuid
import tempfile
import pytest
from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def _create_valid_pdf(path: str):
    """Create a minimal valid PDF with text using PyMuPDF."""
    import fitz
    doc = fitz.open()
    page = doc.new_page()
    page.insert_text((72, 72), "Hello MemoraAI. This is a test PDF document.")
    doc.save(path)
    doc.close()


def _create_corrupted_file(path: str):
    """Create a file with .pdf extension but invalid content."""
    with open(path, "wb") as f:
        f.write(b"This is not a valid PDF content at all")


class TestPdfExtraction:
    """Tests for the PDF text extraction pipeline via POST /api/v1/process."""

    def test_valid_pdf_extraction(self, tmp_path):
        pdf_path = str(tmp_path / "valid.pdf")
        _create_valid_pdf(pdf_path)

        payload = {
            "jobId": str(uuid.uuid4()),
            "documentId": str(uuid.uuid4()),
            "jobType": "OCR",
            "filePath": pdf_path,
        }

        response = client.post("/api/v1/process", json=payload)

        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert data["status"] == "COMPLETED"
        assert data["message"] == "Text extracted successfully"
        assert data["pageCount"] == 1
        assert data["wordCount"] > 0
        assert data["characterCount"] > 0
        assert "MemoraAI" in data["text"]

    def test_missing_pdf(self):
        payload = {
            "jobId": str(uuid.uuid4()),
            "documentId": str(uuid.uuid4()),
            "jobType": "OCR",
            "filePath": "/nonexistent/path/file.pdf",
        }

        response = client.post("/api/v1/process", json=payload)

        assert response.status_code == 200
        data = response.json()
        assert data["success"] is False
        assert data["status"] == "FAILED"
        assert "File not found" in data["message"]

    def test_corrupted_pdf(self, tmp_path):
        corrupted_path = str(tmp_path / "corrupted.pdf")
        _create_corrupted_file(corrupted_path)

        payload = {
            "jobId": str(uuid.uuid4()),
            "documentId": str(uuid.uuid4()),
            "jobType": "OCR",
            "filePath": corrupted_path,
        }

        response = client.post("/api/v1/process", json=payload)

        assert response.status_code == 200
        data = response.json()
        assert data["success"] is False
        assert data["status"] == "FAILED"

    def test_unsupported_format(self, tmp_path):
        txt_path = str(tmp_path / "document.txt")
        with open(txt_path, "w") as f:
            f.write("This is a text file, not a PDF")

        payload = {
            "jobId": str(uuid.uuid4()),
            "documentId": str(uuid.uuid4()),
            "jobType": "OCR",
            "filePath": txt_path,
        }

        response = client.post("/api/v1/process", json=payload)

        assert response.status_code == 200
        data = response.json()
        assert data["success"] is False
        assert data["status"] == "FAILED"
        assert "Unsupported" in data["message"]

    def test_non_ocr_job_with_valid_file(self, tmp_path):
        """Non-OCR jobs should return PROCESSING status (stub behavior)."""
        pdf_path = str(tmp_path / "valid.pdf")
        _create_valid_pdf(pdf_path)

        payload = {
            "jobId": str(uuid.uuid4()),
            "documentId": str(uuid.uuid4()),
            "jobType": "EMBEDDING",
            "filePath": pdf_path,
        }

        response = client.post("/api/v1/process", json=payload)

        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert data["status"] == "PROCESSING"
        assert data["text"] is None
