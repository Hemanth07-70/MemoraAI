import pytest
from fastapi.testclient import TestClient
import uuid
import tempfile
import os
from app.main import app

client = TestClient(app)

def test_submit_process_job_success():
    # Test OCR with invalid non-PDF file will fail extraction
    with tempfile.NamedTemporaryFile(delete=False) as tmp:
        tmp.write(b"Not a PDF")
        tmp_path = tmp.name

    try:
        payload = {
            "jobId": str(uuid.uuid4()),
            "documentId": str(uuid.uuid4()),
            "jobType": "OCR",
            "filePath": tmp_path
        }
        
        response = client.post("/api/v1/process", json=payload)
        
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is False
        assert data["status"] == "FAILED"
    finally:
        os.unlink(tmp_path)

def test_submit_process_job_file_not_found():
    payload = {
        "jobId": str(uuid.uuid4()),
        "documentId": str(uuid.uuid4()),
        "jobType": "EMBEDDING", # Non-OCR test
        "filePath": "/path/to/nonexistent/file.pdf"
    }
    
    response = client.post("/api/v1/process", json=payload)
    
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is False
    assert data["status"] == "FAILED"
    assert "File not found" in data["message"]
