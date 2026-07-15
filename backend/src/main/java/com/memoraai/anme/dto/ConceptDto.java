package com.memoraai.anme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConceptDto {
    private UUID id;
    private String name;
    private String description;
    private Double importanceScore;
    private Double difficultyScore;
    private UUID documentId;
    private LocalDateTime createdAt;
}
