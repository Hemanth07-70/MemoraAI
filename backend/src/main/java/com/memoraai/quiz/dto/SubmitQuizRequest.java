package com.memoraai.quiz.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitQuizRequest {

    @NotNull(message = "Answers cannot be null")
    @Size(min = 1, message = "At least one answer is required")
    private List<AnswerDto> answers;
}
