package com.memoraai.anme.dto;

import lombok.Data;

@Data
public class RelationshipExtractionResult {
    private String sourceConcept;
    private String targetConcept;
    private String relationshipType;
    private Double llmConfidence;
}
