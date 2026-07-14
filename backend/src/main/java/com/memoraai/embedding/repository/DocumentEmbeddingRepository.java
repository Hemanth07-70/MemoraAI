package com.memoraai.embedding.repository;

import com.memoraai.embedding.entity.DocumentEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentEmbeddingRepository extends JpaRepository<DocumentEmbedding, UUID> {

    boolean existsByChunkId(UUID chunkId);

    List<DocumentEmbedding> findByChunkExtractedDocumentDocumentId(UUID documentId);
    
    Optional<DocumentEmbedding> findByChunkId(UUID chunkId);
}
