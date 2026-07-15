package com.memoraai.anme.repository;

import com.memoraai.anme.entity.Concept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConceptRepository extends JpaRepository<Concept, UUID> {
    Optional<Concept> findByNameIgnoreCase(String name);

    List<Concept> findByDocumentId(UUID documentId);
    
    Optional<Concept> findByDocumentIdAndNormalizedName(UUID documentId, String normalizedName);
}
