package com.memoraai.anme.controller;

import com.memoraai.anme.dto.ConceptDto;
import com.memoraai.anme.dto.ConceptRelationshipDto;
import com.memoraai.anme.dto.KnowledgeGraphDto;
import com.memoraai.anme.entity.Concept;
import com.memoraai.anme.entity.ConceptRelationship;
import com.memoraai.anme.entity.RelationshipType;
import com.memoraai.anme.mapper.ANMEMapper;
import com.memoraai.anme.repository.ConceptRelationshipRepository;
import com.memoraai.anme.repository.ConceptRepository;
import com.memoraai.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Knowledge Graph", description = "Knowledge Graph and Relationship APIs")
@SecurityRequirement(name = "bearerAuth")
public class KnowledgeGraphController {

    private final ConceptRepository conceptRepository;
    private final ConceptRelationshipRepository relationshipRepository;
    private final ANMEMapper anmeMapper;

    @Operation(summary = "Get full knowledge graph for a document")
    @GetMapping("/documents/{documentId}/knowledge-graph")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<KnowledgeGraphDto>> getDocumentKnowledgeGraph(@PathVariable UUID documentId) {
        List<ConceptDto> concepts = conceptRepository.findByDocumentId(documentId)
                .stream()
                .map(anmeMapper::conceptToDto)
                .collect(Collectors.toList());

        List<ConceptRelationshipDto> relationships = relationshipRepository.findByDocumentId(documentId)
                .stream()
                .map(anmeMapper::relationshipToDto)
                .collect(Collectors.toList());

        KnowledgeGraphDto graph = KnowledgeGraphDto.builder()
                .concepts(concepts)
                .relationships(relationships)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Knowledge graph retrieved successfully", graph));
    }

    @Operation(summary = "Get directly connected neighbors for a concept")
    @GetMapping("/concepts/{conceptId}/neighbors")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<ConceptRelationshipDto>>> getConceptNeighbors(@PathVariable UUID conceptId) {
        List<ConceptRelationshipDto> neighbors = relationshipRepository.findNeighborsByConceptId(conceptId)
                .stream()
                .map(anmeMapper::relationshipToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Neighbors retrieved successfully", neighbors));
    }

    @Operation(summary = "Get prerequisite chain for a concept")
    @GetMapping("/concepts/{conceptId}/prerequisites")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<ConceptRelationshipDto>>> getConceptPrerequisites(@PathVariable UUID conceptId) {
        List<ConceptRelationship> allPrerequisites = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        
        traversePrerequisites(conceptId, allPrerequisites, visited);

        List<ConceptRelationshipDto> prerequisiteDtos = allPrerequisites.stream()
                .map(anmeMapper::relationshipToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Prerequisite chain retrieved successfully", prerequisiteDtos));
    }
    
    private void traversePrerequisites(UUID currentConceptId, List<ConceptRelationship> allPrerequisites, Set<UUID> visited) {
        if (visited.contains(currentConceptId)) {
            return; // prevent cycles
        }
        visited.add(currentConceptId);
        
        // Find relationships where the current concept is the target, meaning the source is a prerequisite to it
        List<ConceptRelationship> directPrereqs = relationshipRepository.findByTargetConceptIdAndRelationshipType(currentConceptId, RelationshipType.PREREQUISITE);
        
        for (ConceptRelationship rel : directPrereqs) {
            if (!allPrerequisites.contains(rel)) {
                allPrerequisites.add(rel);
                traversePrerequisites(rel.getSourceConcept().getId(), allPrerequisites, visited);
            }
        }
    }

    @Operation(summary = "Get related concepts")
    @GetMapping("/concepts/{conceptId}/related")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<ConceptRelationshipDto>>> getRelatedConcepts(@PathVariable UUID conceptId) {
        List<ConceptRelationshipDto> related = relationshipRepository.findNeighborsByConceptId(conceptId)
                .stream()
                .filter(r -> r.getRelationshipType() == RelationshipType.RELATED || r.getRelationshipType() == RelationshipType.SIMILAR)
                .map(anmeMapper::relationshipToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Related concepts retrieved successfully", related));
    }
}
