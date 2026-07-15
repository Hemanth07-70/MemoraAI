package com.memoraai.quiz.dto;

import com.memoraai.quiz.entity.QuizStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizDto {
    private UUID id;
    private UUID documentId;
    private String title;
    private Integer questionCount;
    private QuizStatus status;
    private LocalDateTime createdAt;
    private List<QuizQuestionDto> questions;
}
