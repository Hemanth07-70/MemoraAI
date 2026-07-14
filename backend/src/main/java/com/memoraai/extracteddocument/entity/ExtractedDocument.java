package com.memoraai.extracteddocument.entity;

import com.memoraai.document.entity.Document;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "extracted_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String extractedText;

    private Integer pageCount;
    private Integer wordCount;
    private Integer characterCount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime extractedAt;

    @PrePersist
    protected void onCreate() {
        extractedAt = LocalDateTime.now();
    }
}
