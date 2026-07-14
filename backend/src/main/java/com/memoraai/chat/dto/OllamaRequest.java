package com.memoraai.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaRequest {
    private String model;
    private String prompt;
    @Builder.Default
    private boolean stream = false;
}
