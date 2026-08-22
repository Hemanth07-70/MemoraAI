import logging
import os

logger = logging.getLogger(__name__)

_MODEL_NAME = "all-MiniLM-L6-v2"
_EMBEDDING_DIMENSION = 384


class EmbeddingService:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(EmbeddingService, cls).__new__(cls)
            cls._instance._initialize()
        return cls._instance

    def _initialize(self):
        from sentence_transformers import SentenceTransformer
        cache_dir = os.environ.get("SENTENCE_TRANSFORMERS_HOME", None)
        logger.info("Loading embedding model '%s' (dim=%d)...", _MODEL_NAME, _EMBEDDING_DIMENSION)
        self._model = SentenceTransformer(_MODEL_NAME, cache_folder=cache_dir)
        self.dimension = _EMBEDDING_DIMENSION
        logger.info("EmbeddingService ready (sentence-transformers CPU, model=%s)", _MODEL_NAME)

    def generate_embedding(self, text: str) -> list:
        if not text or not text.strip():
            logger.warning("Empty text received for embedding generation.")
            return []
        embedding = self._model.encode(text, convert_to_numpy=True)
        return embedding.tolist()

    def get_dimension(self) -> int:
        return self.dimension
