package com.memoraai.quiz.controller;

import com.memoraai.common.response.ApiResponse;
import com.memoraai.quiz.dto.GenerateQuizRequest;
import com.memoraai.quiz.dto.QuizDto;
import com.memoraai.quiz.dto.QuizResultDto;
import com.memoraai.quiz.dto.SubmitQuizRequest;
import com.memoraai.quiz.service.QuizGenerationService;
import com.memoraai.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Quiz Engine", description = "Quiz generation and submission APIs")
@SecurityRequirement(name = "bearerAuth")
public class QuizController {

    private final QuizGenerationService quizService;

    @Operation(
            summary = "Generate a quiz for a document",
            description = "Generates quiz questions using extracted concepts and document chunks via LLM."
    )
    @PostMapping("/documents/{documentId}/quiz")
    public ResponseEntity<ApiResponse<QuizDto>> generateQuiz(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User user,
            @RequestBody(required = false) GenerateQuizRequest request) {

        Integer questionCount = request != null ? request.getQuestionCount() : null;
        QuizDto quiz = quizService.generateQuiz(documentId, user, questionCount);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Quiz generated successfully", quiz));
    }

    @Operation(
            summary = "Get a quiz by ID",
            description = "Returns the quiz with all questions. Correct answers are NOT included in the response."
    )
    @GetMapping("/quizzes/{quizId}")
    public ResponseEntity<ApiResponse<QuizDto>> getQuiz(
            @PathVariable UUID quizId,
            @AuthenticationPrincipal User user) {

        QuizDto quiz = quizService.getQuiz(quizId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Quiz retrieved successfully", quiz));
    }

    @Operation(
            summary = "Submit quiz answers",
            description = "Grades the quiz, stores the attempt, and updates UserMemoryState for each concept tested."
    )
    @PostMapping("/quizzes/{quizId}/submit")
    public ResponseEntity<ApiResponse<QuizResultDto>> submitQuiz(
            @PathVariable UUID quizId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SubmitQuizRequest request) {

        QuizResultDto result = quizService.submitQuiz(quizId, user, request);
        return ResponseEntity.ok(ApiResponse.success("Quiz submitted successfully", result));
    }
}
