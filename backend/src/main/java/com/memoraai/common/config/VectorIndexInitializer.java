package com.memoraai.common.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the HNSW vector index after Hibernate finishes DDL.
 * Must run after ApplicationReadyEvent (table exists by then).
 * The index is needed for Layer 1 of the 3-layer RAG pipeline.
 */
@Slf4j
@Component
public class VectorIndexInitializer {

    @PersistenceContext
    private EntityManager entityManager;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void createHnswIndex() {
        try {
            entityManager.createNativeQuery("""
                CREATE INDEX IF NOT EXISTS idx_embedding_vector_hnsw
                ON document_embeddings
                USING hnsw (embedding_vector vector_cosine_ops)
                WITH (m = 16, ef_construction = 64)
                """).executeUpdate();
            log.info("HNSW index on document_embeddings.embedding_vector is ready");
        } catch (Exception e) {
            log.warn("Could not create HNSW index (non-fatal — cosine search will use seq scan): {}", e.getMessage());
        }
    }
}
