import logging
import os
import requests

logger = logging.getLogger(__name__)

_HF_API_URL = "https://router.huggingface.co/hf-inference/models/sentence-transformers/all-MiniLM-L6-v2/pipeline/feature-extraction"
_EMBEDDING_DIMENSION = 384


class EmbeddingService:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(EmbeddingService, cls).__new__(cls)
            cls._instance._initialize()
        return cls._instance

    def _initialize(self):
        self._api_token = os.environ.get("HF_API_KEY", "")
        self.dimension = _EMBEDDING_DIMENSION
        logger.info("EmbeddingService ready (HF Inference API, model=all-MiniLM-L6-v2, dim=%d)", self.dimension)

    def generate_embedding(self, text: str) -> list:
        if not text or not text.strip():
            logger.warning("Empty text received for embedding generation.")
            return []

        headers = {"Content-Type": "application/json"}
        if self._api_token:
            headers["Authorization"] = f"Bearer {self._api_token}"

        response = requests.post(
            _HF_API_URL,
            headers=headers,
            json={"inputs": text, "options": {"wait_for_model": True}},
            timeout=30,
        )
        response.raise_for_status()
        result = response.json()

        # HF feature-extraction returns [[float x 384]] for a single input
        if isinstance(result, list) and len(result) > 0 and isinstance(result[0], list):
            return result[0]
        if isinstance(result, list) and len(result) == _EMBEDDING_DIMENSION:
            return result
        raise ValueError(f"Unexpected embedding response shape: {type(result)}")

    def get_dimension(self) -> int:
        return self.dimension
