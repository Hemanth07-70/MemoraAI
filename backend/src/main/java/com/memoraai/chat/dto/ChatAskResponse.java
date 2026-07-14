package com.memoraai.chat.dto;

import com.memoraai.search.dto.SearchResultItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatAskResponse {

    @Schema(description = "The generated answer from the LLM")
    private String answer;

    @Schema(description = "The LLM provider used to generate the answer")
    private String provider;

    @Schema(description = "The specific model used to generate the answer")
    private String model;

    @Schema(description = "The sources used to generate the answer")
    private List<SearchResultItem> sources;

    @Schema(description = "Time taken to retrieve chunks in milliseconds")
    private long retrievalTimeMs;

    @Schema(description = "Time taken for LLM generation in milliseconds")
    private long generationTimeMs;

    @Schema(description = "Total request processing time in milliseconds")
    private long totalTimeMs;
}
