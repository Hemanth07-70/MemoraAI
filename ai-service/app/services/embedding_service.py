import logging
import os

logger = logging.getLogger(__name__)

_MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2"
_EMBEDDING_DIMENSION = 384


class EmbeddingService:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(EmbeddingService, cls).__new__(cls)
            cls._instance._initialize()
        return cls._instance

    def _initialize(self):
        from fastembed import TextEmbedding
        cache_dir = os.environ.get("FASTEMBED_CACHE_PATH", None)
        logger.info("Loading embedding model '%s' via fastembed (ONNX, dim=%d)...", _MODEL_NAME, _EMBEDDING_DIMENSION)
        self._model = TextEmbedding(model_name=_MODEL_NAME, cache_dir=cache_dir)
        self.dimension = _EMBEDDING_DIMENSION
        logger.info("EmbeddingService ready (fastembed ONNX, model=%s)", _MODEL_NAME)

    def generate_embedding(self, text: str) -> list:
        if not text or not text.strip():
            logger.warning("Empty text received for embedding generation.")
            return []
        embeddings = list(self._model.embed([text]))
        return embeddings[0].tolist()

    def get_dimension(self) -> int:
        return self.dimension
