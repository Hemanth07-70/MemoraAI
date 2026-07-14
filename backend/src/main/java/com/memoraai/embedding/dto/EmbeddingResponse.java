package com.memoraai.embedding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmbeddingResponse {
    private boolean success;
    private int dimension;
    private List<Float> embedding;
    private String error;
}
