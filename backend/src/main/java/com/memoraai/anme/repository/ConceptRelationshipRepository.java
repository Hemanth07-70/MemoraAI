package com.memoraai.anme.repository;

import com.memoraai.anme.entity.ConceptRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConceptRelationshipRepository extends JpaRepository<ConceptRelationship, UUID> {
    List<ConceptRelationship> findBySourceConceptId(UUID sourceConceptId);
    List<ConceptRelationship> findByTargetConceptId(UUID targetConceptId);

    @Query("SELECT r FROM ConceptRelationship r WHERE r.sourceConcept.document.id = :documentId OR r.targetConcept.document.id = :documentId")
    List<ConceptRelationship> findByDocumentId(@Param("documentId") UUID documentId);

    @Query("SELECT r FROM ConceptRelationship r WHERE r.sourceConcept.id = :conceptId OR r.targetConcept.id = :conceptId")
    List<ConceptRelationship> findNeighborsByConceptId(@Param("conceptId") UUID conceptId);

    List<ConceptRelationship> findBySourceConceptIdAndRelationshipType(UUID sourceConceptId, com.memoraai.anme.entity.RelationshipType type);
    List<ConceptRelationship> findByTargetConceptIdAndRelationshipType(UUID targetConceptId, com.memoraai.anme.entity.RelationshipType type);

    @Query("SELECT r FROM ConceptRelationship r WHERE r.sourceConcept.id = :sourceId AND r.targetConcept.id = :targetId AND r.relationshipType = :type")
    List<ConceptRelationship> findExactRelationship(@Param("sourceId") UUID sourceId, @Param("targetId") UUID targetId, @Param("type") com.memoraai.anme.entity.RelationshipType type);
}
