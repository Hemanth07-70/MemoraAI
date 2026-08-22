package com.memoraai.anme.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoraai.anme.dto.RelationshipExtractionResult;
import com.memoraai.anme.entity.Concept;
import com.memoraai.anme.entity.ConceptRelationship;
import com.memoraai.anme.entity.RelationshipType;
import com.memoraai.anme.repository.ConceptRelationshipRepository;
import com.memoraai.anme.repository.ConceptRepository;
import com.memoraai.chat.service.LLMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGraphService {

    private final ConceptRepository conceptRepository;
    private final ConceptRelationshipRepository relationshipRepository;
    private final LLMService llmService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void generateKnowledgeGraph(UUID documentId) {
        log.info("Starting knowledge graph generation for document {}", documentId);

        List<Concept> concepts = conceptRepository.findByDocumentId(documentId);
        if (concepts.size() < 2) {
            log.warn("Not enough concepts found for document {}. Minimum 2 required.", documentId);
            return;
        }

        // Limit the number of concepts sent to the LLM to avoid context window issues
        List<Concept> targetConcepts = concepts;
        if (concepts.size() > 15) {
            log.warn("Too many concepts ({}). Limiting to top 15 for knowledge graph generation.", concepts.size());
            targetConcepts = concepts.stream()
                    .sorted((a, b) -> Double.compare(b.getImportanceScore(), a.getImportanceScore()))
                    .limit(15)
                    .collect(Collectors.toList());
        }

        Map<String, Concept> conceptNameMap = targetConcepts.stream()
                .collect(Collectors.toMap(
                        c -> c.getName().trim().toLowerCase(),
                        c -> c,
                        (existing, replacement) -> existing
                ));

        List<RelationshipExtractionResult> extractedRelationships = extractRelationshipsWithLLM(targetConcepts);
        
        List<ConceptRelationship> newRelationships = new ArrayList<>();

        for (RelationshipExtractionResult res : extractedRelationships) {
            if (res.getSourceConcept() == null || res.getTargetConcept() == null || res.getRelationshipType() == null) {
                continue;
            }

            Concept source = conceptNameMap.get(res.getSourceConcept().trim().toLowerCase());
            Concept target = conceptNameMap.get(res.getTargetConcept().trim().toLowerCase());

            if (source == null || target == null || source.getId().equals(target.getId())) {
                continue;
            }

            RelationshipType type;
            try {
                type = RelationshipType.valueOf(res.getRelationshipType().toUpperCase());
            } catch (IllegalArgumentException e) {
                continue;
            }

            // Deduplicate logic
            List<ConceptRelationship> existing = relationshipRepository.findExactRelationship(source.getId(), target.getId(), type);
            if (!existing.isEmpty()) {
                continue;
            }

            // Compute confidence score
            double semanticSimilarity = computeCosineSimilarity(source, target);
            double coOccurrence = computeCoOccurrence(source, target); // Simplified mock for now
            double frequencyScore = Math.min(1.0, (source.getFrequency() + target.getFrequency()) / 20.0);
            double llmConf = res.getLlmConfidence() != null ? res.getLlmConfidence() : 0.5;

            double confidenceScore = (0.40 * llmConf) + (0.20 * semanticSimilarity) + (0.20 * coOccurrence) + (0.20 * frequencyScore);
            confidenceScore = Math.min(1.0, Math.max(0.0, confidenceScore));

            ConceptRelationship rel = ConceptRelationship.builder()
                    .sourceConcept(source)
                    .targetConcept(target)
                    .relationshipType(type)
                    .confidenceScore(confidenceScore)
                    .build();

            newRelationships.add(rel);
        }

        relationshipRepository.saveAll(newRelationships);
        log.info("Persisted {} new relationships for document {}", newRelationships.size(), documentId);
    }

    private List<RelationshipExtractionResult> extractRelationshipsWithLLM(List<Concept> concepts) {
        StringBuilder conceptListBuilder = new StringBuilder();
        for (Concept c : concepts) {
            conceptListBuilder.append("- ").append(c.getName()).append(": ").append(c.getDescription()).append("\n");
        }

        String prompt = "You are an AI generating an educational knowledge graph.\n" +
                "Given the following list of learning concepts, extract all valid relationships between them.\n" +
                "Valid relationship types are: PREREQUISITE, RELATED, DEPENDS_ON, PART_OF, IMPLEMENTS, USES, EXTENDS, SIMILAR.\n" +
                "Return ONLY valid JSON. Do not return markdown.\n" +
                "Example format:\n" +
                "[\n" +
                "  {\n" +
                "    \"sourceConcept\": \"Transformer\",\n" +
                "    \"targetConcept\": \"Attention Mechanism\",\n" +
                "    \"relationshipType\": \"USES\",\n" +
                "    \"llmConfidence\": 0.95\n" +
                "  }\n" +
                "]\n\n" +
                "Concepts:\n" + conceptListBuilder.toString();

        try {
            String json = llmService.generateAnswer(prompt).block();
            if (json == null) return Collections.emptyList();

            json = json.trim();
            if (json.startsWith("```json")) {
                json = json.substring(7);
            } else if (json.startsWith("```")) {
                json = json.substring(3);
            }
            if (json.endsWith("```")) {
                json = json.substring(0, json.length() - 3);
            }
            json = json.trim();

            return objectMapper.readValue(json, new TypeReference<List<RelationshipExtractionResult>>() {});
        } catch (Exception e) {
            log.error("Failed to parse relationship JSON from LLM: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private double computeCosineSimilarity(Concept c1, Concept c2) {
        if (c1.getEmbedding() == null || c2.getEmbedding() == null) {
            return 0.5; // fallback
        }
        List<Double> v1 = parseVector(c1.getEmbedding().getEmbeddingJson());
        List<Double> v2 = parseVector(c2.getEmbedding().getEmbeddingJson());

        if (v1.isEmpty() || v1.size() != v2.size()) return 0.5;

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            dotProduct += v1.get(i) * v2.get(i);
            norm1 += Math.pow(v1.get(i), 2);
            norm2 += Math.pow(v2.get(i), 2);
        }
        if (norm1 == 0 || norm2 == 0) return 0.0;
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private List<Double> parseVector(String vectorStr) {
        try {
            if (vectorStr == null || vectorStr.isBlank()) return Collections.emptyList();
            return objectMapper.readValue(vectorStr, new TypeReference<List<Double>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private double computeCoOccurrence(Concept c1, Concept c2) {
        // Simplified: Co-occurrence is highly likely if their semantic similarity is high, 
        // or just placeholder 0.5 if we don't scan all chunks.
        return 0.5; 
    }
}
