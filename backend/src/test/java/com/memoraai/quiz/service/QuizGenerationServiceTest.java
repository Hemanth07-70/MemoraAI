package com.memoraai.quiz.service;

import com.memoraai.anme.entity.Concept;
import com.memoraai.anme.entity.UserMemoryState;
import com.memoraai.anme.mapper.ANMEMapper;
import com.memoraai.anme.repository.ConceptRepository;
import com.memoraai.anme.repository.UserMemoryStateRepository;
import com.memoraai.anme.service.ANMEMemoryService;
import com.memoraai.chat.service.LLMService;
import com.memoraai.chunking.repository.DocumentChunkRepository;
import com.memoraai.document.entity.Document;
import com.memoraai.document.repository.DocumentRepository;
import com.memoraai.quiz.dto.AnswerDto;
import com.memoraai.quiz.dto.QuizResultDto;
import com.memoraai.quiz.dto.SubmitQuizRequest;
import com.memoraai.quiz.entity.Quiz;
import com.memoraai.quiz.entity.QuizQuestion;
import com.memoraai.quiz.entity.QuizStatus;
import com.memoraai.quiz.entity.QuestionType;
import com.memoraai.quiz.repository.QuizAttemptRepository;
import com.memoraai.quiz.repository.QuizQuestionRepository;
import com.memoraai.quiz.repository.QuizRepository;
import com.memoraai.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizGenerationServiceTest {

    @Mock private QuizRepository quizRepository;
    @Mock private QuizQuestionRepository questionRepository;
    @Mock private QuizAttemptRepository attemptRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private ConceptRepository conceptRepository;
    @Mock private DocumentChunkRepository chunkRepository;
    @Mock private UserMemoryStateRepository memoryStateRepository;
    @Mock private ANMEMemoryService memoryService;
    @Mock private ANMEMapper anmeMapper;
    @Mock private LLMService llmService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private QuizGenerationService quizService;

    private User user;
    private Concept concept;
    private Quiz quiz;
    private QuizQuestion question;
    private UUID quizId;
    private UUID conceptId;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
        conceptId = UUID.randomUUID();
        concept = Concept.builder()
                .id(conceptId)
                .name("Machine Learning")
                .importanceScore(0.9)
                .difficultyScore(0.7)
                .build();

        quizId = UUID.randomUUID();
        quiz = Quiz.builder()
                .id(quizId)
                .user(user)
                .document(Document.builder().id(UUID.randomUUID()).originalFileName("test.pdf").build())
                .title("Test Quiz")
                .questionCount(1)
                .status(QuizStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        question = QuizQuestion.builder()
                .id(UUID.randomUUID())
                .quiz(quiz)
                .concept(concept)
                .questionType(QuestionType.TRUE_FALSE)
                .questionText("Is supervised learning a type of machine learning?")
                .options(List.of("True", "False"))
                .correctAnswer("True")
                .difficulty(0.4)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void submitQuiz_allCorrect_returnsFullScore() {
        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
        when(questionRepository.findByQuizId(quizId)).thenReturn(List.of(question));
        when(quizRepository.save(any())).thenReturn(quiz);
        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserMemoryState state = buildMemoryState();
        when(memoryStateRepository.findByUserIdAndConceptId(user.getId(), conceptId))
                .thenReturn(Optional.of(state));
        when(memoryService.updateMemoryAfterQuiz(any(), anyDouble()))
                .thenReturn(state);
        when(anmeMapper.userMemoryStateToDto(any())).thenReturn(null);

        SubmitQuizRequest request = SubmitQuizRequest.builder()
                .answers(List.of(AnswerDto.builder()
                        .questionId(question.getId())
                        .userAnswer("True")
                        .build()))
                .build();

        QuizResultDto result = quizService.submitQuiz(quizId, user, request);

        assertNotNull(result);
        assertEquals(1, result.getCorrectAnswers());
        assertEquals(0, result.getWrongAnswers());
        assertEquals(100.0, result.getPercentage(), 0.01);
        assertEquals(1, result.getScore());
        verify(attemptRepository).save(any());
    }

    @Test
    void submitQuiz_allWrong_returnsZeroScore() {
        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
        when(questionRepository.findByQuizId(quizId)).thenReturn(List.of(question));
        when(quizRepository.save(any())).thenReturn(quiz);
        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserMemoryState state = buildMemoryState();
        when(memoryStateRepository.findByUserIdAndConceptId(user.getId(), conceptId))
                .thenReturn(Optional.of(state));
        when(memoryService.updateMemoryAfterQuiz(any(), anyDouble())).thenReturn(state);
        when(anmeMapper.userMemoryStateToDto(any())).thenReturn(null);

        SubmitQuizRequest request = SubmitQuizRequest.builder()
                .answers(List.of(AnswerDto.builder()
                        .questionId(question.getId())
                        .userAnswer("False") // wrong
                        .build()))
                .build();

        QuizResultDto result = quizService.submitQuiz(quizId, user, request);

        assertEquals(0, result.getCorrectAnswers());
        assertEquals(1, result.getWrongAnswers());
        assertEquals(0.0, result.getPercentage(), 0.01);
    }

    @Test
    void submitQuiz_marksQuizAsCompleted() {
        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
        when(questionRepository.findByQuizId(quizId)).thenReturn(List.of(question));
        when(quizRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserMemoryState state = buildMemoryState();
        when(memoryStateRepository.findByUserIdAndConceptId(user.getId(), conceptId))
                .thenReturn(Optional.of(state));
        when(memoryService.updateMemoryAfterQuiz(any(), anyDouble())).thenReturn(state);
        when(anmeMapper.userMemoryStateToDto(any())).thenReturn(null);

        SubmitQuizRequest request = SubmitQuizRequest.builder()
                .answers(List.of(AnswerDto.builder()
                        .questionId(question.getId())
                        .userAnswer("True")
                        .build()))
                .build();

        quizService.submitQuiz(quizId, user, request);

        assertEquals(QuizStatus.COMPLETED, quiz.getStatus());
    }

    @Test
    void submitQuiz_updatesMemoryForTestedConcept() {
        when(quizRepository.findById(quizId)).thenReturn(Optional.of(quiz));
        when(questionRepository.findByQuizId(quizId)).thenReturn(List.of(question));
        when(quizRepository.save(any())).thenReturn(quiz);
        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserMemoryState state = buildMemoryState();
        when(memoryStateRepository.findByUserIdAndConceptId(user.getId(), conceptId))
                .thenReturn(Optional.of(state));
        when(memoryService.updateMemoryAfterQuiz(any(), anyDouble())).thenReturn(state);
        when(anmeMapper.userMemoryStateToDto(any())).thenReturn(null);

        SubmitQuizRequest request = SubmitQuizRequest.builder()
                .answers(List.of(AnswerDto.builder()
                        .questionId(question.getId())
                        .userAnswer("True")
                        .build()))
                .build();

        quizService.submitQuiz(quizId, user, request);

        verify(memoryService).updateMemoryAfterQuiz(any(UserMemoryState.class), anyDouble());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UserMemoryState buildMemoryState() {
        return UserMemoryState.builder()
                .id(UUID.randomUUID())
                .user(user)
                .concept(concept)
                .memoryScore(0.5)
                .reviewCount(0)
                .lastReviewedAt(LocalDateTime.now())
                .nextReviewAt(LocalDateTime.now())
                .build();
    }
}
