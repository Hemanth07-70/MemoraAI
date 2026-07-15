package com.memoraai.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateQuizRequest {

    /**
     * Number of questions to generate. Defaults to 10 in the service if null.
     */
    private Integer questionCount;
}
