package com.memoraai.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    @NotBlank(message = "Query cannot be blank")
    @Schema(description = "The query text to search for", example = "Machine Learning")
    private String query;

    @Builder.Default
    @Schema(description = "Maximum number of results to return", example = "5", defaultValue = "5")
    private Integer topK = 5;
}
