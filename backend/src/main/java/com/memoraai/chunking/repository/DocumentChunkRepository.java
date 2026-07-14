package com.memoraai.chunking.repository;

import com.memoraai.chunking.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {
    List<DocumentChunk> findByExtractedDocumentDocumentIdOrderByChunkIndexAsc(UUID documentId);
}
