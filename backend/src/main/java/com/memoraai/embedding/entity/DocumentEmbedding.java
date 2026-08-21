package com.memoraai.embedding.entity;

import com.memoraai.chunking.entity.DocumentChunk;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_embeddings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chunk_id", nullable = false, unique = true)
    private DocumentChunk chunk;

    @Column(nullable = false)
    private Integer dimension;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String embeddingJson;

    // pgvector column — written via native SQL (explicit cast required); used for HNSW similarity search
    @Column(name = "embedding_vector", columnDefinition = "vector(384)", insertable = false, updatable = false)
    private String embeddingVector;

    @Column(nullable = false, length = 100)
    private String modelName;

    @Column(nullable = false)
    private Instant generatedAt;

    @Column(nullable = false)
    private Long generationTimeMs;
}
