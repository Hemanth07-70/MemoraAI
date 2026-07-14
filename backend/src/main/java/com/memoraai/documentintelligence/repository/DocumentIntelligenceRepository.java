package com.memoraai.documentintelligence.repository;

import com.memoraai.documentintelligence.entity.DocumentIntelligence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentIntelligenceRepository extends JpaRepository<DocumentIntelligence, UUID> {
    Optional<DocumentIntelligence> findByDocumentId(UUID documentId);
}
