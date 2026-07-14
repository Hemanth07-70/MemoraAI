import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_generate_embedding_success():
    response = client.post(
        "/api/v1/embeddings",
        json={
            "chunkId": "test-chunk-1",
            "text": "This is a test document chunk for embedding generation."
        }
    )
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert data["dimension"] == 384
    assert len(data["embedding"]) == 384
    assert isinstance(data["embedding"][0], float)

def test_generate_embedding_empty_text():
    response = client.post(
        "/api/v1/embeddings",
        json={
            "chunkId": "test-chunk-2",
            "text": "   "
        }
    )
    assert response.status_code == 500
    data = response.json()
    assert "Text cannot be empty" in data["detail"]

def test_generate_embedding_missing_fields():
    response = client.post(
        "/api/v1/embeddings",
        json={
            "text": "This is missing chunkId"
        }
    )
    assert response.status_code == 422
