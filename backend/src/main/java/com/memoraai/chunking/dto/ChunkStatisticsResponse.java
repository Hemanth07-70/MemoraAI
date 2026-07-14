package com.memoraai.chunking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkStatisticsResponse {
    private Integer chunkCount;
    private Integer averageChunkSize;
    private Integer largestChunk;
    private Integer smallestChunk;
}
