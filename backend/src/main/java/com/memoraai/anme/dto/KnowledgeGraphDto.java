package com.memoraai.anme.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class KnowledgeGraphDto {
    private List<ConceptDto> concepts;
    private List<ConceptRelationshipDto> relationships;
}
