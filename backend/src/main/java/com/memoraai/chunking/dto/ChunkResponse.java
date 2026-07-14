package com.memoraai.chunking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkResponse {
    private UUID id;
    private Integer chunkIndex;
    private String chunkText;
    private Integer startOffset;
    private Integer endOffset;
    private Integer characterCount;
    private Integer wordCount;
}
