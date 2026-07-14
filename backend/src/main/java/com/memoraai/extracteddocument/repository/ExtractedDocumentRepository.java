package com.memoraai.extracteddocument.repository;

import com.memoraai.document.entity.Document;
import com.memoraai.extracteddocument.entity.ExtractedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExtractedDocumentRepository extends JpaRepository<ExtractedDocument, UUID> {
    Optional<ExtractedDocument> findByDocument(Document document);
}
