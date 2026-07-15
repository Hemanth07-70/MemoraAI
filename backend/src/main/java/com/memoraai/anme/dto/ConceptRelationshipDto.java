package com.memoraai.anme.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ConceptRelationshipDto {
    private UUID id;
    private UUID sourceConceptId;
    private String sourceConceptName;
    private UUID targetConceptId;
    private String targetConceptName;
    private String relationshipType;
    private Double confidenceScore;
}
