package com.memoraai.quiz.dto;

import com.memoraai.quiz.entity.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionDto {
    private UUID id;
    private QuestionType questionType;
    private String questionText;
    private List<String> options;
    private UUID conceptId;
    private Double difficulty;
}
