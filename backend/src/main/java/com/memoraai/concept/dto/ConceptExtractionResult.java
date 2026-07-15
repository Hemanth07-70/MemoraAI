package com.memoraai.concept.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConceptExtractionResult {
    private String name;
    private String description;
    private Double importance;
    private Double difficulty;
}
