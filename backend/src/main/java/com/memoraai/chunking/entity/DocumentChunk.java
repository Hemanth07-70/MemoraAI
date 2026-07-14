package com.memoraai.chunking.entity;

import com.memoraai.extracteddocument.entity.ExtractedDocument;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_chunks", indexes = {
        @Index(name = "idx_document_chunk_doc_id", columnList = "extracted_document_id"),
        @Index(name = "idx_document_chunk_index", columnList = "chunk_index")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extracted_document_id", nullable = false)
    private ExtractedDocument extractedDocument;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    private String chunkText;

    @Column(name = "start_offset", nullable = false)
    private Integer startOffset;

    @Column(name = "end_offset", nullable = false)
    private Integer endOffset;

    @Column(name = "character_count", nullable = false)
    private Integer characterCount;

    @Column(name = "word_count", nullable = false)
    private Integer wordCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
