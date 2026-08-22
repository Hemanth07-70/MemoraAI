package com.memoraai.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Runs after Hibernate DDL. Uses JdbcTemplate (no @Transactional) so a DDL
 * failure does not poison a surrounding transaction and crash the app.
 *
 * Step 1: CREATE EXTENSION — safe to run every startup (IF NOT EXISTS is idempotent).
 *         This covers the case where the postgres volume existed before init.sql was added.
 * Step 2: CREATE INDEX USING hnsw — needed for Layer 1 of the 3-layer RAG pipeline.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorIndexInitializer {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        enableExtension();
        addVectorColumn();
        createHnswIndex();
    }

    private void enableExtension() {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            log.info("pgvector extension is enabled");
        } catch (Exception e) {
            log.warn("Could not enable pgvector extension: {}", e.getMessage());
        }
    }

    private void addVectorColumn() {
        try {
            jdbcTemplate.execute(
                "ALTER TABLE document_embeddings ADD COLUMN IF NOT EXISTS embedding_vector vector(384)");
            log.info("document_embeddings.embedding_vector column is ready");
        } catch (Exception e) {
            log.warn("Could not add embedding_vector column: {}", e.getMessage());
        }
    }

    private void createHnswIndex() {
        try {
            jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_embedding_vector_hnsw
                ON document_embeddings
                USING hnsw (embedding_vector vector_cosine_ops)
                WITH (m = 16, ef_construction = 64)
                """);
            log.info("HNSW index on document_embeddings.embedding_vector is ready");
        } catch (Exception e) {
            log.warn("Could not create HNSW index (cosine search will use seq scan): {}", e.getMessage());
        }
    }
}
