package com.memoraai.processing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiProcessResponse {
    private boolean success;
    private String status;
    private String message;
    private Integer pageCount;
    private Integer wordCount;
    private Integer characterCount;
    private String text;
}
