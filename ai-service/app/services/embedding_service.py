import logging
from sentence_transformers import SentenceTransformer

logger = logging.getLogger(__name__)

class EmbeddingService:
    _instance = None
    _model = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(EmbeddingService, cls).__new__(cls)
            cls._instance._initialize()
        return cls._instance

    def _initialize(self):
        self.model_name = "sentence-transformers/all-MiniLM-L6-v2"
        logger.info(f"Loading embedding model: {self.model_name}")
        # Load the model once
        self._model = SentenceTransformer(self.model_name, device="cpu")
        self.dimension = self._model.get_sentence_embedding_dimension()
        logger.info(f"Embedding model loaded successfully. Dimension: {self.dimension}")

    def generate_embedding(self, text: str) -> list[float]:
        if not text or not text.strip():
            logger.warning("Empty text received for embedding generation.")
            return []
        
        # Convert NumPy array to Python list of floats
        embedding = self._model.encode(text, convert_to_numpy=True).tolist()
        return embedding

    def get_dimension(self) -> int:
        return self.dimension
