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
public class UserMemoryStateDto {
    private UUID id;
    private UUID userId;
    private ConceptDto concept;
    private Double memoryScore;
    private Integer reviewCount;
    private LocalDateTime lastReviewedAt;
    private LocalDateTime nextReviewAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
