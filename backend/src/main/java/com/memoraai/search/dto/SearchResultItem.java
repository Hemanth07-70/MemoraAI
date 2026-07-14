package com.memoraai.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultItem {
    @Schema(description = "The ID of the document")
    private UUID documentId;
    
    @Schema(description = "The ID of the specific chunk")
    private UUID chunkId;
    
    @Schema(description = "Cosine similarity score (0.0 to 1.0)", example = "0.94")
    private double score;
    
    @Schema(description = "The sequential index of this chunk in the document", example = "2")
    private Integer chunkIndex;
    
    @Schema(description = "The text content of the matching chunk")
    private String text;
}
