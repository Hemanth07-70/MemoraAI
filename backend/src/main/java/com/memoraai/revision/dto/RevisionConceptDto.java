package com.memoraai.revision.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevisionConceptDto {
    private UUID conceptId;
    private String conceptName;
    private UUID documentId;
    private Double memoryScore;
    private Double importanceScore;
    private Double difficultyScore;
    private Double priority;
}
