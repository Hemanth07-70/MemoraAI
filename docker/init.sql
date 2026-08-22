-- Enable pgvector extension (required before Hibernate creates vector(384) columns)
CREATE EXTENSION IF NOT EXISTS vector;
