package com.memoraai.embedding.repository;

import com.memoraai.embedding.entity.DocumentEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentEmbeddingRepository extends JpaRepository<DocumentEmbedding, UUID> {

    boolean existsByChunkId(UUID chunkId);

    List<DocumentEmbedding> findByChunkExtractedDocumentDocumentId(UUID documentId);

    Optional<DocumentEmbedding> findByChunkId(UUID chunkId);

    /** Writes the vector column via explicit cast (Hibernate can't cast String→vector implicitly) */
    @Modifying
    @Query(value = "UPDATE document_embeddings SET embedding_vector = CAST(:embeddingJson AS vector) WHERE id = :id",
            nativeQuery = true)
    void updateEmbeddingVector(@Param("id") UUID id, @Param("embeddingJson") String embeddingJson);

    /** pgvector HNSW search — user-scoped, cross-document */
    @Query(value = """
        SELECT d.id            AS document_id,
               dc.id           AS chunk_id,
               dc.chunk_index  AS chunk_index,
               dc.chunk_text   AS chunk_text,
               (1 - (de.embedding_vector <=> CAST(:queryVector AS vector))) AS score
        FROM document_embeddings de
        JOIN document_chunks dc        ON de.chunk_id = dc.id
        JOIN extracted_documents ed    ON dc.extracted_document_id = ed.id
        JOIN documents d               ON ed.document_id = d.id
        WHERE d.owner_id = :userId
          AND d.is_deleted = false
          AND de.embedding_vector IS NOT NULL
        ORDER BY de.embedding_vector <=> CAST(:queryVector AS vector)
        LIMIT :topK
        """, nativeQuery = true)
    List<Object[]> vectorSearchForUser(
            @Param("queryVector") String queryVector,
            @Param("userId") UUID userId,
            @Param("topK") int topK);

    /** pgvector HNSW search — single document */
    @Query(value = """
        SELECT d.id            AS document_id,
               dc.id           AS chunk_id,
               dc.chunk_index  AS chunk_index,
               dc.chunk_text   AS chunk_text,
               (1 - (de.embedding_vector <=> CAST(:queryVector AS vector))) AS score
        FROM document_embeddings de
        JOIN document_chunks dc        ON de.chunk_id = dc.id
        JOIN extracted_documents ed    ON dc.extracted_document_id = ed.id
        JOIN documents d               ON ed.document_id = d.id
        WHERE ed.document_id = :documentId
          AND d.owner_id = :userId
          AND d.is_deleted = false
          AND de.embedding_vector IS NOT NULL
        ORDER BY de.embedding_vector <=> CAST(:queryVector AS vector)
        LIMIT :topK
        """, nativeQuery = true)
    List<Object[]> vectorSearchForDocument(
            @Param("queryVector") String queryVector,
            @Param("documentId") UUID documentId,
            @Param("userId") UUID userId,
            @Param("topK") int topK);
}
