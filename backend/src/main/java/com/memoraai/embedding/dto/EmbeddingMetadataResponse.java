package com.memoraai.embedding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmbeddingMetadataResponse {
    private UUID id;
    private UUID chunkId;
    private Integer dimension;
    private String modelName;
    private Long generationTimeMs;
}
