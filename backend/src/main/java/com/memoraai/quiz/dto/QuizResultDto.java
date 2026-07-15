package com.memoraai.quiz.dto;

import com.memoraai.anme.dto.UserMemoryStateDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultDto {
    private Integer score;
    private Integer correctAnswers;
    private Integer wrongAnswers;
    private Double percentage;
    private List<UserMemoryStateDto> updatedMemory;
    private List<QuestionResultDto> questionResults;
}
