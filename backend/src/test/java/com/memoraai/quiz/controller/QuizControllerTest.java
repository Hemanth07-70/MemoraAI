package com.memoraai.quiz.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoraai.quiz.dto.AnswerDto;
import com.memoraai.quiz.dto.QuizDto;
import com.memoraai.quiz.dto.QuizResultDto;
import com.memoraai.quiz.dto.SubmitQuizRequest;
import com.memoraai.quiz.entity.QuizStatus;
import com.memoraai.quiz.service.QuizGenerationService;
import com.memoraai.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuizController.class)
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QuizGenerationService quizService;

    @MockBean
    private com.memoraai.auth.util.JwtUtil jwtUtil;

    private final UUID documentId = UUID.randomUUID();
    private final UUID quizId = UUID.randomUUID();

    @Test
    @WithMockUser
    void generateQuiz_returns201WithQuiz() throws Exception {
        QuizDto quiz = QuizDto.builder()
                .id(quizId)
                .documentId(documentId)
                .title("Quiz: test.pdf")
                .questionCount(5)
                .status(QuizStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .questions(Collections.emptyList())
                .build();

        when(quizService.generateQuiz(eq(documentId), any(User.class), any()))
                .thenReturn(quiz);

        mockMvc.perform(post("/api/v1/documents/{documentId}/quiz", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionCount\": 5}")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(quizId.toString()));
    }

    @Test
    @WithMockUser
    void getQuiz_returns200WithQuiz() throws Exception {
        QuizDto quiz = QuizDto.builder()
                .id(quizId)
                .documentId(documentId)
                .title("Quiz: test.pdf")
                .questionCount(5)
                .status(QuizStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .questions(Collections.emptyList())
                .build();

        when(quizService.getQuiz(eq(quizId), any())).thenReturn(quiz);

        mockMvc.perform(get("/api/v1/quizzes/{quizId}", quizId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(quizId.toString()));
    }

    @Test
    @WithMockUser
    void submitQuiz_returns200WithResult() throws Exception {
        QuizResultDto result = QuizResultDto.builder()
                .score(4)
                .correctAnswers(4)
                .wrongAnswers(1)
                .percentage(80.0)
                .updatedMemory(List.of())
                .build();

        when(quizService.submitQuiz(eq(quizId), any(User.class), any()))
                .thenReturn(result);

        SubmitQuizRequest request = SubmitQuizRequest.builder()
                .answers(List.of(AnswerDto.builder()
                        .questionId(UUID.randomUUID())
                        .userAnswer("True")
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/quizzes/{quizId}/submit", quizId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.percentage").value(80.0))
                .andExpect(jsonPath("$.data.correctAnswers").value(4));
    }

    @Test
    void generateQuiz_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/documents/{documentId}/quiz", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
